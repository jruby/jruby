
package org.jruby.runtime.profile;

import java.util.HashMap;

public class Invocation {
    public int methodSerialNumber;
    public Invocation parent = null;
    public long duration     = 0;
    public int count         = 0;
    public HashMap<Integer, Invocation> children = new HashMap<Integer, Invocation>();
    
    public Invocation(int serial) {
        this.methodSerialNumber = serial;
    }
    
    public Invocation(Invocation parent, int serial) {
        this.parent             = parent;
        this.methodSerialNumber = serial;
    }
    
    public Invocation childInvocationFor(int serial) {
        Invocation child;
        if ((child = children.get(serial)) == null) {
            child = new Invocation(this, serial);
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