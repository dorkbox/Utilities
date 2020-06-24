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
package dorkbox.util.serialization;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;

public
interface SerializationManager {

    /**
     * Registers the class using the lowest, next available integer ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}.
     * If the class is already registered, the existing entry is updated with the new serializer.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when using this
     * method. The order must be the same at deserialization as it was for serialization.
     */
    <T> SerializationManager register(Class<T> clazz);

    /**
     * Registers the class using the specified ID. If the ID is already in use by the same type, the old entry is overwritten. If the ID
     * is already in use by a different type, a {@link KryoException} is thrown.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * IDs must be the same at deserialization as they were for serialization.
     *
     * @param id Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and String, but
     *         these IDs can be repurposed.
     */
    <T> SerializationManager register(Class<T> clazz, int id);

    /**
     * Registers the class using the lowest, next available integer ID and the specified serializer. If the class is already registered,
     * the existing entry is updated with the new serializer.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when using this
     * method. The order must be the same at deserialization as it was for serialization.
     */
    <T> SerializationManager register(Class<T> clazz, Serializer<T> serializer);

    /**
     * Registers the class using the specified ID and serializer. If the ID is already in use by the same type, the old entry is
     * overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * IDs must be the same at deserialization as they were for serialization.
     *
     * @param id Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and String, but
     *         these IDs can be repurposed.
     */
    <T> SerializationManager register(Class<T> clazz, Serializer<T> serializer, int id);

    /**
     * Waits until a kryo is available to write, using CAS operations to prevent having to synchronize.
     * <p/>
     * No crypto and no sequence number
     * <p/>
     * There is a small speed penalty if there were no kryo's available to use.
     */
    void write(ByteBuf buffer, Object message) throws IOException;

    /**
     * Reads an object from the buffer.
     * <p/>
     * No crypto and no sequence number
     *
     * @param length should ALWAYS be the length of the expected object!
     */
    Object read(ByteBuf buffer, int length) throws IOException;

    /**
     * Writes the class and object using an available kryo instance
     */
    void writeFullClassAndObject(Output output, Object value) throws IOException;

    /**
     * Returns a class read from the input
     */
    Object readFullClassAndObject(final Input input) throws IOException;
}
