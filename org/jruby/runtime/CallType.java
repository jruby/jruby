package org.jruby.runtime;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class CallType {
    public static final CallType NORMAL = new CallType();
    public static final CallType FUNCTIONAL = new CallType();
    public static final CallType SUPER = new CallType();
    public static final CallType VARIABLE = new CallType();

    private CallType() {
    }
    
    public boolean isSuper() {
        return this == SUPER;
    }

    public boolean isVariable() {
        return this == VARIABLE;
    }

    public boolean isFunctional() {
        return this == FUNCTIONAL;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }
}