package dorkbox.util.storage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;

public
interface StorageBuilder {
    /**
     * Builds the storage using the specified configuration
     */
    Storage build();

    /**
     * Registers the class using the lowest, next available integer ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}.
     * If the class is already registered, the existing entry is updated with the new serializer.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when using this
     * method. The order must be the same at deserialization as it was for serialization.
     */
    <T> StorageBuilder register(Class<T> clazz);

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
    <T> StorageBuilder register(Class<T> clazz, int id);

    /**
     * Registers the class using the lowest, next available integer ID and the specified serializer. If the class is already registered,
     * the existing entry is updated with the new serializer.
     * <p>
     * Registering a primitive also affects the corresponding primitive wrapper.
     * <p>
     * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when using this
     * method. The order must be the same at deserialization as it was for serialization.
     */
    <T> StorageBuilder register(Class<T> clazz, Serializer<T> serializer);

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
    <T> StorageBuilder register(Class<T> clazz, Serializer<T> serializer, int id);
}
