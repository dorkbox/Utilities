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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import dorkbox.util.FileUtil;

public class PropertiesProvider {

    // the basePath for properties based settings. In JAVA proper, this is by default relative to the jar location.
    // in ANDROID dalvik, this must be specified to be the location of the APK plus some extra info. This must be set by the android app.
    public static String basePath = "";

    private String comments = "Settings and configuration file. Strings must be escape formatted!";
    private final Properties properties = new SortedProperties();
    private final File propertiesFile;

    public PropertiesProvider(String propertiesFile) {
        this(new File(propertiesFile));
    }

    public PropertiesProvider(File propertiesFile) {
        if (propertiesFile == null) {
            throw new NullPointerException("propertiesFile");
        }

        propertiesFile = FileUtil.normalize(propertiesFile);
        // make sure the parent dir exists...
        File parentFile = propertiesFile.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }

        this.propertiesFile = propertiesFile;

        _load();
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    private final void _load() {
        if (!this.propertiesFile.canRead() || !this.propertiesFile.exists()) {
            // in this case, our properties file doesn't exist yet... create one!
            _save();
        }

        try {
            FileInputStream fis = new FileInputStream(this.propertiesFile);
            this.properties.load(fis);
            fis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // oops!
            System.err.println("Properties cannot load!");
            e.printStackTrace();
        }
    }


    private final void _save() {
        try {
            FileOutputStream fos = new FileOutputStream(this.propertiesFile);
            this.properties.store(fos, this.comments);
            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Properties cannot save!");
        } catch (IOException e) {
            // oops!
            System.err.println("Properties cannot save!");
            e.printStackTrace();
        }
    }


    public synchronized final void remove(final String key) {
        this.properties.remove(key);
        _save();
    }

    public synchronized final void save(final String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        if (value instanceof Color) {
            value = ((Color)value).getRGB();
        }

        this.properties.setProperty(key, value.toString());

        _save();
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T get(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }

        String property = this.properties.getProperty(key);
        if (property == null) {
            return null;
        }

        // special cases
        try {
            if (clazz.equals(Integer.class)) {
                return (T) new Integer(Integer.parseInt(property));
            }
            if (clazz.equals(Long.class)) {
                return (T) new Long(Long.parseLong(property));
            }
            if (clazz.equals(Color.class)) {
                return (T) new Color(new Integer(Integer.parseInt(property)), true);
            }

            else {
                return (T) property;
            }
        } catch (Exception e) {
            throw new RuntimeException("Properties Loader for property: " + key + System.getProperty("line.separator") + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "PropertiesProvider [" + this.propertiesFile + "]";
    }
}
