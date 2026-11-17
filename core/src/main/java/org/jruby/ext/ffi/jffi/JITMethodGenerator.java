package org.jruby.ext.ffi.jffi;

public interface JITMethodGenerator {
    boolean isSupported(JITSignature signature);
    void generate(AsmClassBuilder builder, String functionName, JITSignature signature);
}
