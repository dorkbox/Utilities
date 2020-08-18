/*
 * Copyright 2018 dorkbox, llc
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
package dorkbox.jna.macos.foundation;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * "how to" from: https://gist.github.com/3974488
 * <p>
 * https://developer.apple.com/documentation/objectivec/objective-c_runtime
 * <p>
 * We cannot use JNA "direct mapping", because direct mapping DOES NOT support var-args (which `objc_msgSend` requires)
 */
public
class ObjectiveC {
    interface Objc extends Library {
        Pointer objc_lookUpClass(String name);

        long objc_msgSend(Pointer theReceiver, Pointer theSelector, Object... string);

        Pointer objc_allocateClassPair(Pointer superClass, String name, long extraBytes);

        void objc_registerClassPair(Pointer clazz);

        Pointer class_createInstance(Pointer clazz, int extraBytes);

        boolean class_addMethod(Pointer clazz, Pointer selector, Callback callback, String types);

        Pointer sel_getUid(String name);

        Pointer sel_registerName(String name);
    }

    private static final Objc INSTANCE = Native.load("objc", Objc.class);

    /**
     * Returns the class definition of a specified class.
     *
     * @param name The name of the class to look up.
     *
     * @return The Class object for the named class, or nil if the class is not registered with the Objective-C runtime.
     */
    public static
    Pointer objc_lookUpClass(String name) {
        return INSTANCE.objc_lookUpClass(name);
    }

    /**
     * Sends a message with a simple return value to an instance of a class.
     *
     * @param self A pointer that points to the instance of the class that is to receive the message.
     * @param op The selector of the method that handles the message.
     * @param args A variable argument list containing the arguments to the method.
     *
     * @return The return value of the method.
     */
    public static
    long objc_msgSend(Pointer self, Pointer op, Object... args) {
        return INSTANCE.objc_msgSend(self, op, args);
    }

    /**
     * Creates a new class and metaclass.
     *
     * @param superClass The class to use as the new class's superclass, or Nil to create a new root class.
     * @param name The string to use as the new class's name. The string will be copied.
     * @param extraBytes The number of bytes to allocate for indexed ivars at the end of the class and metaclass objects. This should usually be 0.
     *
     * @return The new class, or Nil if the class could not be created (for example, the desired name is already in use).
     */
    public static
    Pointer objc_allocateClassPair(Pointer superClass, String name, long extraBytes) {
        return INSTANCE.objc_allocateClassPair(superClass, name, extraBytes);
    }


    /**
     * Registers a class that was allocated using objc_allocateClassPair.
     *
     * @param cls The class you want to register.
     */
    public static
    void objc_registerClassPair(Pointer cls) {
        INSTANCE.objc_registerClassPair(cls);
    }

    /**
     * Creates an instance of a class, allocating memory for the class in the default malloc memory zone.
     *
     * @param cls The class that you want to allocate an instance of.
     * @param extraBytes An integer indicating the number of extra bytes to allocate. The additional bytes can be used to store additional instance variables beyond those defined in the class definition.
     *
     * @return An instance of the class cls.
     */
    public static
    Pointer class_createInstance(Pointer cls, int extraBytes) {
        return INSTANCE.class_createInstance(cls, extraBytes);
    }

    /**
     * Adds a new method to a class with a given name and implementation.
     * NOTE: class_addMethod will add an override of a superclass's implementation, but will not replace an existing implementation in this class. To change an existing implementation, use method_setImplementation.
     *
     * @param cls The class to which to add a method.
     * @param name A selector that specifies the name of the method being added.
     * @param imp A function which is the implementation of the new method. The function must take at least two arguments—self and _cmd.
     * @param types An array of characters that describe the types of the arguments to the method. For possible values, see Objective-C Runtime Programming Guide > Type Encodings. Since the function must take at least two arguments—self and _cmd, the second and third characters must be "@:" (the first character is the return type).
     *
     * @return YES if the method was added successfully, otherwise NO (for example, the class already contains a method implementation with that name).
     */
    public static
    boolean class_addMethod(Pointer cls, Pointer name, Callback imp, String types) {
        return INSTANCE.class_addMethod(cls, name, imp, types);
    }

    /**
     * Registers a method name with the Objective-C runtime system.
     * NOTE: The implementation of this method is identical to the implementation of sel_registerName.
     *
     * @param str A pointer to a C string. Pass the name of the method you wish to register.
     *
     * @return A pointer of type SEL specifying the selector for the named method.
     */
    public static
    Pointer sel_getUid(String str) {
        return INSTANCE.sel_getUid(str);
    }

    /**
     * Registers a method with the Objective-C runtime system, maps the method name to a selector, and returns the selector value.
     * NOTE: You must register a method name with the Objective-C runtime system to obtain the method’s selector before you can add the method to a class definition. If the method name has already been registered, this function simply returns the selector.
     *
     * @param str A pointer to a C string. Pass the name of the method you wish to register.
     *
     * @return A pointer of type SEL specifying the selector for the named method.
     */
    public static
    Pointer sel_registerName(String str) {
        return INSTANCE.sel_registerName(str);
    }
}
