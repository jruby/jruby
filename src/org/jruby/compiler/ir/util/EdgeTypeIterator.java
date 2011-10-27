/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author enebo
 */
public class EdgeTypeIterator<T extends DataInfo> implements Iterator<Edge<T>> {
    private Iterator<Edge<T>> internalIterator;
    private Object type;
    private Edge nextEdge = null;
    
    public EdgeTypeIterator(Set<Edge<T>> edges, Object type) {
        this.internalIterator = edges.iterator();
        this.type = type;
    }

    public boolean hasNext() {
        // Multiple hasNext calls with no next...hasNext still true
        if (nextEdge != null) return true;
        
        while (internalIterator.hasNext()) {
            Edge edge = internalIterator.next();
                
            if (edge.getType() == type) {
                nextEdge = edge;
                return true;
            }
        }

        return false;
    }

    public Edge<T> next() {
        if (hasNext()) {
            Edge<T> tmp = nextEdge;
            nextEdge = null;
            return tmp;
        }

        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }
}
