package dorkbox.util.crypto.serialization;

import org.bouncycastle.crypto.params.IESWithCipherParameters;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *  Only public keys are ever sent across the wire.
 */
public class IesWithCipherParametersSerializer extends Serializer<IESWithCipherParameters> {

    @Override
    public void write(Kryo kryo, Output output, IESWithCipherParameters key) {
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


        ///////////
        output.writeInt(key.getCipherKeySize(), true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IESWithCipherParameters read (Kryo kryo, Input input, Class type) {

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

        /////////////
        int cipherKeySize = input.readInt(true);

        return new IESWithCipherParameters(derivation, encoding, macKeySize, cipherKeySize);
     }
}
