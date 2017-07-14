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

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * Utility methods for SWT. This will override {@link Swt} methods, and is written in Javassist. This is so we don't require a hard
 * dependency on SWT.
 * <p>
 * The methods/fields that this originated from are commented out in the {@link Swt} class.
 */
public
class SwtBytecodeOverride {
    static {
        try {
//            byte[] swtClassBytes = getSwtBytes();
//
//            // whoosh, past the classloader and directly into memory. This will take precedence over the "proper" Swt class
//            BootStrapClassLoader.defineClass(swtClassBytes);

            // now we have to load various classes into the classloader
            Class<?> aClass = Class.forName("org.eclipse.swt.widgets.Display");
            Swt.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static
    void init() {
        // placeholder to initialize the class.
    }

    private static
    byte[] getSwtBytes() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "dorkbox/util/Swt", null, "java/lang/Object", null);

        cw.visitSource("Swt.java", null);

        cw.visitInnerClass("dorkbox/util/Swt$1", null, null, 0);

        cw.visitInnerClass("dorkbox/util/Swt$2", null, null, 0);

        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "currentDisplay", "Lorg/eclipse/swt/widgets/Display;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "currentDisplayThread", "Ljava/lang/Thread;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(35, l0);
            mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/swt/widgets/Display", "getCurrent", "()Lorg/eclipse/swt/widgets/Display;", false);
            mv.visitFieldInsn(PUTSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(37, l1);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/widgets/Display", "getThread", "()Ljava/lang/Thread;", false);
            mv.visitFieldInsn(PUTSTATIC, "dorkbox/util/Swt", "currentDisplayThread", "Ljava/lang/Thread;");
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(38, l2);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(29, l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "Ldorkbox/util/Swt;", null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "init", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(42, l0);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            Label l1 = new Label();
            mv.visitJumpInsn(IFNONNULL, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(43, l2);
            mv.visitLdcInsn(Type.getType("Ldorkbox/util/Swt;"));
            mv.visitMethodInsn(INVOKESTATIC, "org/slf4j/LoggerFactory", "getLogger", "(Ljava/lang/Class;)Lorg/slf4j/Logger;", false);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLineNumber(44, l3);
            mv.visitLdcInsn("Unable to get the current display for SWT. Please create an issue with your OS and Java version so we may further investigate this issue.");
            mv.visitMethodInsn(INVOKEINTERFACE, "org/slf4j/Logger", "error", "(Ljava/lang/String;)V", true);
            mv.visitLabel(l1);
            mv.visitLineNumber(48, l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "dispatch", "(Ljava/lang/Runnable;)V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(52, l0);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/widgets/Display", "syncExec", "(Ljava/lang/Runnable;)V", false);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLineNumber(54, l1);
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("runnable", "Ljava/lang/Runnable;", null, l0, l2, 0);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "isEventThread", "()Z", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(58, l0);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplayThread", "Ljava/lang/Thread;");
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "onShutdown", "(Ljava/lang/Runnable;)V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(65, l0);
            mv.visitMethodInsn(INVOKESTATIC, "dorkbox/util/Swt", "isEventThread", "()Z", false);
            Label l1 = new Label();
            mv.visitJumpInsn(IFEQ, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLineNumber(66, l2);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/widgets/Display", "getShells", "()[Lorg/eclipse/swt/widgets/Shell;", false);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(AALOAD);
            mv.visitIntInsn(BIPUSH, 21);
            mv.visitTypeInsn(NEW, "dorkbox/util/Swt$1");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "dorkbox/util/Swt$1", "<init>", "(Ljava/lang/Runnable;)V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               "org/eclipse/swt/widgets/Shell",
                               "addListener",
                               "(ILorg/eclipse/swt/widgets/Listener;)V",
                               false);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitLineNumber(73, l3);
            Label l4 = new Label();
            mv.visitJumpInsn(GOTO, l4);
            mv.visitLabel(l1);
            mv.visitLineNumber(74, l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitTypeInsn(NEW, "dorkbox/util/Swt$2");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "dorkbox/util/Swt$2", "<init>", "(Ljava/lang/Runnable;)V", false);
            mv.visitMethodInsn(INVOKESTATIC, "dorkbox/util/Swt", "dispatch", "(Ljava/lang/Runnable;)V", false);
            mv.visitLabel(l4);
            mv.visitLineNumber(89, l4);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLocalVariable("runnable", "Ljava/lang/Runnable;", null, l0, l5, 0);
            mv.visitMaxs(5, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$0", "()Lorg/eclipse/swt/widgets/Display;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitLineNumber(30, l0);
            mv.visitFieldInsn(GETSTATIC, "dorkbox/util/Swt", "currentDisplay", "Lorg/eclipse/swt/widgets/Display;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
