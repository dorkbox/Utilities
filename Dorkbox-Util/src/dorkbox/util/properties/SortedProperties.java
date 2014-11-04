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
package dorkbox.util.properties;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class SortedProperties extends Properties {

    private static final long serialVersionUID = 3988064683926999433L;

    private final Comparator<Object> compare = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }};

    @Override
    public synchronized Enumeration<Object> keys() {
        Enumeration<Object> keysEnum = super.keys();

        Vector<Object> vector = new Vector<Object>(size());
        for (;keysEnum.hasMoreElements();) {
            vector.add(keysEnum.nextElement());
        }

        Collections.sort(vector, this.compare);

        return vector.elements();
    }
}
