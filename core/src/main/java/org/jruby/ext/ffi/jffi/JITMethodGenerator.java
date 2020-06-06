package org.jruby.ext.ffi.jffi;

/**
 * 
 */
public interface JITMethodGenerator {

    public boolean isSupported(JITSignature signature);

    public void generate(AsmClassBuilder builder, String functionName, JITSignature signature);
}
