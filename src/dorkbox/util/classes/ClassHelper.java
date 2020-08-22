/*
 * Copyright 2018 dorkbox, llc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dorkbox.util.classes;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;

import net.jodah.typetools.TypeResolver;

public final
class ClassHelper {

    /**
     * Retrieves the generic type parameter for the PARENT (super) class of the specified class or lambda expression.
     *
     * Because of how type erasure works in java, this will work on lambda expressions and ONLY parent/super classes.
     *
     * @param genericTypeClass  this class is what your are looking for
     * @param classToCheck              class to actually get the parameter from
     * @param genericParameterToGet 0-based index of parameter as class to get
     *
     * @return null if the generic type could not be found.
     */
    @SuppressWarnings({"StatementWithEmptyBody", "UnnecessaryLocalVariable"})
    public static
    Class<?> getGenericParameterAsClassForSuperClass(Class<?> genericTypeClass, Class<?> classToCheck, int genericParameterToGet) {
        Class<?> loopClassCheck = classToCheck;

        // this will ALWAYS return something, if it is unknown, it will return TypeResolver.Unknown.class
        Class<?>[] classes = TypeResolver.resolveRawArguments(genericTypeClass, loopClassCheck);
        if (classes.length > genericParameterToGet && classes[genericParameterToGet] != TypeResolver.Unknown.class) {
            return classes[genericParameterToGet];
        }

        // case of multiple inheritance, we are trying to get the first available generic info
        // don't check for Object.class (this is where superclass is null)
        while (loopClassCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class
            Type superClassGeneric = loopClassCheck.getGenericSuperclass();

            classes = TypeResolver.resolveRawArguments(superClassGeneric, loopClassCheck);
            if (classes.length > genericParameterToGet) {
                Class<?> aClass = classes[genericParameterToGet];
                if (aClass != TypeResolver.Unknown.class) {
                    return classes[genericParameterToGet];
                }
            }

            // NO MATCH, so walk up.
            loopClassCheck = loopClassCheck.getSuperclass();
        }

        // NOTHING! now check interfaces!
        loopClassCheck = classToCheck;
        while (loopClassCheck != Object.class) {
            // check to see if we have what we are looking for on our CURRENT class interfaces
            Type[] genericInterfaces = loopClassCheck.getGenericInterfaces();

            for (Type genericInterface : genericInterfaces) {
                classes = TypeResolver.resolveRawArguments(genericInterface, loopClassCheck);
                if (classes.length > genericParameterToGet) {
                    Class<?> aClass = classes[genericParameterToGet];
                    if (aClass != TypeResolver.Unknown.class) {
                        return aClass;
                    }
                }
            }

            // NO MATCH, so walk up.
            loopClassCheck = loopClassCheck.getSuperclass();
        }

        // couldn't find it.
        return null;
    }

    // from: https://github.com/square/retrofit/blob/108fe23964b986107aed352ba467cd2007d15208/retrofit/src/main/java/retrofit2/Utils.java
    public static Class<?> getRawType(Type type) {
        Objects.requireNonNull(type, "type == null");

        if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        }
        if (type instanceof TypeVariable) {
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;
        }
        if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }

        throw new IllegalArgumentException(
                "Expected a Class, ParameterizedType, or "
                + "GenericArrayType, but <"
                + type
                + "> is of type "
                + type.getClass().getName());
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
