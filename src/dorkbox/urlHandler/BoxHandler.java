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
package dorkbox.urlHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public
class BoxHandler extends URLStreamHandler {
    //
    // This is also in the (ClassLoader project) Node!!!, but I didn't want to force a dependency just because of this.
    //
    //
    // The following must ALL be valid URI symbols, defined by RFC 3986: http://tools.ietf.org/html/rfc3986#section-2
    //
    // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=.
    //
    // Any other character needs to be encoded with the percent-encoding (%hh). Each part of the URI has further restrictions about
    // what characters need to be represented by an percent-encoded word.

    /** This is exclusively used to identify if a resource we are requesting is inside of a jar that was already parsed */
    static final char jarUrlSeperator = '*';
    static final char jarPathToken = '/';
    static final char packageToken = '.';

    static final String protocol = "box";

    static final String protocolFull = protocol + ":/";
    static final int protocolLength = protocolFull.length();


    public BoxHandler() {
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new BoxURLConnection(url);
    }

    /**
     * Makes sure that when creating paths, etc, from this URL, that we also make sure to add a token, so
     * our classloader knows where to find the resource.
     *
     * This absolutely MUST not end in special characters. it must be the letters/numbers or a "/". NOTHING ELSE.
     */
    @Override
    protected String toExternalForm(URL url) {
        // ONLY append jarUrlSeperator if we haven't already done so!
        String externalForm = super.toExternalForm(url);

        char jarurlseperator = jarUrlSeperator;

        if (externalForm.indexOf(jarurlseperator) == -1) {
            int length = externalForm.length();
            StringBuilder stringBuilder = new StringBuilder(length + 1);
            stringBuilder.append(externalForm);
            if (length > 1 && externalForm.charAt(length-1) == jarPathToken) {
                stringBuilder.insert(length, jarurlseperator);
            } else {
                stringBuilder.append(jarurlseperator);
            }
            return stringBuilder.toString();
        } else {
            // we've already modified it, don't do it again.
            return externalForm;
        }
    }

    @Override
    public final Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    public final void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    public final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
