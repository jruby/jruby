/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 */
public class DataIterator<T> implements Iterator<T> {
    private Iterator<Edge<T>> internalIterator;
    private Object type;
    private Edge nextEdge = null;
    private boolean source;
    private boolean negate;

    public DataIterator(Set<Edge<T>> edges, Object type, boolean source, boolean negate) {
        this.internalIterator = edges.iterator();
        this.type = type;
        this.source = source;
        this.negate = negate;
    }

    @Override
    public boolean hasNext() {
        // Multiple hasNext calls with no next...hasNext still true
        if (nextEdge != null) return true;

        while (internalIterator.hasNext()) {
            Edge edge = internalIterator.next();

            if (negate) {
                if (edge.getType() != type) {
                    nextEdge = edge;
                    return true;
                }
            } else  if (edge.getType() == type) {
                nextEdge = edge;
                return true;
            }
        }

        return false;
    }

    @Override
    public T next() {
        if (hasNext()) {
            Edge<T> tmp = nextEdge;
            nextEdge = null;
            return source ? tmp.getSource().getData() : tmp.getDestination().getData();
        }

        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }
}

