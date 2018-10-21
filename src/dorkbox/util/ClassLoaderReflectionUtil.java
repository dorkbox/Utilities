package dorkbox.util;

import java.lang.reflect.Method;

import io.netty.util.internal.ReflectionUtil;

/**
 * Meant to be overridden in java 9+ and injected into the bootstrap classloader
 */
public
class ClassLoaderReflectionUtil {
    /**
     * Defines a class in the current threads class-loader
     * @param bytes the bytes of the class to define
     */
    protected static
    void defineClass(final byte[] bytes) throws Exception {
        // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // ClassLoaderReflectionUtil.defineClass2(classLoader, bytes);

        try {
            Class<?> bitsClass = Class.forName("java.lang.ForceDefine", false, ClassLoader.getSystemClassLoader());
            Method defineClassMethod = bitsClass.getDeclaredMethod("defineClass2");
            Throwable cause = ReflectionUtil.trySetAccessible(defineClassMethod, true);
            defineClassMethod.invoke(ClassLoader.getSystemClassLoader(), bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static
    void defineClass2(final java.lang.ClassLoader classLoader, final byte[] bytes) throws java.lang.Exception {
        java.lang.reflect.Method defineClass = java.lang.ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        setAccessible(defineClass);
        defineClass.invoke(classLoader, bytes, 0, bytes.length);
    }

    public static
    void setAccessible(final Method defineClass) {
        System.err.println("WHAT!");
        // defineClass.setAccessible(true);
    }
}
