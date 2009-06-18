package org.jruby.compiler.ir;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickString extends Operand
{
    final public List<Operand> _pieces;

    public BacktickString(Operand val) { _pieces = new ArrayList<Operand>(); _pieces.add(val); }
    public BacktickString(List<Operand> pieces) { _pieces = pieces; }
}
