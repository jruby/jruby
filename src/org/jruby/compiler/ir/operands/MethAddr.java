package org.jruby.compiler.ir.operands;

// Placeholder class for method address
public class MethAddr extends Reference 
{
    // The following constants capture pre-known static call sites -- used to implement ruby internals
    public final static MethAddr DEFINE_ALIAS      = new MethAddr("defineAlias");
    public final static MethAddr RETRIEVE_CONSTANT = new MethAddr("retrieveConstant");
    public final static MethAddr FOR_EACH          = new MethAddr("each");
    public final static MethAddr SUPER             = new MethAddr("super");

    // The following constants capture pre-known static call sites -- JRuby implementations of methods of ruby classes
    // SSS FIXME: Note that compiler/impl/BaseBodyCompiler is using op_match2 for match() and and op_match for match2 ... Is this a bug there?
    public final static MethAddr MATCH             = new MethAddr("op_match2");
    public final static MethAddr MATCH2            = new MethAddr("op_match");
    public final static MethAddr MATCH3            = new MethAddr("match3");
    // SSS FIXME: This method (at least in the context of multiple assignment) is a little weird.
    // It calls regular to_ary on the object.  But, if it encounters a method_missing, the value
    // is inserted into an 1-element array!
    // try "a,b,c = 1" first; then define Fixnum.to_ary method and try it again.
    // Ex: http://gist.github.com/163551
    public final static MethAddr TO_ARY            = new MethAddr("aryToAry");
    public final static MethAddr GET_FILE_NAME     = new MethAddr("getFileName");

    public MethAddr(String name) { super(name); }

    public String getName() { return _refName; }

    public String toString() { return _refName; }
}
