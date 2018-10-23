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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallFunctionMapper;

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

    @SuppressWarnings({"unchecked", "unused"})
    public static class Bootstrap {
        public static final int JNI_VERSION_1_1 = 0x00010001;
        public static final int JNI_VERSION_1_2 = 0x00010002;
        public static final int JNI_VERSION_1_4 = 0x00010004;
        public static final int JNI_VERSION_1_6 = 0x00010006;
        public static final int JNI_VERSION_1_7 = 0x00010007;
        public static final int JNI_VERSION_1_8 = 0x00010008;

        // if we want to change the JNI version, this is how we do it.
        public static int JNI_VERSION = JNI_VERSION_1_4;

        private static JVM libjvm;

        public static
        class JavaVM extends Structure {
            public static
            class ByReference extends JavaVM implements Structure.ByReference {}

            public volatile JNIInvokeInterface.ByReference functions;

            JavaVM() {
            }

            @Override
            protected
            List<String> getFieldOrder() {
                //noinspection ArraysAsListWithZeroOrOneArgument
                return Arrays.asList("functions");
            }
        }

        // Note: this does not work in java8 x86 *on windows XP windows7, etc. It only works on x64
        @SuppressWarnings("UnusedReturnValue")
        public
        interface JVM extends com.sun.jna.Library {
            void JVM_DefineClass(Pointer env, String name, Object loader, byte[] buffer, int length, Object protectionDomain);
            int JNI_GetCreatedJavaVMs(JavaVM.ByReference[] vmArray, int bufsize, int[] vmCount);
        }

        @SuppressWarnings("unused")
        public static
        class JNIInvokeInterface extends Structure {
            public static
            class ByReference extends JNIInvokeInterface implements Structure.ByReference {}

            public volatile Pointer reserved0;
            public volatile Pointer reserved1;
            public volatile Pointer reserved2;

            public volatile Pointer DestroyJavaVM;
            public volatile Pointer AttachCurrentThread;
            public volatile Pointer DetachCurrentThread;

            public volatile GetEnv GetEnv;
            public volatile Pointer AttachCurrentThreadAsDaemon;

            @Override
            protected
            List getFieldOrder() {
                return Arrays.asList("reserved0",
                                     "reserved1",
                                     "reserved2",
                                     "DestroyJavaVM",
                                     "AttachCurrentThread",
                                     "DetachCurrentThread",
                                     "GetEnv",
                                     "AttachCurrentThreadAsDaemon");
            }

            public
            interface GetEnv extends com.sun.jna.Callback {
                int callback(JavaVM.ByReference vm, PointerByReference penv, int version);
            }
        }

        static {
            String libName;
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
            if (OS.isWindows() && OS.is32bit()) {
                Map options = new HashMap();
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
                libjvm = Native.loadLibrary(libName, JVM.class, options);
            } else {
                libjvm = Native.loadLibrary(libName, JVM.class);
            }
        }

        /**
         * Inject class bytes directly into the bootstrap classloader.
         * <p>
         * This is a VERY DANGEROUS method to use!
         *
         * @param classBytes
         *                 the bytes to inject
         */
        public static
        void defineClass(byte[] classBytes) throws Exception {
            // get the number of JVM's running
            int[] jvmCount = {100};
            libjvm.JNI_GetCreatedJavaVMs(null, 0, jvmCount);

            // actually get the JVM's
            JavaVM.ByReference[] vms = new JavaVM.ByReference[jvmCount[0]];
            for (int i = 0, vmsLength = vms.length; i < vmsLength; i++) {
                vms[i] = new JavaVM.ByReference();
            }

            // now get the JVM's
            libjvm.JNI_GetCreatedJavaVMs(vms, vms.length, jvmCount);

            Exception exception = null;
            for (int i = 0; i < jvmCount[0]; ++i) {
                JavaVM.ByReference vm = vms[i];
                PointerByReference penv = new PointerByReference();
                vm.functions.GetEnv.callback(vm, penv, ClassLoaderUtil.Bootstrap.JNI_VERSION);

                // inject into all JVM's that are started by us (is USUALLY 1, but not always)
                try {
                    libjvm.JVM_DefineClass(penv.getValue(), null, null, classBytes, classBytes.length, null);
                } catch (Exception e) {
                    exception = e;
                }
            }

            // something failed, just show us THE LAST of the failures
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * Defines a class in the current threads class-loader
     * @param bytes the bytes of the class to define
     */
    public static
    void defineClass(final byte[] bytes) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        defineClass.invoke(classLoader, bytes, 0, bytes.length);
    }
}
