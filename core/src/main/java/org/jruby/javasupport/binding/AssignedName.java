package org.jruby.javasupport.binding;

/**
* Created by headius on 2/26/15.
*/
public class AssignedName {
    String name;
    Priority type;

    public AssignedName() {}
    public AssignedName(String name, Priority type) {
        this.name = name;
        this.type = type;
    }
}
