package org.jruby.javasupport.test;

import java.io.Serializable;

public class HashBase implements Cloneable, Serializable {

    public static int hash(HashBase obj) {
        return obj.hash(10);
    }

    public int hash(final int offset) {
        return offset + this.hashCode();
    }
}
