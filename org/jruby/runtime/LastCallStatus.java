package org.jruby.runtime;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class LastCallStatus {
    private static final Object NORMAL = new Object();
    private static final Object PRIVATE = new Object();
    private static final Object PROTECTED = new Object();
    private static final Object VARIABLE = new Object();
    
    private Object status = NORMAL;

    public LastCallStatus() {
    }

    public boolean isPrivate() {
        return status == PRIVATE;
    }

    public boolean isProtected() {
        return status == PROTECTED;
    }

    public boolean isVariable() {
        return status == VARIABLE;
    }

    public void setNormal() {
        status = NORMAL;
    }

    public void setPrivate() {
        status = PRIVATE;
    }

    public void setProtected() {
        status = PROTECTED;
    }

    public void setVariable() {
        status = VARIABLE;
    }
}
