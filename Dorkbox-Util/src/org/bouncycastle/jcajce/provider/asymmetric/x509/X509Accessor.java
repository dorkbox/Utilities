package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;

public class X509Accessor {

    /**
     * Verify the TIME/DATE of the certificate
     * Stupid BC is package private, so this will let us access this method.
     */
    public static void verifyDate(java.security.cert.Certificate certificate) throws CertificateExpiredException, CertificateNotYetValidException {
        // TODO: when checking the validite of the certificate, it is important to use a date from somewhere other than the
        // host computer! (maybe use google? or something...)
        // this will validate the DATES of the certificate, to make sure the cert is valid during the correct time period.

        org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateObject cert = (org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateObject) certificate;
        cert.checkValidity(new Date());
    }
}
