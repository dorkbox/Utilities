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

public
class PropertiesProvider {

    private final Properties properties = new SortedProperties();
    private final File propertiesFile;
    private String comments = "Settings and configuration file. Strings must be escape formatted!";

    public
    PropertiesProvider(String propertiesFile) {
        this(new File(propertiesFile));
    }

    public
    PropertiesProvider(File propertiesFile) {
        if (propertiesFile == null) {
            throw new NullPointerException("propertiesFile");
        }

        propertiesFile = FileUtil.normalize(propertiesFile);
        // make sure the parent dir exists...
        File parentFile = propertiesFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new RuntimeException("Unable to create directories for: " + propertiesFile);
            }
        }

        this.propertiesFile = propertiesFile;

        _load();
    }

    public
    void setComments(String comments) {
        this.comments = comments;
    }

    private
    void _load() {
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


    private
    void _save() {
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


    public final synchronized
    void remove(final String key) {
        this.properties.remove(key);
        _save();
    }

    @SuppressWarnings("AutoBoxing")
    public final synchronized
    void save(final String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        if (value instanceof Color) {
            value = ((Color) value).getRGB();
        }

        this.properties.setProperty(key, value.toString());

        _save();
    }

    @SuppressWarnings({"unchecked", "AutoUnboxing"})
    public synchronized
    <T> T get(String key, Class<T> clazz) {
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
                return (T) Integer.valueOf(Integer.parseInt(property));
            }
            if (clazz.equals(Long.class)) {
                return (T) Long.valueOf(Long.parseLong(property));
            }
            if (clazz.equals(Color.class)) {
                return (T) new Color(Integer.parseInt(property), true);
            }

            else {
                return (T) property;
            }
        } catch (Exception e) {
            throw new RuntimeException("Properties Loader for property: " + key + System.getProperty("line.separator") + e.getMessage());
        }
    }

    @Override
    public
    String toString() {
        return "PropertiesProvider [" + this.propertiesFile + "]";
    }
}
