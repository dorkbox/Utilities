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
