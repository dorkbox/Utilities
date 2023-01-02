/*
 * Copyright 2016 dorkbox, llc
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
package dorkbox.jna;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.win32.StdCallFunctionMapper;

import dorkbox.os.OS;

// http://hg.openjdk.java.net/jdk/jdk10/file/b09e56145e11/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk10/jdk10/jdk/file/777356696811/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk9/jdk9/jdk/file/65464a307408/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/be698ac28848/src/share/native/java/lang/ClassLoader.c
// http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/tip/src/share/vm/prims/jvm.cpp

// objdump -T <file> | grep foo
// otool -tvV <file> | grep foo

/**
 * Gives us the ability to inject bytes into the "normal" classloader, or directly into java's bootstrap classloader.
 * <p>
 * When injecting into the bootstrap classloader, this COMPLETELY bypass all security checks, as it calls native methods directly via JNA.
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class ClassUtils {
    private static final JVM libJvm;

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "1.33";
    }


    // Note: this does not work in java8 x86 *on windows XP windows7, etc. It only works on x64
    @SuppressWarnings("UnusedReturnValue")
    public
    interface JVM extends com.sun.jna.Library {
        void JVM_DefineClass(JNIEnv env, String name, Object classLoader, byte[] buffer, int length, Object protectionDomain);
        // Class JVM_FindLoadedClass(JNIEnv env, Object classLoader, JString name);
    }

    private static final String libName;
    static {
        if (OS.INSTANCE.isMacOsX()) {
            if (OS.INSTANCE.getJavaVersion() < 7) {
                libName = "JavaVM";
            } else {
                String javaLocation = System.getProperty("java.home");

                // have to explicitly specify the JVM library via full path
                // this is OK, because for java on MacOSX, this is the only location it can exist
                libName = javaLocation + "/lib/server/libjvm.dylib";
            }
        }
        else {
            libName = "jvm";
        }


        // function name is SLIGHTLY different on windows x32 java builds.
        // For actual name use: http://www.nirsoft.net/utils/dll_export_viewer.html
        //noinspection rawtypes
        Map options = new HashMap();
        options.put(Library.OPTION_ALLOW_OBJECTS, Boolean.TRUE);

        if (OS.INSTANCE.isWindows() && OS.INSTANCE.is32bit()) {
            options.put(Library.OPTION_FUNCTION_MAPPER, new StdCallFunctionMapper() {
                @Override
                public
                String getFunctionName(NativeLibrary library, Method method) {
                    String methodName = method.getName();
                    if (methodName.equals("JVM_DefineClass")) {
                        // specifically Oracle Java 32bit builds. Tested on XP and Win7
                        return "_JVM_DefineClass@24";
                    }
                    return methodName;
                }
            });
            libJvm = Native.load(libName, JVM.class, options);
        } else {
            libJvm = Native.load(libName, JVM.class, options);
        }
    }


    /**
     * Defines a class in the current threads class-loader
     *
     * @param classBytes the bytes of the class to define
     */
    public static
    void defineClass(final byte[] classBytes) throws Exception {
        java.lang.ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        defineClass(classLoader, classBytes);
    }

    /**
     * Inject class bytes directly into the bootstrap classloader.
     * <p>
     * This is a VERY DANGEROUS method to use!
     *
     * @param classLoader the classLoader to use. null will use the BOOTSTRAP classloader
     * @param classBytes the bytes to inject
     */
    @SuppressWarnings("RedundantThrows")
    public static
    void defineClass(java.lang.ClassLoader classLoader, byte[] classBytes) throws Exception {
        // inject into the FIRST JVM that are started by us (is USUALLY 1, but not always)
        libJvm.JVM_DefineClass(JNIEnv.CURRENT, null, classLoader, classBytes, classBytes.length, null);
    }


    private static
    AtomicBoolean loaded = new AtomicBoolean(false);


    /**
     * Finds the class in the specified classloader without the use of reflection
     */
    public static Class<?> findLoadedClass(ClassLoader classLoader, String className) {
        // we rewrite the ClassFixer at runtime, as appropriate
        if (!loaded.getAndSet(true)) {
            // we do not have a dependency on javassist simply because we ALREADY know what the generated class bytes are (so there's no need)


            // try {
            //     ClassPool pool = ClassPool.getDefault();
            //     CtClass dynamicClass = pool.makeClass("java.lang.ClassLoaderAccessor");
            //     CtMethod method = CtNewMethod.make("public static Class findLoadedClass(ClassLoader classLoader, String className) { " +
            //                                             "return classLoader.findLoadedClass(className);" +
            //                                        "}", dynamicClass);
            //     dynamicClass.addMethod(method);
            //     dynamicClass.setModifiers(dynamicClass.getModifiers() & ~Modifier.STATIC);
            //
            //     final byte[] classLoaderAccessorBytes = dynamicClass.toBytecode();
            //     Sys.printArray(classLoaderAccessorBytes);
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }
            //
            byte[] classLoaderAccessorBytes = {
                    -54,-2,-70,-66,0,0,0,52,0,19,1,0,29,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,76,111,97,100,101,114,65,99,99,101,115,115,111,
                    114,7,0,1,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,7,0,3,1,0,10,83,111,117,114,99,101,70,105,108,101,1,
                    0,24,67,108,97,115,115,76,111,97,100,101,114,65,99,99,101,115,115,111,114,46,106,97,118,97,1,0,15,102,105,110,100,76,111,97,100,101,100,67,
                    108,97,115,115,1,0,60,40,76,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,76,111,97,100,101,114,59,76,106,97,118,97,47,108,97,110,
                    103,47,83,116,114,105,110,103,59,41,76,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,59,1,0,21,106,97,118,97,47,108,97,110,103,47,
                    67,108,97,115,115,76,111,97,100,101,114,7,0,9,1,0,37,40,76,106,97,118,97,47,108,97,110,103,47,83,116,114,105,110,103,59,41,76,106,97,
                    118,97,47,108,97,110,103,47,67,108,97,115,115,59,12,0,7,0,11,10,0,10,0,12,1,0,4,67,111,100,101,1,0,6,60,105,110,105,116,62,
                    1,0,3,40,41,86,12,0,15,0,16,10,0,4,0,17,0,33,0,2,0,4,0,0,0,0,0,2,0,9,0,7,0,8,0,1,0,14,0,0,
                    0,18,0,2,0,2,0,0,0,6,42,43,-74,0,13,-80,0,0,0,0,0,1,0,15,0,16,0,1,0,14,0,0,0,17,0,1,0,1,0,0,
                    0,5,42,-73,0,18,-79,0,0,0,0,0,1,0,5,0,0,0,2,0,6};
            try {
                ClassUtils.defineClass(null, classLoaderAccessorBytes);
            } catch (Exception e) {
                // we are fully expecting this to be available. completely die if it's not!
                throw new RuntimeException(e);
            }


            // try {
            //     ClassPool pool = ClassPool.getDefault();
            //     CtClass classFixer = pool.get("dorkbox.jna.ClassLoaderAccessory");
            //     CtMethod ctMethod = classFixer.getDeclaredMethod("findLoadedClass");
            //     ctMethod.setBody("{" +
            //          "return java.lang.ClassLoaderAccessor.findLoadedClass($1, $2);" +
            //     "}");
            //
            //     // perform pre-verification for the modified method
            //     ctMethod.getMethodInfo().rebuildStackMapForME(pool);
            //
            //     final byte[] classFixerBytes = classFixer.toBytecode();
            //     Sys.printArray(classFixerBytes);
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }
            byte[] classFixerBytes = {
                    -54,-2,-70,-66,0,0,0,52,0,22,1,0,32,100,111,114,107,98,111,120,47,106,110,97,47,67,108,97,115,115,76,111,97,100,101,114,65,99,99,101,115,
                    115,111,114,121,7,0,1,1,0,16,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,7,0,3,1,0,6,60,105,110,105,116,62,1,0,
                    3,40,41,86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,78,117,109,98,101,114,84,97,98,108,101,1,0,18,76,111,99,97,108,86,97,114,
                    105,97,98,108,101,84,97,98,108,101,1,0,4,116,104,105,115,1,0,34,76,100,111,114,107,98,111,120,47,106,110,97,47,67,108,97,115,115,76,111,
                    97,100,101,114,65,99,99,101,115,115,111,114,121,59,12,0,5,0,6,10,0,4,0,12,1,0,15,102,105,110,100,76,111,97,100,101,100,67,108,97,
                    115,115,1,0,60,40,76,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,76,111,97,100,101,114,59,76,106,97,118,97,47,108,97,110,103,47,
                    83,116,114,105,110,103,59,41,76,106,97,118,97,47,108,97,110,103,47,67,108,97,115,115,59,1,0,29,106,97,118,97,47,108,97,110,103,47,67,108,
                    97,115,115,76,111,97,100,101,114,65,99,99,101,115,115,111,114,7,0,16,12,0,14,0,15,10,0,17,0,18,1,0,10,83,111,117,114,99,101,70,
                    105,108,101,1,0,25,67,108,97,115,115,76,111,97,100,101,114,65,99,99,101,115,115,111,114,121,46,106,97,118,97,0,32,0,2,0,4,0,0,0,
                    0,0,2,0,0,0,5,0,6,0,1,0,7,0,0,0,47,0,1,0,1,0,0,0,5,42,-73,0,13,-79,0,0,0,2,0,8,0,0,0,6,
                    0,1,0,0,0,3,0,9,0,0,0,12,0,1,0,0,0,5,0,10,0,11,0,0,0,12,0,14,0,15,0,1,0,7,0,0,0,18,0,2,
                    0,2,0,0,0,6,42,43,-72,0,19,-80,0,0,0,0,0,1,0,20,0,0,0,2,0,21};

            try {
                // specifically not in the bootstrap classloader -- but instead in the classloader initially searched (which might be the wrong one)
                ClassUtils.defineClass(classLoader, classFixerBytes);
            } catch (Exception e) {
                // we are fully expecting this to be available. completely die if it's not!
                throw new RuntimeException(e);
            }
        }

        return ClassLoaderAccessory.findLoadedClass(classLoader, className);
    }

    /**
     * Checks if the specified classloader has the class loaded or not, without the use of reflection
     */
    public static boolean isClassLoaded(ClassLoader classLoader, String className) {
        return null != findLoadedClass(classLoader, className);
    }
}
