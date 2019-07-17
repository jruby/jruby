package org.jruby.ast;

public interface IList {
    int size();
    Node get(int idx);
    Node[] children();
}
