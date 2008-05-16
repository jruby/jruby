/*
 * SkinnyMethodAdapter.java
 *
 * Created on March 10, 2007, 2:52 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.io.PrintStream;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author headius
 */
public class SkinnyMethodAdapter implements MethodVisitor, Opcodes {
    private MethodVisitor method;
    
    /** Creates a new instance of SkinnyMethodAdapter */
    public SkinnyMethodAdapter(MethodVisitor method) {
        this.method = method;
    }
    
    public SkinnyMethodAdapter() {
    }
    
    public MethodVisitor getMethodVisitor() {
        return method;
    }
    
    public void setMethodVisitor(MethodVisitor mv) {
        this.method = mv;
    }
    
    public void aload(int arg0) {
        getMethodVisitor().visitVarInsn(ALOAD, arg0);
    }
    
    public void iload(int arg0) {
        getMethodVisitor().visitVarInsn(ILOAD, arg0);
    }
    
    public void astore(int arg0) {
        getMethodVisitor().visitVarInsn(ASTORE, arg0);
    }
    
    public void ldc(Object arg0) {
        getMethodVisitor().visitLdcInsn(arg0);
    }
    
    public void bipush(int arg) {
        getMethodVisitor().visitIntInsn(BIPUSH, arg);
    }
    
    public void sipush(int arg) {
        getMethodVisitor().visitIntInsn(SIPUSH, arg);
    }
        
    public void pushInt(int value) {
        if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            switch (value) {
            case -1:
                iconst_m1();
                break;
            case 0:
                iconst_0();
                break;
            case 1:
                iconst_1();
                break;
            case 2:
                iconst_2();
                break;
            case 3:
                iconst_3();
                break;
            case 4:
                iconst_4();
                break;
            case 5:
                iconst_5();
                break;
            default:
                bipush(value);
                break;
            }
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            sipush(value);
        } else {
            ldc(value);
        }
    }
        
    public void pushBoolean(boolean bool) {
        if (bool) iconst_1(); else iconst_0();
    }
    
    public void invokestatic(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitMethodInsn(INVOKESTATIC, arg1, arg2, arg3);
    }
    
    public void invokespecial(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitMethodInsn(INVOKESPECIAL, arg1, arg2, arg3);
    }
    
    public void invokevirtual(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitMethodInsn(INVOKEVIRTUAL, arg1, arg2, arg3);
    }
    
    public void invokeinterface(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitMethodInsn(INVOKEINTERFACE, arg1, arg2, arg3);
    }
    
    public void aprintln() {
        dup();
        getstatic(p(System.class), "out", ci(PrintStream.class));
        swap();
        invokevirtual(p(PrintStream.class), "println", sig(void.class, params(Object.class)));
    }
    
    public void areturn() {
        getMethodVisitor().visitInsn(ARETURN);
    }
    
    public void newobj(String arg0) {
        getMethodVisitor().visitTypeInsn(NEW, arg0);
    }
    
    public void dup() {
        getMethodVisitor().visitInsn(DUP);
    }
    
    public void swap() {
        getMethodVisitor().visitInsn(SWAP);
    }
    
    public void swap2() {
        dup2_x2();
        pop2();
    }
    
    public void getstatic(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitFieldInsn(GETSTATIC, arg1, arg2, arg3);
    }
    
    public void putstatic(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitFieldInsn(PUTSTATIC, arg1, arg2, arg3);
    }
    
    public void getfield(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitFieldInsn(GETFIELD, arg1, arg2, arg3);
    }
    
    public void putfield(String arg1, String arg2, String arg3) {
        getMethodVisitor().visitFieldInsn(PUTFIELD, arg1, arg2, arg3);
    }
    
    public void voidreturn() {
        getMethodVisitor().visitInsn(RETURN);
    }
    
    public void anewarray(String arg0) {
        getMethodVisitor().visitTypeInsn(ANEWARRAY, arg0);
    }
    
    public void newarray(int arg0) {
        getMethodVisitor().visitIntInsn(NEWARRAY, arg0);
    }
    
    public void iconst_m1() {
        getMethodVisitor().visitInsn(ICONST_M1);
    }
    
    public void iconst_0() {
        getMethodVisitor().visitInsn(ICONST_0);
    }
    
    public void iconst_1() {
        getMethodVisitor().visitInsn(ICONST_1);
    }
    
    public void iconst_2() {
        getMethodVisitor().visitInsn(ICONST_2);
    }
    
    public void iconst_3() {
        getMethodVisitor().visitInsn(ICONST_3);
    }
    
    public void iconst_4() {
        getMethodVisitor().visitInsn(ICONST_4);
    }
    
    public void iconst_5() {
        getMethodVisitor().visitInsn(ICONST_5);
    }
    
    public void lconst_0() {
        getMethodVisitor().visitInsn(LCONST_0);
    }
    
    public void isub() {
        getMethodVisitor().visitInsn(ISUB);
    }
    
    public void aconst_null() {
        getMethodVisitor().visitInsn(ACONST_NULL);
    }
    
    public void label(Label label) {
        getMethodVisitor().visitLabel(label);
    }
    
    public void nop() {
        getMethodVisitor().visitInsn(NOP);
    }
    
    public void pop() {
        getMethodVisitor().visitInsn(POP);
    }
    
    public void pop2() {
        getMethodVisitor().visitInsn(POP2);
    }
    
    public void arrayload() {
        getMethodVisitor().visitInsn(AALOAD);
    }
    
    public void arraystore() {
        getMethodVisitor().visitInsn(AASTORE);
    }
    
    public void iarrayload() {
        getMethodVisitor().visitInsn(IALOAD);
    }
    
    public void barrayload() {
        getMethodVisitor().visitInsn(BALOAD);
    }
    
    public void barraystore() {
        getMethodVisitor().visitInsn(BASTORE);
    }
    
    public void dup_x2() {
        getMethodVisitor().visitInsn(DUP_X2);
    }
    
    public void dup_x1() {
        getMethodVisitor().visitInsn(DUP_X1);
    }
    
    public void dup2_x2() {
        getMethodVisitor().visitInsn(DUP2_X2);
    }
    
    public void dup2_x1() {
        getMethodVisitor().visitInsn(DUP2_X1);
    }
    
    public void dup2() {
        getMethodVisitor().visitInsn(DUP2);
    }
    
    public void trycatch(Label arg0, Label arg1, Label arg2,
                                   String arg3) {
        getMethodVisitor().visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }
    
    public void go_to(Label arg0) {
        getMethodVisitor().visitJumpInsn(GOTO, arg0);
    }
    
    public void lookupswitch(Label arg0, int[] arg1, Label[] arg2) {
        getMethodVisitor().visitLookupSwitchInsn(arg0, arg1, arg2);
    }
    
    public void athrow() {
        getMethodVisitor().visitInsn(ATHROW);
    }
    
    public void instance_of(String arg0) {
        getMethodVisitor().visitTypeInsn(INSTANCEOF, arg0);
    }
    
    public void ifeq(Label arg0) {
        getMethodVisitor().visitJumpInsn(IFEQ, arg0);
    }
    
    public void ifne(Label arg0) {
        getMethodVisitor().visitJumpInsn(IFNE, arg0);
    }
    
    public void if_acmpne(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ACMPNE, arg0);
    }
    
    public void if_acmpeq(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ACMPEQ, arg0);
    }
    
    public void if_icmple(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ICMPLE, arg0);
    }
    
    public void if_icmpgt(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ICMPGT, arg0);
    }
    
    public void if_icmplt(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ICMPLT, arg0);
    }
    
    public void if_icmpne(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ICMPNE, arg0);
    }
    
    public void if_icmpeq(Label arg0) {
        getMethodVisitor().visitJumpInsn(IF_ICMPEQ, arg0);
    }
    
    public void checkcast(String arg0) {
        getMethodVisitor().visitTypeInsn(CHECKCAST, arg0);
    }
    
    public void start() {
        getMethodVisitor().visitCode();
    }
    
    public void end() {
        getMethodVisitor().visitMaxs(1, 1);
        getMethodVisitor().visitEnd();
    }
    
    public void ifnonnull(Label arg0) {
        getMethodVisitor().visitJumpInsn(IFNONNULL, arg0);
    }
    
    public void ifnull(Label arg0) {
        getMethodVisitor().visitJumpInsn(IFNULL, arg0);
    }
    
    public void ifle(Label arg0) {
        getMethodVisitor().visitJumpInsn(IFLE, arg0);
    }
    
    public void arraylength() {
        getMethodVisitor().visitInsn(ARRAYLENGTH);
    }
    
    public void iadd() {
        getMethodVisitor().visitInsn(IADD);
    }
    
    public void iinc(int arg0, int arg1) {
        getMethodVisitor().visitIincInsn(arg0, arg1);
    }
    
    public AnnotationVisitor visitAnnotationDefault() {
        return getMethodVisitor().visitAnnotationDefault();
    }

    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        return getMethodVisitor().visitAnnotation(arg0, arg1);
    }

    public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
                                                      boolean arg2) {
        return getMethodVisitor().visitParameterAnnotation(arg0, arg1, arg2);
    }

    public void visitAttribute(Attribute arg0) {
        getMethodVisitor().visitAttribute(arg0);
    }

    public void visitCode() {
        getMethodVisitor().visitCode();
    }

    public void visitInsn(int arg0) {
        getMethodVisitor().visitInsn(arg0);
    }

    public void visitIntInsn(int arg0, int arg1) {
        getMethodVisitor().visitIntInsn(arg0, arg1);
    }

    public void visitVarInsn(int arg0, int arg1) {
        getMethodVisitor().visitVarInsn(arg0, arg1);
    }

    public void visitTypeInsn(int arg0, String arg1) {
        getMethodVisitor().visitTypeInsn(arg0, arg1);
    }

    public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
        getMethodVisitor().visitFieldInsn(arg0, arg1, arg2, arg3);
    }

    public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
        getMethodVisitor().visitMethodInsn(arg0, arg1, arg2, arg3);
    }

    public void visitJumpInsn(int arg0, Label arg1) {
        getMethodVisitor().visitJumpInsn(arg0, arg1);
    }

    public void visitLabel(Label arg0) {
        getMethodVisitor().visitLabel(arg0);
    }

    public void visitLdcInsn(Object arg0) {
        getMethodVisitor().visitLdcInsn(arg0);
    }

    public void visitIincInsn(int arg0, int arg1) {
        getMethodVisitor().visitIincInsn(arg0, arg1);
    }

    public void visitTableSwitchInsn(int arg0, int arg1, Label arg2,
                                     Label[] arg3) {
        getMethodVisitor().visitTableSwitchInsn(arg0, arg1, arg2, arg3);
    }

    public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
        getMethodVisitor().visitLookupSwitchInsn(arg0, arg1, arg2);
    }

    public void visitMultiANewArrayInsn(String arg0, int arg1) {
        getMethodVisitor().visitMultiANewArrayInsn(arg0, arg1);
    }

    public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2,
                                   String arg3) {
        getMethodVisitor().visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }

    public void visitLocalVariable(String arg0, String arg1, String arg2,
                                   Label arg3, Label arg4, int arg5) {
        getMethodVisitor().visitLocalVariable(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public void visitLineNumber(int arg0, Label arg1) {
        getMethodVisitor().visitLineNumber(arg0, arg1);
    }

    public void visitMaxs(int arg0, int arg1) {
        getMethodVisitor().visitMaxs(arg0, arg1);
    }

    public void visitEnd() {
        getMethodVisitor().visitEnd();
    }
    
    public void tableswitch(int min, int max, Label defaultLabel, Label[] cases) {
        getMethodVisitor().visitTableSwitchInsn(min, max, defaultLabel, cases);
    }

    public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
        getMethodVisitor().visitFrame(arg0, arg1, arg2, arg3, arg4);
    }

}
