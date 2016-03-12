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
import com.esotericsoftware.reflectasm.FieldAccess;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * Only public keys are ever sent across the wire.
 */
public
class EccPrivateKeySerializer extends Serializer<ECPrivateKeyParameters> {

    // we use ASM here
    private static final FieldAccess ecCurveAccess = FieldAccess.get(ECCurve.class);
    private static final int ecCoordIndex = ecCurveAccess.getIndex("coord");


    private static final byte usesName = (byte) 1;
    private static final byte usesOid = (byte) 2;

    public static
    void write(Output output, ECPrivateKeyParameters key) {
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


        serializeECPoint(g, output);

        /////////////
        bytes = key.getD()
                   .toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);
    }

    public static
    ECPrivateKeyParameters read(Input input) {
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


        // D
        /////////////
        length = input.readInt(true);
        bytes = new byte[length];
        input.readBytes(bytes, 0, length);
        BigInteger D = new BigInteger(bytes);


        ECDomainParameters ecDomainParameters = new ECDomainParameters(curve, g, n);

        return new ECPrivateKeyParameters(D, ecDomainParameters);
    }

    static
    void serializeCurve(Output output, ECCurve curve) {
        byte[] bytes;
        int length;
        // save out if it's a NAMED curve, or a UN-NAMED curve. If it is named, we can do less work.
        String curveName = curve.getClass()
                                .getSimpleName();

        if (CustomNamedCurves.getByName(curveName) != null) {
            // we use the name instead of serializing the full curve
            output.writeInt(usesName, true);
            output.writeString(curveName);
            return;
        }

        else if (curveName.endsWith("Curve")) {
            String cleanedName = curveName.substring(0, curveName.indexOf("Curve"));

            if (!cleanedName.isEmpty()) {
                ASN1ObjectIdentifier oid = CustomNamedCurves.getOID(cleanedName);
                if (oid != null) {
                    // we use the OID (instead of serializing the entire curve)
                    output.writeInt(usesOid, true);
                    curveName = oid.getId();
                    output.writeString(curveName);
                    return;
                }
            }
        }

        // we have to serialize the ENTIRE curve.
        // save out the curve info
        BigInteger a = curve.getA()
                            .toBigInteger();
        BigInteger b = curve.getB()
                            .toBigInteger();
        BigInteger order = curve.getOrder();
        BigInteger cofactor = curve.getCofactor();
        BigInteger q = curve.getField()
                            .getCharacteristic();

        /////////////
        bytes = a.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        /////////////
        bytes = b.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);

        /////////////
        bytes = order.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = cofactor.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        /////////////
        bytes = q.toByteArray();
        length = bytes.length;
        output.writeInt(length, true);
        output.writeBytes(bytes, 0, length);


        // coordinate system
        int coordinateSystem = curve.getCoordinateSystem();
        output.writeInt(coordinateSystem, true);
    }

    static
    ECCurve deserializeCurve(Input input) {
        byte[] bytes;
        int length;

        ECCurve curve;
        int serializatioType = input.readInt(true);

        // lookup via name
        if (serializatioType == usesName) {
            String curveName = input.readString();
            X9ECParameters x9Curve = CustomNamedCurves.getByName(curveName);
            curve = x9Curve.getCurve();
        }

        // this means we just lookup the curve via the OID
        else if (serializatioType == usesOid) {
            String oid = input.readString();
            X9ECParameters x9Curve = CustomNamedCurves.getByOID(new ASN1ObjectIdentifier(oid));
            curve = x9Curve.getCurve();
        }

        // we have to read in the entire curve information.
        else {
            /////////////
            length = input.readInt(true);
            bytes = new byte[length];
            input.readBytes(bytes, 0, length);
            BigInteger a = new BigInteger(bytes);

            /////////////
            length = input.readInt(true);
            bytes = new byte[length];
            input.readBytes(bytes, 0, length);
            BigInteger b = new BigInteger(bytes);

            /////////////
            length = input.readInt(true);
            bytes = new byte[length];
            input.readBytes(bytes, 0, length);
            BigInteger order = new BigInteger(bytes);

            /////////////
            length = input.readInt(true);
            bytes = new byte[length];
            input.readBytes(bytes, 0, length);
            BigInteger cofactor = new BigInteger(bytes);

            /////////////
            length = input.readInt(true);
            bytes = new byte[length];
            input.readBytes(bytes, 0, length);
            BigInteger q = new BigInteger(bytes);


            // coord system
            int coordinateSystem = input.readInt(true);

            curve = new ECCurve.Fp(q, a, b, order, cofactor);
            ecCurveAccess.setInt(curve, ecCoordIndex, coordinateSystem);
        }
        return curve;
    }

    static
    void serializeECPoint(ECPoint point, Output output) {
        if (point.isInfinity()) {
            return;
        }

        ECPoint normed = point.normalize();

        byte[] X = normed.getXCoord()
                         .getEncoded();
        byte[] Y = normed.getYCoord()
                         .getEncoded();

        int length = 1 + X.length + Y.length;
        output.writeInt(length, true);

        output.write(0x04);
        output.write(X);
        output.write(Y);
    }

    @Override
    public
    void write(Kryo kryo, Output output, ECPrivateKeyParameters key) {
        write(output, key);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public
    ECPrivateKeyParameters read(Kryo kryo, Input input, Class type) {
        return read(input);
    }
}
