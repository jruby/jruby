package org.jruby.ir.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 */
public class EdgeTypeIterator<T> implements Iterator<Edge<T>> {
    private Iterator<Edge<T>> internalIterator;
    private Object type;
    private Edge nextEdge = null;
    private boolean negate;

    public EdgeTypeIterator(Set<Edge<T>> edges, Object type, boolean negate) {
        this.internalIterator = edges.iterator();
        this.type = type;
        this.negate = negate;
    }

    @Override
    public boolean hasNext() {
        // Multiple hasNext calls with no next...hasNext still true
        if (nextEdge != null) return true;

        while (internalIterator.hasNext()) {
            Edge edge = internalIterator.next();
            Object edgeType = edge.getType();

            if (negate) {
                // When edgeType or type is null compare them directly. Otherwise compare them using equals
                if ((edgeType != null && !edgeType.equals(type)) || (edgeType == null && edgeType != type)) {
                    nextEdge = edge;
                    return true;
                }
                // When edgeType or type is null compare them directly. Otherwise compare them using equals
            } else if ((edgeType != null && edgeType.equals(type)) || (edgeType == null && edgeType == type)) {
                nextEdge = edge;
                return true;
            }
        }
        return false;
    }

    @Override
    public Edge<T> next() {
        if (hasNext()) {
            Edge<T> tmp = nextEdge;
            nextEdge = null;
            return tmp;
        }

        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }
}
