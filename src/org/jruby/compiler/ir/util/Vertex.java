package org.jruby.compiler.ir.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class Vertex<T extends DataInfo> implements Comparable<Vertex<T>> {
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

    public Iterable<Edge<T>> getIncomingEdgesNotOfType(Object type) {
        return new EdgeTypeIterable<T>(getIncomingEdges(), type, true);
    }
    
    public Iterable<Edge<T>> getOutgoingEdgesOfType(Object type) {
        return new EdgeTypeIterable<T>(getOutgoingEdges(), type);
    }

    public T getIncomingSourceData() {
        Edge<T> edge = getSingleEdge(getIncomingEdges().iterator(), "");
        
        return edge == null ? null : edge.getSource().getData();  
    }    
    
    public T getIncomingSourceDataOfType(Object type) {
        Edge<T> edge = getSingleEdge(getIncomingEdgesOfType(type).iterator(), type);
        
        return edge == null ? null : edge.getSource().getData();
    }    
    
    public Iterable<T> getIncomingSourcesData() {
        return new DataIterable<T>(getIncomingEdges(), null, true);
    }
    
    public Iterable<T> getIncomingSourcesDataOfType(Object type) {
        return new DataIterable<T>(getIncomingEdges(), type, false);
    }      
    
    public Iterable<T> getIncomingSourcesDataNotOfType(Object type) {
        return new DataIterable<T>(getIncomingEdges(), type, true);
    }      
        
    public Iterable<Edge<T>> getOutgoingEdgesNotOfType(Object type) {
        return new EdgeTypeIterable<T>(getOutgoingEdges(), type, true);
    }
    
    public Iterable<T> getOutgoingDestinationsData() {
        return new DataIterable<T>(getOutgoingEdges(), null, true);
    }

    public Iterable<T> getOutgoingDestinationsDataOfType(Object type) {
        return new DataIterable<T>(getOutgoingEdges(), type, false);
    }
    
    public Iterable<T> getOutgoingDestinationsDataNotOfType(Object type) {
        return new DataIterable<T>(getOutgoingEdges(), type, true);
    }

    public T getOutgoingDestinationData() {
        Edge<T> edge = getSingleEdge(getOutgoingEdges().iterator(), "");
        
        return edge == null ? null : edge.getSource().getData();  
    }    
    
    public T getOutgoingDestinationDataOfType(Object type) {
        Edge<T> edge = getSingleEdge(getOutgoingEdgesOfType(type).iterator(), type);
        
        return edge == null ? null : edge.getDestination().getData();
    }
    
    private Edge<T> getSingleEdge(Iterator<Edge<T>> iterator, Object type) {
        if (iterator.hasNext()) {
            Edge<T> edge = iterator.next();
            
            assert !iterator.hasNext() : "Should only be one edge of type " + type;
            
            return edge;
        }
        
        return null;
    }
    
    public Edge<T> getIncomingEdgeOfType(Object type) {
        return getSingleEdge(getIncomingEdgesOfType(type).iterator(), type);
    }

    public Edge<T> getOutgoingEdgeOfType(Object type) {
        return getSingleEdge(getOutgoingEdgesOfType(type).iterator(), type);
    }
    
    /**
     * Get single incoming edge of any type and assert if there is more than
     * one.
     */
    public Edge<T> getIncomingEdge() {
        return getSingleEdge(getIncomingEdgesNotOfType(null).iterator(), null);
    }
    
    /**
     * Get single outgoing edge of any type and assert if there is more than
     * one.
     */
    public Edge<T> getOutgoingEdge() {
        return getSingleEdge(getOutgoingEdgesNotOfType(null).iterator(), null);
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

    public int compareTo(Vertex<T> that) {
        if (this.getData().getID() == that.getData().getID()) return 0;
        if (this.getData().getID() < that.getData().getID()) return -1;
        return 1;
    }
}
