/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.util;

import java.lang.reflect.Type;

public final
class ClassHelper {

    /**
     * Retrieves the generic type parameter for the PARENT (super) class of the specified class or lambda expression.
     *
     * Because of how type erasure works in java, this will work on lambda expressions and ONLY parent/super classes.
     *
     * @param clazz                 class that defines what the parameters can be
     * @param subClazz              class to actually get the parameter from
     * @param genericParameterToGet 0-based index of parameter as class to get
     *
     * @return null if the generic type could not be found.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "UnnecessaryLocalVariable"})
    public static
    Class<?> getGenericParameterAsClassForSuperClass(Class<?> clazz, Class<?> subClazz, int genericParameterToGet) {
        Class<?> classToCheck = subClazz;

        // this will ALWAYS return something, if it is unknown, it will return TypeResolver.Unknown.class
        Class<?>[] classes = TypeResolver.resolveRawArguments(clazz, classToCheck);
        if (classes.length > genericParameterToGet && classes[genericParameterToGet] != TypeResolver.Unknown.class) {
            return classes[genericParameterToGet];
        }

        // case of multiple inheritance, we are trying to get the first available generic info
        // don't check for Object.class (this is where superclass is null)
        while (classToCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class
            Type superClassGeneric = classToCheck.getGenericSuperclass();

            classes = TypeResolver.resolveRawArguments(superClassGeneric, classToCheck);
            if (classes.length > genericParameterToGet && classes[genericParameterToGet] != TypeResolver.Unknown.class) {
                return classes[genericParameterToGet];
            }

            // NO MATCH, so walk up.
            classToCheck = classToCheck.getSuperclass();
        }

        // NOTHING! now check interfaces!
        classToCheck = subClazz;
        while (classToCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class interfaces
            Type[] genericInterfaces = classToCheck.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {

                classes = TypeResolver.resolveRawArguments(genericInterface, classToCheck);
                if (classes.length > genericParameterToGet && classes[genericParameterToGet] != TypeResolver.Unknown.class) {
                    return classes[genericParameterToGet];
                }
            }


            // NO MATCH, so walk up.
            classToCheck = classToCheck.getSuperclass();
        }

        // couldn't find it.
        return null;
    }

    /**
     * Check to see if clazz or interface directly has one of the interfaces defined by requiredClass
     * <p/>
     * If the class DOES NOT directly have the interface it will fail.
     */
    public static
    boolean hasInterface(Class<?> requiredClass, Class<?> clazz) {
        if (requiredClass == clazz) {
            return true;
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            if (iface == requiredClass) {
                return true;
            }
        }
        // now walk up to see if we can find it.
        for (Class<?> iface : interfaces) {
            boolean b = hasInterface(requiredClass, iface);
            if (b) {
                return b;
            }
        }

        // nothing, so now we check the PARENT of this class
        Class<?> superClass = clazz.getSuperclass();

        // case of multiple inheritance, we are trying to get the first available generic info
        // don't check for Object.class (this is where superclass is null)
        while (superClass != null && superClass != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class
            if (hasInterface(requiredClass, superClass)) {
                return true;
            }

            // NO MATCH, so walk up.
            superClass = superClass.getSuperclass();
        }

        // if we don't find it.
        return false;
    }

    /**
     * Checks to see if the clazz is a subclass of a parent class.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public static
    boolean hasParentClass(Class<?> parentClazz, Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        if (parentClazz == superClass) {
            return true;
        }

        if (superClass != null && superClass != Object.class) {
            return hasParentClass(parentClazz, superClass);
        }

        return false;
    }

    private
    ClassHelper() {
    }
}
