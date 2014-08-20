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
