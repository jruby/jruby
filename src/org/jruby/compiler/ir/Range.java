package org.jruby.compiler.ir;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the Range object
public class Range extends Operand
{
    final public Operand _begin;
    final public Operand _end;

    public Range(Operand b, Operand e) { _begin = b; _end = e; }

    public String toString() { return "(" + _begin + ".." + _end + "):Range"; }

// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() 
    {
        return _begin.isConstant() && _end.isConstant();
    }

    public boolean isCompoundValue() { return true; }

/**
 * SSS FIXME: Do not instantiate eagerly!  You will be in trouble!
    public Operand toArray()
    {
        if (isConstant() && (_begin instanceof Fixnum) && (_end instanceof Fixnum))
        }
        else {
            // SSS FIXME: Is this the accepted semantics?  If used for compile-time optimizations only, this should perhaps return null?
            return this;
        }
    }
**/
}
