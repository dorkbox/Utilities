/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import java.math.BigInteger;

/**
 *  Only public keys are ever sent across the wire.
 */
public class RsaPrivateKeySerializer extends Serializer<RSAPrivateCrtKeyParameters> {

    @Override
    public void write(Kryo kryo, Output output, RSAPrivateCrtKeyParameters key) {
        byte[] bytes;
        int length;

        /////////////
        bytes = key.getDP().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        /////////////
        bytes = key.getDQ().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getExponent().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getModulus().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getP().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getPublicExponent().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getQ().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = key.getQInv().toByteArray();
        length = bytes.length;

        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public RSAPrivateCrtKeyParameters read (Kryo kryo, Input input, Class type) {

        byte[] bytes;
        int length;

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger DP = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger DQ = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger exponent = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger modulus = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger P = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger publicExponent = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger q = new BigInteger(bytes);

        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger qInv = new BigInteger(bytes);

        return new RSAPrivateCrtKeyParameters(modulus, publicExponent, exponent, P, q, DP, DQ, qInv);
     }
}
