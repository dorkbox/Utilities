package dorkbox.jna;

class ClassLoaderAccessory {
    // this class is to be overridden. This is on purpose, since this allows us to SKIP reflection when accessing classes, as we can
    // redefine this class at runtime (and at compile time, it is accessible)
    protected static Class findLoadedClass(ClassLoader classLoader, String className) {
        return null;
    }
}
