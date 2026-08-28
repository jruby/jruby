package org.jruby.test;

public abstract class Abstract {

    public String result = null;

    public Abstract() { super(); }

    public Abstract(boolean callProtected) {
        // NOTE: the leaking this anti-pattern :
        if ( callProtected ) result = call_protected();
    }

    public Abstract(final String result) {
        this(check(result));
        //if ( this.result == null ) this.result = result;
    }

    private static boolean check(final String name) {
        return name.replace('_', ' ').contains("call protected method");
    }

    public String call_protected() {
        return protected_method();
    }

    protected abstract String protected_method() ;
}
