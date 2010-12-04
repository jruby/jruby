
package org.jruby.runtime.profile;

import java.util.ArrayList;

public class InvocationSet { 
    Invocation[] invocations;
    
    public InvocationSet(ArrayList<Invocation> invs) {
        invocations = new Invocation[invs.size()];
        for (int i = 0; i < invocations.length; i++)
            invocations[i] = invs.get(i);
    }
    
    public long totalTime() {
        long t = 0;
        for (Invocation inv : invocations) {
            t += inv.duration;
        }
        return t;
    }
    
    public long selfTime() {
        return totalTime() - childTime();
    }
    
    public long childTime() {
        long t = 0;
        for (Invocation inv : invocations) {
            t += inv.childTime();
        }
        return t;
    }
    
    public int totalCount() {
        int t = 0;
        for (Invocation inv : invocations) {
            t += inv.count;
        }
        return t;
    }
    
    public long timeSpentInChild(int serial) {
        long t = 0;
        for (Invocation inv : invocations) {
            Invocation childInv = inv.children.get(serial);
            if (childInv != null)
                t += childInv.duration;
        }
        return t;
    }

    public int callsOfChild(int serial) {
        int c = 0;
        for (Invocation inv : invocations) {
            Invocation childInv = inv.children.get(serial);
            if (childInv != null)
                c += childInv.count;
        }
        return c;
    }
}