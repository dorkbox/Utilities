package dorkbox.util.serialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;

import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

public
class SerializationDefaults {
    /**
     * Allows for the kryo registration of sensible defaults in a common, well used way.
     */
    public static
    void register(Kryo kryo) {
        // these are registered using the default serializers. We don't customize these, because we don't care about it.
        kryo.register(String.class);
        kryo.register(String[].class);

        kryo.register(int[].class);
        kryo.register(short[].class);
        kryo.register(float[].class);
        kryo.register(double[].class);
        kryo.register(long[].class);
        kryo.register(byte[].class);
        kryo.register(char[].class);
        kryo.register(boolean[].class);

        kryo.register(Integer[].class);
        kryo.register(Short[].class);
        kryo.register(Float[].class);
        kryo.register(Double[].class);
        kryo.register(Long[].class);
        kryo.register(Byte[].class);
        kryo.register(Character[].class);
        kryo.register(Boolean[].class);

        kryo.register(Object[].class);
        kryo.register(Object[][].class);
        kryo.register(Class.class);

        kryo.register(NullPointerException.class);

        // necessary for the transport of exceptions.
        kryo.register(StackTraceElement.class);
        kryo.register(StackTraceElement[].class);

        kryo.register(ArrayList.class);
        kryo.register(HashMap.class);
        kryo.register(HashSet.class);

        kryo.register(Collections.emptyList().getClass());
        kryo.register(Collections.emptySet().getClass());
        kryo.register(Collections.emptyMap().getClass());

        kryo.register(Collections.emptyNavigableSet().getClass());
        kryo.register(Collections.emptyNavigableMap().getClass());


        // hacky way to register unmodifiable serializers
        Kryo kryoHack = new Kryo() {
            @Override
            public
            Registration register(final Class type, final Serializer serializer) {
                kryo.register(type, serializer);
                return null;
            }
        };

        UnmodifiableCollectionsSerializer.registerSerializers(kryoHack);
    }
}
