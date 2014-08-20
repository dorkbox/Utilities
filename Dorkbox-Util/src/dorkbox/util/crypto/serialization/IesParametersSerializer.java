package dorkbox.util.crypto.serialization;

import org.bouncycastle.crypto.params.IESParameters;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *  Only public keys are ever sent across the wire.
 */
public class IesParametersSerializer extends Serializer<IESParameters> {

    @Override
    public void write(Kryo kryo, Output output, IESParameters key) {
        byte[] bytes;
        int length;

        ///////////
        bytes = key.getDerivationV();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        ///////////
        bytes = key.getEncodingV();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        ///////////
        output.writeInt(key.getMacKeySize(), true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IESParameters read (Kryo kryo, Input input, Class type) {

        int length;

        /////////////
        length = input.readInt(true);
        byte[] derivation = new byte[length];
        input.readBytes(derivation, 0, length);

        /////////////
        length = input.readInt(true);
        byte[] encoding = new byte[length];
        input.readBytes(encoding, 0, length);

        /////////////
        int macKeySize = input.readInt(true);

        return new IESParameters(derivation, encoding, macKeySize);
     }
}
