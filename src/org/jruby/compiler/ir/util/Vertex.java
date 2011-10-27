package org.jruby.compiler.ir.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class Vertex<T extends DataInfo> {
    private DirectedGraph graph;
    private T data;
    private Set<Edge<T>> incoming = null;
    private Set<Edge<T>> outgoing = null;
    
    public Vertex(DirectedGraph graph, T data) {
        this.graph = graph;
        this.data = data;
    }

    public void addEdgeTo(Vertex destination, Object type) {
        Edge edge = new Edge<T>(this, destination, type);
        getOutgoingEdges().add(edge);
        destination.getIncomingEdges().add(edge);
        graph.edges().add(edge);
    }
    
    public void addEdgeTo(T destination, Object type) {
        Vertex destinationVertex = graph.vertexFor(destination);
        
        addEdgeTo(destinationVertex, type);
    }
    
    public boolean removeEdgeTo(Vertex destination) {
        for (Edge edge: getOutgoingEdges()) {
            if (edge.getDestination() == destination) {
                getOutgoingEdges().remove(edge);
                edge.getDestination().getIncomingEdges().remove(edge);
                graph.edges().remove(edge);
                return true;
            }
        }
        
        return false;
    }
    
    public void removeAllEdges() {
        for (Edge edge: getIncomingEdges()) {
            edge.getSource().getOutgoingEdges().remove(edge);
        }
        incoming = null;
        
        
        for (Edge edge: getOutgoingEdges()) {
            edge.getDestination().getIncomingEdges().remove(edge);
        }
        outgoing = null;
    }
    
    public Iterable<Edge<T>> getIncomingEdgesOfType(Object type) {
        return new EdgeTypeIterable<T>(getIncomingEdges(), type);
    }
    
    public Iterable<Edge<T>> getOutgoingEdgesOfType(Object type) {
        return new EdgeTypeIterable<T>(getOutgoingEdges(), type);
    }
    
    public Set<Edge<T>> getIncomingEdges() {
        if (incoming == null) incoming = new HashSet<Edge<T>>();
        
        return incoming;
    }
    
    public Set<Edge<T>> getOutgoingEdges() {
        if (outgoing == null) outgoing = new HashSet<Edge<T>>();
        
        return outgoing;
    }    
    
    public T getData() {
        return data;
    }
    
    @Override
    public String toString() {
        boolean found = false;
        StringBuilder buf = new StringBuilder(data.toString());

        buf.append(":");
        
        Set<Edge<T>> edges = getOutgoingEdges();
        int size = edges.size();
        
        if (size > 0) {
            found = true;
            buf.append(">[");
            Iterator<Edge<T>> iterator = edges.iterator();
            
            for (int i = 0; i < size - 1; i++) {
                buf.append(iterator.next().getDestination().getData().getID()).append(",");
            }
            buf.append(iterator.next().getDestination().getData().getID()).append("]");
        }

        edges = getIncomingEdges();
        size = edges.size();
        
        if (size > 0) {
            if (found) buf.append(", ");
            buf.append("<[");            
            Iterator<Edge<T>> iterator = edges.iterator();

            for (int i = 0; i < size - 1; i++) {
                buf.append(iterator.next().getSource().getData().getID()).append(",");
            }
            buf.append(iterator.next().getSource().getData().getID()).append("]");
        }
        buf.append("\n");
        
        return buf.toString();
    }
}
