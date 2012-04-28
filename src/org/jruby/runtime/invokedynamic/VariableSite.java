package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class VariableSite extends MutableCallSite {
    public final String name;
    private int chainCount;
    public VariableSite(MethodType type, String name) {
        super(type);
        this.name = name;
        this.chainCount = 0;
    }

    public synchronized int chainCount() {
        return chainCount;
    }

    public synchronized void incrementChainCount() {
        chainCount += 1;
    }

    public synchronized void clearChainCount() {
        chainCount = 0;
    }
}
