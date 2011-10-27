package org.jruby.compiler.ir.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class Vertex<T> {
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
    
    public Iterator<Edge<T>> getIncomingEdgesOfType(Object type) {
        return new EdgeTypeIterator<T>(getIncomingEdges(), type);
    }
    
    public Iterator<Edge<T>> getOutgoingEdgesOfType(Object type) {
        return new EdgeTypeIterator<T>(getOutgoingEdges(), type);
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
        StringBuilder buf = new StringBuilder("Vertex ");
        
        buf.append(data).append(":\nincoming edges\n");
        for (Edge edge: getIncomingEdges()) {
            buf.append(edge).append("\n");
        }
        
        buf.append("outgoing edges\n");
        for (Edge edge: getOutgoingEdges()) {
            buf.append(edge).append("\n");
        }
        
        return buf.toString();
    }
}
