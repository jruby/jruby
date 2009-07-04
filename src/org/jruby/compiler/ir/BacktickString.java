package org.jruby.compiler.ir;

import java.util.List;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickString extends Operand
{
    final public Operand[] _pieces;

    public BacktickString(Operand val) { _pieces = new Operand[] { val };  }
    public BacktickString(List<Operand> pieces) { _pieces = (Operand[])pieces.toArray(); }

    public boolean isConstant() {
       for (Operand o: _pieces)
          if (!o.isConstant())
             return false;

       return true;
    }
}
