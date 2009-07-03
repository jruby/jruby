package org.jruby.compiler.ir;

// Placeholder class for method address
public class MethAddr extends Reference 
{
    // The following constants capture pre-known static call sites -- used to implement ruby internals
    public final MethAddr DEFINE_ALIAS      = new MethAddr("defineAlias");
    public final MethAddr RETRIEVE_CONSTANT = new MethAddr("retrieveConstant");
    public final MethAddr FOR_EACH          = new MethAddr("each");

    // The following constants capture pre-known static call sites -- JRuby implementations of methods of ruby classes
    // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2 ... Is this a bug there?
    public final MethAddr MATCH             = new MethAddr("op_match2");
    public final MethAddr MATCH2            = new MethAddr("op_match");
    public final MethAddr MATCH3            = new MethAddr("match3");
    public final MethAddr TO_ARY            = new MethAddr("aryToAry");
    public final MethAddr GET_FILE_NAME     = new MethAddr("getFileName");

    public MethAddr(String name) { super(name); }
}
