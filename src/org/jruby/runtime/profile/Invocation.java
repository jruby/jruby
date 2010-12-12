
package org.jruby.runtime.profile;

import java.util.HashMap;

public class Invocation {
    public int methodSerialNumber;
    public int recursiveDepth;
    public Invocation parent = null;
    public long duration     = 0;
    public int count         = 0;
    public HashMap<Integer, Invocation> children = new HashMap<Integer, Invocation>();
    
    public Invocation(int serial) {
        this.methodSerialNumber = serial;
        this.recursiveDepth = 1;
    }
    
    public Invocation(Invocation parent, int serial, int recursiveDepth) {
        this.parent             = parent;
        this.methodSerialNumber = serial;
        this.recursiveDepth     = recursiveDepth;
    }
    
    public Invocation childInvocationFor(int serial, int recursiveDepth) {
        Invocation child;
        if ((child = children.get(serial)) == null) {
            child = new Invocation(this, serial, recursiveDepth);
            children.put(serial, child);
        }
        return child;
    }
    
    public long childTime() {
        long t = 0;
        for (Invocation inv : children.values()) {
            t += inv.duration;
        }
        return t;
    }
    
    public long selfTime() {
        return duration - childTime();
    }
}