package org.jruby.ir.targets;

import java.lang.invoke.MethodType;

import org.jruby.util.collections.IntHashMap;

/**
 * Context for JITing methods.  Temporary data.
 */
public class JVMVisitorMethodContext {
    // Method name in the jitted version of this method
    private String jittedName;

    // Signatures to the jitted versions of this method
    private IntHashMap<MethodType> signatures;
    private MethodType varSignature; // for arity == -1

    public void setJittedName(String jittedName) {
        this.jittedName = jittedName;
    }

    public String getJittedName() {
        return jittedName;
    }

    public void addNativeSignature(int arity, MethodType signature) {
        if ( arity == -1 ) varSignature = signature;
        else {
            if ( signatures == null ) signatures = new IntHashMap<>(2);
            signatures.put(arity, signature);
        }
    }

    public MethodType getNativeSignature(int arity) {
        if ( arity == -1 ) return varSignature;
        return signatures == null ? null : signatures.get(arity);
    }

    public int getNativeSignaturesCount() {
        int count = varSignature == null ? 0 : 1;
        if ( signatures != null ) count += signatures.size();
        return count;
    }

    public IntHashMap<MethodType> getNativeSignaturesExceptVariable() {
        return signatures == null ? IntHashMap.<MethodType>nullMap() : signatures;
    }

}
