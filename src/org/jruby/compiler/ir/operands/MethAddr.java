package org.jruby.compiler.ir.operands;

// Placeholder class for method address

import org.jruby.interpreter.InterpreterContext;

public class MethAddr extends Reference {
    // The following constants capture pre-known static call sites -- used to implement ruby internals
    public final static MethAddr GVAR_ALIAS        = new MethAddr("aliasGlobalVariable");
    public final static MethAddr DEFINE_ALIAS      = new MethAddr("defineAlias");
    public final static MethAddr RETRIEVE_CONSTANT = new MethAddr("retrieveConstant");
    public final static MethAddr FOR_EACH          = new MethAddr("each");
    public final static MethAddr SUPER             = new MethAddr("super");
    public final static MethAddr ZSUPER            = new MethAddr("super");
    public final static MethAddr GET_FILE_NAME     = new MethAddr("getFileName");

    public MethAddr(String name) {
        super(name);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MethAddr) && ((MethAddr)o).getName().equals(getName());
    }
}
