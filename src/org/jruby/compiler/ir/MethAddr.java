package org.jruby.compiler.ir;

// Placeholder class for method address
public class MethAddr extends Reference 
{
    // The following constants capture pre-known static call sites -- used to implement ruby internals
    public final MethAddr DEFINE_ALIAS      = new MethAddr("defineAlias");
    public final MethAddr RETRIEVE_CONSTANT = new MethAddr("retrieveConstant");
    public final MethAddr FOR_EACH          = new MethAddr("each");

    public MethAddr(String name) { super(name); }
}
