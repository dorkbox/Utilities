package dorkbox.util.database;

import dorkbox.util.bytes.ByteArrayWrapper;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import java.util.Arrays;

public
class DB_Server {
    /**
     * Address 0.0.0.0/32 may be used as a source address for this host on this network.
     */
    public static final ByteArrayWrapper IP_0_0_0_0 = ByteArrayWrapper.wrap(new byte[] {0, 0, 0, 0});

    // salt + IP address is used for equals!
    private byte[] ipAddress;
    private byte[] salt;

    private ECPrivateKeyParameters privateKey;
    private ECPublicKeyParameters publicKey;

    // must have empty constructor
    public
    DB_Server() {
    }

    public
    byte[] getAddress() {
        if (this.ipAddress == null) {
            return null;
        }
        return this.ipAddress;
    }

    public
    void setAddress(byte[] ipAddress) {
        this.ipAddress = ipAddress;
    }

    public
    byte[] getSalt() {
        return this.salt;
    }

    public
    void setSalt(byte[] salt) {
        this.salt = salt;
    }


    public
    ECPrivateKeyParameters getPrivateKey() {
        return this.privateKey;
    }

    public
    void setPrivateKey(ECPrivateKeyParameters privateKey) {
        this.privateKey = privateKey;
    }


    public
    ECPublicKeyParameters getPublicKey() {
        return this.publicKey;
    }

    public
    void setPublicKey(ECPublicKeyParameters publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public
    int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.ipAddress == null ? 0 : Arrays.hashCode(this.ipAddress));
        return result;
    }

    @Override
    public
    boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DB_Server other = (DB_Server) obj;
        if (this.ipAddress == null) {
            if (other.ipAddress != null) {
                return false;
            }
        }
        else if (!Arrays.equals(this.ipAddress, other.ipAddress)) {
            return false;
        }
        return Arrays.equals(this.salt, other.salt);
    }

    @Override
    public
    String toString() {
        byte[] bytes = this.ipAddress;
        if (bytes != null) {
            return "DB_Server " + Arrays.toString(bytes);
        }

        return "DB_Server [no-ip-set]";
    }
}
