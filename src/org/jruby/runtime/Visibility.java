package org.jruby.runtime;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class Visibility {
    public static final Visibility PUBLIC = new Visibility();
    public static final Visibility PROTECTED = new Visibility();
    public static final Visibility PRIVATE = new Visibility();
    public static final Visibility MODULE_FUNCTION = new Visibility();

    /**
     * Constructor for MethodScope.
     */
    private Visibility() {
        super();
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
}