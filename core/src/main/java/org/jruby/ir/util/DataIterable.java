/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.util;

import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author enebo
 */
public class DataIterable<T> implements Iterable<T> {
    private Set<Edge<T>> edges;
    private Object type;
    private boolean negate;
    private boolean source;

    public DataIterable(Set<Edge<T>> edges, Object type, boolean source, boolean negate) {
        this.edges = edges;
        this.type = type;
        this.negate = negate;
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        return new DataIterator<T>(edges, type, source, negate);
    }
}
