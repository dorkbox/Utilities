/*
 * Copyright 2012 Benjamin Diedrichsen
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util.classes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dorkbox.util.collections.IdentityMap;

/**
 * @author bennidi
 *         Date: 2/16/12
 *         Time: 12:14 PM
 * @author dorkbox
 *         Date: 2/2/15
 */
public final
class ReflectionUtils {

    private static final Method[] EMPTY_METHODS = new Method[0];

    private
    ReflectionUtils() {
    }

    /**
     * Get methods annotated with the specified annotation.
     *
     * @param target
     * @param annotationClass
     * @param <A>
     * @return
     */
    public static
    <A extends Annotation> Method[] getMethods(Class<?> target,  Class<A> annotationClass) {
        ArrayList<Method> methods = new ArrayList<Method>();

        getMethods(target, annotationClass, methods);
        return methods.toArray(EMPTY_METHODS);
    }

    private static
    <A extends Annotation> void getMethods(Class<?> target, Class<A> annotationClass, ArrayList<Method> methods) {
        try {
            for (Method method : target.getDeclaredMethods()) {
                if (getAnnotation(method, annotationClass) != null) {
                    methods.add(method);
                }
            }
        } catch (Exception ignored) {
        }

        // recursively go until root
        if (!target.equals(Object.class)) {
            getMethods(target.getSuperclass(), annotationClass, methods);
        }
    }

    /**
     * Traverses the class hierarchy upwards, starting at the given subclass, looking
     * for an override of the given methods -> finds the bottom most override of the given
     * method if any exists
     */
    public static
    Method getOverridingMethod(final Method overridingMethod, final Class<?> subclass) {
        Class<?> current = subclass;
        while (!current.equals(overridingMethod.getDeclaringClass())) {
            try {
                return current.getDeclaredMethod(overridingMethod.getName(), overridingMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }


    public static
    boolean containsOverridingMethod(final Method[] allMethods, final Method methodToCheck) {
        final int length = allMethods.length;
        Method method;

        for (int i = 0; i < length; i++) {
            method = allMethods[i];

            if (isOverriddenBy(methodToCheck, method)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches for an Annotation of the given type on the class.  Supports meta annotations.
     *
     * @param from           AnnotatedElement (class, method...)
     * @param annotationType Annotation class to look for.
     * @param <A>            Class of annotation type
     * @return Annotation instance or null
     */
    private static
    <A extends Annotation> A getAnnotation(AnnotatedElement from, Class<A> annotationType, IdentityMap<AnnotatedElement, Boolean> visited) {
        if (visited.containsKey(from)) {
            return null;
        }
        visited.put(from, Boolean.TRUE);
        A ann = from.getAnnotation(annotationType);
        if (ann != null) {
            return ann;
        }
        for (Annotation metaAnn : from.getAnnotations()) {
            ann = getAnnotation(metaAnn.annotationType(), annotationType, visited);
            if (ann != null) {
                return ann;
            }
        }
        return null;
    }

    public static
    <A extends Annotation> A getAnnotation(AnnotatedElement from, Class<A> annotationType) {
        return getAnnotation(from, annotationType, new IdentityMap<AnnotatedElement, Boolean>());
    }

    //
    private static
    boolean isOverriddenBy(final Method superclassMethod, final Method subclassMethod) {
        // if the declaring classes are the same or the subclass method is not defined in the subclass
        // hierarchy of the given superclass method or the method names are not the same then
        // subclassMethod does not override superclassMethod
        if (superclassMethod.getDeclaringClass().equals(subclassMethod.getDeclaringClass()) ||
            !superclassMethod.getDeclaringClass().isAssignableFrom(subclassMethod.getDeclaringClass()) ||
            !superclassMethod.getName().equals(subclassMethod.getName())) {
            return false;
        }

        final Class<?>[] superClassMethodParameters = superclassMethod.getParameterTypes();
        final Class<?>[] subClassMethodParameters = subclassMethod.getParameterTypes();

        // method must specify the same number of parameters
        //the parameters must occur in the exact same order
        for (int i = 0; i < subClassMethodParameters.length; i++) {
            if (!superClassMethodParameters[i].equals(subClassMethodParameters[i])) {
                return false;
            }
        }
        return true;
    }
}
