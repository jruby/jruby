package org.jruby.ir.targets;

import java.lang.invoke.MethodType;

import org.jruby.util.collections.IntHashMap;

/**
 * Context for JITing methods.  Temporary data.
 */
public class JVMVisitorMethodContext {
    // The base name of this method. It will match specific if non-null, otherwise varargs.
    private String baseName;

    // Method name in the specific-arity version of this method. May be null
    private String specificName;

    // Method name in the variable-arity version of this method
    private String variableName;

    // Signatures to the jitted versions of this method
    private IntHashMap<MethodType> signatures;
    private MethodType varSignature; // for arity == -1

    public void setSpecificName(String specificName) {
        this.specificName = specificName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getSpecificName() {
        return specificName;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getBaseName() {
        return baseName;
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
