/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.ir.operands;

/**
 *
 * @author enebo
 */
public class LocalVariable extends Variable {
    final public String name;

    public LocalVariable(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LocalVariable)) return false;

        return name.equals(((LocalVariable) obj).name);
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof LocalVariable)) return 0;

        return name.compareTo(((LocalVariable) arg0).name);
    }
}
