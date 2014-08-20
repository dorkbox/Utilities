package dorkbox.util.crypto.serialization;

import java.math.BigInteger;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 *  Only public keys are ever sent across the wire.
 */
public class RsaPublicKeySerializer extends Serializer<RSAKeyParameters> {

    @Override
    public void write(Kryo kryo, Output output, RSAKeyParameters key) {
        byte[] bytes;
        int length;

        ///////////
        bytes = key.getModulus().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        /////////////
        bytes = key.getExponent().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public RSAKeyParameters read (Kryo kryo, Input input, Class type) {

        byte[] bytes;
        int length;

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger modulus = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger exponent = new BigInteger(bytes);

        return new RSAKeyParameters(false, modulus, exponent);
     }
}
