package org.jruby.runtime;

import org.jruby.util.Asserts;

import java.io.Serializable;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class Visibility implements Serializable {
    public static final Visibility PUBLIC = new Visibility((byte)0);
    public static final Visibility PROTECTED = new Visibility((byte)1);
    public static final Visibility PRIVATE = new Visibility((byte)2);
    public static final Visibility MODULE_FUNCTION = new Visibility((byte)3);

    private final byte restore;

    static final long serialVersionUID = 2002102900L;

    /**
     * Constructor for MethodScope.
     */
    private Visibility(byte restore) {
        this.restore = restore;
    }
    
    private Object readResolve() {
        switch (restore) {
            case 0:
                return PUBLIC;
            case 1:
                return PROTECTED;
            case 2:
                return PRIVATE;
            case 3:
                return MODULE_FUNCTION;
            default:
                Asserts.notReached();
                return null;
        }
    } 

    public boolean isPublic() {
        return this == PUBLIC;
    }

    public boolean isProtected() {
        return this == PROTECTED;
    }

    public boolean isPrivate() {
        return this == PRIVATE;
    }
    
    public boolean isModuleFunction() {
        return this == MODULE_FUNCTION;
    }

    public String toString() {
        switch (restore) {
            case 0:
                return "public";
            case 1:
                return "protected";
            case 2:
                return "private";
            case 3:
                return "module_function";
            default:
                Asserts.notReached();
                return null;
        }
    }
}