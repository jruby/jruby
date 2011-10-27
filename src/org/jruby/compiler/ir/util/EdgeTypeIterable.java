package org.jruby.compiler.ir.util;

import java.util.Iterator;
import java.util.Set;

/**
 */
public class EdgeTypeIterable<T extends DataInfo> implements Iterable<Edge<T>> {
    private Set<Edge<T>> edges;
    private Object type;
    
    public EdgeTypeIterable(Set<Edge<T>> edges, Object type) {
        this.edges = edges;
        this.type = type;
    }

    public Iterator<Edge<T>> iterator() {
        return new EdgeTypeIterator<T>(edges, type);
    }
}
