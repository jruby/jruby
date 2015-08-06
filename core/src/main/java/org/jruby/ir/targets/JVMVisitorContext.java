package org.jruby.ir.targets;

/**
 * Created by enebo on 8/6/15.
 */
public class JVMVisitorContext {
    // Method name in the jitted version of this method
    private String jittedName;

    public void setJittedName(String jittedName) {
        this.jittedName = jittedName;
    }

    public String getJittedName() {
        return jittedName;
    }
}
