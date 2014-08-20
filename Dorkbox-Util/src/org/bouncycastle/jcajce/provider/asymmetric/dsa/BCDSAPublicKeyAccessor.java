package org.bouncycastle.jcajce.provider.asymmetric.dsa;

import java.math.BigInteger;
import java.security.spec.DSAParameterSpec;

public class BCDSAPublicKeyAccessor {
    public static BCDSAPublicKey newInstance(BigInteger bigInteger, DSAParameterSpec dsaParameterSpec) {
        return new BCDSAPublicKey(bigInteger, dsaParameterSpec);
    }
}
