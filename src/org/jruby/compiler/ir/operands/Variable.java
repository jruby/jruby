package org.jruby.compiler.ir.operands;

public class Variable extends Operand implements Comparable
{
    final public String _name;

    public Variable(String n) { _name = n; }

    public String toString() { return _name; }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Variable other = (Variable)obj;
        if ((this._name == null) ? (other._name != null) : !this._name.equals(other._name)) {
            return false;
        }
        return true;
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof Variable) {
            return _name.compareTo(((Variable)arg0)._name);
        }
        return 0;
    }
}
