package org.bouncycastle.jcajce.provider.asymmetric.rsa;

import org.bouncycastle.crypto.params.RSAKeyParameters;


public class BCRSAPublicKeyAccessor {
    public static BCRSAPublicKey newInstance(RSAKeyParameters publicKey) {
        return new BCRSAPublicKey(publicKey);
    }
}
