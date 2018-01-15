package dorkbox.util.serialization;

import java.io.File;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serialize the path of a file instead of the File object
 */
public
class FileSerializer extends Serializer<File> {

    @Override
    public
    void write(Kryo kryo, Output output, File file) {
        output.writeString(file.getPath());
    }

    @Override
    public
    File read(Kryo kryo, Input input, Class<File> type) {
        String path = input.readString();
        return new File(path);
    }
}
