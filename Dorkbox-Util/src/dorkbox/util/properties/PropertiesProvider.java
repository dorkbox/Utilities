package dorkbox.util.properties;


import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesProvider {

    // the basePath for properties based settings. In JAVA proper, this is by default relative to the jar location.
    // in ANDROID dalvik, this must be specified to be the location of the APK plus some extra info. This must be set by the android app.
    public static String basePath = "";

    private final Properties properties = new SortedProperties();
    private final File propertiesFile;

    public PropertiesProvider(File propertiesFile) {
        propertiesFile = propertiesFile.getAbsoluteFile();
        // make sure the parent dir exists...
        File parentFile = propertiesFile.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }

        this.propertiesFile = propertiesFile;

        _load();
    }

    private final void _load() {
        if (!propertiesFile.canRead() || !propertiesFile.exists()) {
            // in this case, our properties file doesn't exist yet... create one!
            _save();
        }

        try {
            FileInputStream fis = new FileInputStream(propertiesFile);
            properties.load(fis);
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
            FileOutputStream fos = new FileOutputStream(propertiesFile);
            properties.store(fos, "Settings and configuration file. Strings must be escape formatted!");
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
        properties.remove(key);
        _save();
    }

    public synchronized final void save(final String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        if (value instanceof Color) {
            value = ((Color)value).getRGB();
        }

        properties.setProperty(key, value.toString());

        _save();
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T get(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }

        String property = properties.getProperty(key);
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
        return "PropertiesProvider [" + propertiesFile + "]";
    }
}
