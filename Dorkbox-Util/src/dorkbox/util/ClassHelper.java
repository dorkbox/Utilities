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

import java.lang.reflect.*;

public final
class ClassHelper {

    /**
     * Retrieves the generic type parameter for the PARENT (super) class of the specified class. This ONLY works
     * on parent classes because of how type erasure works in java!
     *
     * @param clazz                 class to get the parameter from
     * @param genericParameterToGet 0-based index of parameter as class to get
     */
    @SuppressWarnings({"StatementWithEmptyBody", "UnnecessaryLocalVariable"})
    public static
    Class<?> getGenericParameterAsClassForSuperClass(Class<?> clazz, int genericParameterToGet) {
        Class<?> classToCheck = clazz;

        // case of multiple inheritance, we are trying to get the first available generic info
        // don't check for Object.class (this is where superclass is null)
        while (classToCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class
            Type superClassGeneric = classToCheck.getGenericSuperclass();
            if (superClassGeneric instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) superClassGeneric).getActualTypeArguments();
                // is it possible?
                if (actualTypeArguments.length > genericParameterToGet) {
                    Class<?> rawTypeAsClass = getRawTypeAsClass(actualTypeArguments[genericParameterToGet]);
                    return rawTypeAsClass;
                }
                else {
                    // record the parameters.

                }
            }

            // NO MATCH, so walk up.
            classToCheck = classToCheck.getSuperclass();
        }

        // NOTHING! now check interfaces!
        classToCheck = clazz;
        while (classToCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class interfaces
            Type[] genericInterfaces = classToCheck.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    Type[] actualTypeArguments = ((ParameterizedType) genericInterface).getActualTypeArguments();
                    // is it possible?
                    if (actualTypeArguments.length > genericParameterToGet) {
                        Class<?> rawTypeAsClass = ClassHelper.getRawTypeAsClass(actualTypeArguments[genericParameterToGet]);
                        return rawTypeAsClass;
                    }
                    else {
                        // record the parameters.
                    }
                }
            }


            // NO MATCH, so walk up.
            classToCheck = classToCheck.getSuperclass();
        }


        // couldn't find it.
        return null;
    }

    /**
     * Return the class that is this type.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static
    Class<?> getRawTypeAsClass(Type type) {
        if (type instanceof Class) {
            Class<?> class1 = (Class<?>) type;

//            if (class1.isArray()) {
//                System.err.println("CLASS IS ARRAY TYPE: SHOULD WE DO ANYTHING WITH IT? " + class1.getSimpleName());
//                return class1.getComponentType();
//            } else {
            return class1;
//            }
        }
        else if (type instanceof GenericArrayType) {
            // note: cannot have primitive types here, only objects that are arrays (byte[], Integer[], etc)
            Type type2 = ((GenericArrayType) type).getGenericComponentType();
            Class<?> rawType = getRawTypeAsClass(type2);

            return Array.newInstance(rawType, 0)
                        .getClass();
        }
        else if (type instanceof ParameterizedType) {
            // we cannot use parameterized types, because java can't go between classes and ptypes - and this
            // going "in-between" is the magic -- and value -- of this entire infrastructure.
            // return the type.

            return (Class<?>) ((ParameterizedType) type).getRawType();

//            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
//            return (Class<?>) actualTypeArguments[0];
        }
        else if (type instanceof TypeVariable) {
            // we have a COMPLEX type parameter
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return getRawTypeAsClass(bounds[0]);
            }
        }

        throw new RuntimeException("Unknown/messed up type parameter . Can't figure it out... Quit being complex!");
    }

    /**
     * Check to see if clazz or interface directly has one of the interfaces defined by clazzItMustHave
     * <p/>
     * If the class DOES NOT directly have the interface it will fail.
     */
    public static
    boolean hasInterface(Class<?> clazzItMustHave, Class<?> clazz) {
        if (clazzItMustHave == clazz) {
            return true;
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            if (iface == clazzItMustHave) {
                return true;
            }
        }
        // now walk up to see if we can find it.
        for (Class<?> iface : interfaces) {
            boolean b = hasInterface(clazzItMustHave, iface);
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
            if (hasInterface(clazzItMustHave, superClass)) {
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
