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
package dorkbox.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.win32.StdCallFunctionMapper;

// http://hg.openjdk.java.net/jdk/jdk10/file/b09e56145e11/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk10/jdk10/jdk/file/777356696811/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk9/jdk9/jdk/file/65464a307408/src/java.base/share/native/libjava/ClassLoader.c
// http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/be698ac28848/src/share/native/java/lang/ClassLoader.c
// http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/tip/src/share/vm/prims/jvm.cpp

// objdump -T <file> | grep foo
// otool -T <file> | grep foo

/**
 * Gives us the ability to inject bytes into the "normal" classloader, or directly into java's bootstrap classloader.
 * <p>
 * When injecting into the bootstrap classloader, this COMPLETELY bypass all security checks, as it calls native methods directly via JNA.
 */
@SuppressWarnings("WeakerAccess")
public class ClassLoaderUtil {
    private static JVM libjvm;

    // Note: this does not work in java8 x86 *on windows XP windows7, etc. It only works on x64
    @SuppressWarnings("UnusedReturnValue")
    public
    interface JVM extends com.sun.jna.Library {
        void JVM_DefineClass(JNIEnv env, String name, Object classLoader, byte[] buffer, int length, Object protectionDomain);
        // Class JVM_FindLoadedClass(JNIEnv env, Object classLoader, JString name);
    }

    private static final String libName;
    static {
        if (OS.isMacOsX()) {
            if (OS.javaVersion < 7) {
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
        Map options = new HashMap();
        options.put(Library.OPTION_ALLOW_OBJECTS, Boolean.TRUE);

        if (OS.isWindows() && OS.is32bit()) {
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
            libjvm = Native.load(libName, JVM.class, options);
        } else {
            libjvm = Native.load(libName, JVM.class, options);
        }
    }


    /**
     * Defines a class in the current threads class-loader
     *
     * @param classBytes the bytes of the class to define
     */
    public static
    void defineClass(final byte[] classBytes) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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
    public static
    void defineClass(ClassLoader classLoader, byte[] classBytes) throws Exception {
        // inject into the FIRST JVM that are started by us (is USUALLY 1, but not always)
        libjvm.JVM_DefineClass(JNIEnv.CURRENT, null, classLoader, classBytes, classBytes.length, null);
    }
}
