package org.jruby.ir.targets;

import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by enebo on 8/6/15.
 */
public class JVMVisitorContext {
    // Method name in the jitted version of this method
    private String jittedName;

    // Signatures to the jitted versions of this method
    private Map<Integer, MethodType> signatures;

    public void setJittedName(String jittedName) {
        this.jittedName = jittedName;
    }

    public String getJittedName() {
        return jittedName;
    }


    public void addNativeSignature(int arity, MethodType signature) {
        if (signatures == null) signatures = new HashMap<>(1);
        signatures.put(arity, signature);
    }

    public Map<Integer, MethodType> getNativeSignatures() {
        return Collections.unmodifiableMap(signatures);
    }
}
