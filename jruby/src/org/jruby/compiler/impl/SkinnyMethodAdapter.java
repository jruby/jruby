/*
 * SkinnyMethodAdapter.java
 *
 * Created on March 10, 2007, 2:52 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

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
    private MethodVisitor mv;
    
    /** Creates a new instance of SkinnyMethodAdapter */
    public SkinnyMethodAdapter(MethodVisitor mv) {
        this.mv = mv;
    }
    
    public void aload(int arg0) {
        mv.visitVarInsn(ALOAD, arg0);
    }
    
    public void astore(int arg0) {
        mv.visitVarInsn(ASTORE, arg0);
    }
    
    public void ldc(Object arg0) {
        mv.visitLdcInsn(arg0);
    }
    
    public void invokestatic(String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(INVOKESTATIC, arg1, arg2, arg3);
    }
    
    public void invokespecial(String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(INVOKESPECIAL, arg1, arg2, arg3);
    }
    
    public void invokevirtual(String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(INVOKEVIRTUAL, arg1, arg2, arg3);
    }
    
    public void invokeinterface(String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(INVOKEINTERFACE, arg1, arg2, arg3);
    }
    
    public void areturn() {
        mv.visitInsn(ARETURN);
    }
    
    public void newobj(String arg0) {
        mv.visitTypeInsn(NEW, arg0);
    }
    
    public void dup() {
        mv.visitInsn(DUP);
    }
    
    public void swap() {
        mv.visitInsn(SWAP);
    }
    
    public void swap2() {
        dup2_x2();
        pop2();
    }
    
    public void getstatic(String arg1, String arg2, String arg3) {
        mv.visitFieldInsn(GETSTATIC, arg1, arg2, arg3);
    }
    
    public void putstatic(String arg1, String arg2, String arg3) {
        mv.visitFieldInsn(PUTSTATIC, arg1, arg2, arg3);
    }
    
    public void voidreturn() {
        mv.visitInsn(RETURN);
    }
    
    public void anewarray(String arg0) {
        mv.visitTypeInsn(ANEWARRAY, arg0);
    }
    
    public void iconst_0() {
        mv.visitInsn(ICONST_0);
    }
    
    public void iconst_1() {
        mv.visitInsn(ICONST_1);
    }
    
    public void iconst_2() {
        mv.visitInsn(ICONST_2);
    }
    
    public void isub() {
        mv.visitInsn(ISUB);
    }
    
    public void aconst_null() {
        mv.visitInsn(ACONST_NULL);
    }
    
    public void label(Label label) {
        mv.visitLabel(label);
    }
    
    public void pop() {
        mv.visitInsn(POP);
    }
    
    public void pop2() {
        mv.visitInsn(POP2);
    }
    
    public void arrayload() {
        mv.visitInsn(AALOAD);
    }
    
    public void arraystore() {
        mv.visitInsn(AASTORE);
    }
    
    public void dup_x2() {
        mv.visitInsn(DUP_X2);
    }
    
    public void dup_x1() {
        mv.visitInsn(DUP_X1);
    }
    
    public void dup2_x2() {
        mv.visitInsn(DUP2_X2);
    }
    
    public void dup2_x1() {
        mv.visitInsn(DUP2_X1);
    }
    
    public void dup2() {
        mv.visitInsn(DUP2);
    }
    
    public void trycatch(Label arg0, Label arg1, Label arg2,
                                   String arg3) {
        mv.visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }
    
    public void go_to(Label arg0) {
        mv.visitJumpInsn(GOTO, arg0);
    }
    
    public void lookupswitch(Label arg0, int[] arg1, Label[] arg2) {
        mv.visitLookupSwitchInsn(arg0, arg1, arg2);
    }
    
    public void athrow() {
        mv.visitInsn(ATHROW);
    }
    
    public void ifeq(Label arg0) {
        mv.visitJumpInsn(IFEQ, arg0);
    }
    
    public void ifne(Label arg0) {
        mv.visitJumpInsn(IFNE, arg0);
    }
    
    public void if_acmpne(Label arg0) {
        mv.visitJumpInsn(IF_ACMPNE, arg0);
    }
    
    public void checkcast(String arg0) {
        mv.visitTypeInsn(CHECKCAST, arg0);
    }
    
    public void start() {
        mv.visitCode();
    }
    
    public void end() {
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    public void ifnonnull(Label arg0) {
        mv.visitJumpInsn(IFNONNULL, arg0);
    }
    
    public void ifnull(Label arg0) {
        mv.visitJumpInsn(IFNULL, arg0);
    }
    
    public void ifle(Label arg0) {
        mv.visitJumpInsn(IFLE, arg0);
    }
    
    public void arraylength() {
        mv.visitInsn(ARRAYLENGTH);
    }
    
    public void iadd() {
        mv.visitInsn(IADD);
    }
    
    public AnnotationVisitor visitAnnotationDefault() {
        return mv.visitAnnotationDefault();
    }

    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        return mv.visitAnnotation(arg0, arg1);
    }

    public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
                                                      boolean arg2) {
        return mv.visitParameterAnnotation(arg0, arg1, arg2);
    }

    public void visitAttribute(Attribute arg0) {
        mv.visitAttribute(arg0);
    }

    public void visitCode() {
        mv.visitCode();
    }

    public void visitInsn(int arg0) {
        mv.visitInsn(arg0);
    }

    public void visitIntInsn(int arg0, int arg1) {
        mv.visitIntInsn(arg0, arg1);
    }

    public void visitVarInsn(int arg0, int arg1) {
        mv.visitVarInsn(arg0, arg1);
    }

    public void visitTypeInsn(int arg0, String arg1) {
        mv.visitTypeInsn(arg0, arg1);
    }

    public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
        mv.visitFieldInsn(arg0, arg1, arg2, arg3);
    }

    public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(arg0, arg1, arg2, arg3);
    }

    public void visitJumpInsn(int arg0, Label arg1) {
        mv.visitJumpInsn(arg0, arg1);
    }

    public void visitLabel(Label arg0) {
        mv.visitLabel(arg0);
    }

    public void visitLdcInsn(Object arg0) {
        mv.visitLdcInsn(arg0);
    }

    public void visitIincInsn(int arg0, int arg1) {
        mv.visitIincInsn(arg0, arg1);
    }

    public void visitTableSwitchInsn(int arg0, int arg1, Label arg2,
                                     Label[] arg3) {
        mv.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
    }

    public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
        mv.visitLookupSwitchInsn(arg0, arg1, arg2);
    }

    public void visitMultiANewArrayInsn(String arg0, int arg1) {
        mv.visitMultiANewArrayInsn(arg0, arg1);
    }

    public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2,
                                   String arg3) {
        mv.visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }

    public void visitLocalVariable(String arg0, String arg1, String arg2,
                                   Label arg3, Label arg4, int arg5) {
        mv.visitLocalVariable(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public void visitLineNumber(int arg0, Label arg1) {
        mv.visitLineNumber(arg0, arg1);
    }

    public void visitMaxs(int arg0, int arg1) {
        mv.visitMaxs(arg0, arg1);
    }

    public void visitEnd() {
        mv.visitEnd();
    }
    
    public void tableswitch(int min, int max, Label defaultLabel, Label[] cases) {
        mv.visitTableSwitchInsn(min, max, defaultLabel, cases);
    }

}
