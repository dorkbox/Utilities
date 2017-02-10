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
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * Only public keys are ever sent across the wire.
 */
public
class EccPublicKeySerializer extends Serializer<ECPublicKeyParameters> {

    public static
    void write(Output output, ECPublicKeyParameters key) {
        byte[] bytes;
        int length;

        ECDomainParameters parameters = key.getParameters();
        ECCurve curve = parameters.getCurve();

        EccPrivateKeySerializer.serializeCurve(output, curve);

        /////////////
        BigInteger n = parameters.getN();
        ECPoint g = parameters.getG();


        /////////////
        bytes = n.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        EccPrivateKeySerializer.serializeECPoint(g, output);
        EccPrivateKeySerializer.serializeECPoint(key.getQ(), output);
    }

    public static
    ECPublicKeyParameters read(Input input) {
        byte[] bytes;
        int length;

        ECCurve curve = EccPrivateKeySerializer.deserializeCurve(input);


        // N
        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger n = new BigInteger(bytes);


        // G
        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        ECPoint g = curve.decodePoint(bytes);


        ECDomainParameters ecDomainParameters = new ECDomainParameters(curve, g, n);


        // Q
        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        ECPoint Q = curve.decodePoint(bytes);

        return new ECPublicKeyParameters(Q, ecDomainParameters);
    }

    @Override
    public
    void write(Kryo kryo, Output output, ECPublicKeyParameters key) {
        write(output, key);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public
    ECPublicKeyParameters read(Kryo kryo, Input input, Class type) {
        return read(input);
    }
}
