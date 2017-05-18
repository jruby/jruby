package org.jruby.javasupport.binding;

/**
* Created by headius on 2/26/15.
*/
public final class AssignedName {
    final String name;
    final Priority type;

    AssignedName(String name, Priority type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode()) + '(' + name + ' ' + type + ')';
    }
}
