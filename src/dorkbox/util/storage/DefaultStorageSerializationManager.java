/*
 * Copyright 2014 dorkbox, llc
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
package dorkbox.util.storage;

import java.io.IOException;

import org.slf4j.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

import dorkbox.util.SerializationManager;
import io.netty.buffer.ByteBuf;

class DefaultStorageSerializationManager implements SerializationManager {
    private Kryo kryo = new Kryo() {{
            // we don't want logging from Kryo...
            Log.set(Log.LEVEL_ERROR);
        }};

    @Override
    public
    SerializationManager register(final Class<?> clazz) {
        kryo.register(clazz);
        return this;
    }

    @Override
    public
    SerializationManager register(final Class<?> clazz, final Serializer<?> serializer) {
        kryo.register(clazz, serializer);
        return this;
    }

    @Override
    public
    SerializationManager register(final Class<?> type, final Serializer<?> serializer, final int id) {
        kryo.register(type, serializer, id);
        return this;
    }

    @Override
    public
    void write(final ByteBuf buffer, final Object message) {
        final Output output = new Output();
        writeFullClassAndObject(null, output, message);
        buffer.writeBytes(output.getBuffer());
    }

    @Override
    public
    Object read(final ByteBuf buffer, final int length) throws IOException {
        final Input input = new Input();
        buffer.readBytes(input.getBuffer());

        final Object o = readFullClassAndObject(null, input);
        buffer.skipBytes(input.position());

        return o;
    }

    @Override
    public
    void writeFullClassAndObject(final Logger logger, final Output output, final Object value) {
        kryo.writeClassAndObject(output, value);
    }

    @Override
    public
    Object readFullClassAndObject(final Logger logger, final Input input) throws IOException {
        return kryo.readClassAndObject(input);
    }

    @Override
    public
    void finishInit() {
    }

    @Override
    public
    boolean initialized() {
        return false;
    }
}
