package org.jruby.compiler.ir.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Meant to be single-threaded.  More work on whole impl if not.
 */
public class DirectedGraph<T> {
    private Map<T, Vertex<T>> vertices = new HashMap<T, Vertex<T>>();
    private Set<Edge> edges = new HashSet<Edge>();
    
    public Collection<Vertex<T>> vertices() {
        return vertices.values();
    }
    
    public Collection<Edge> edges() {
        return edges;
    }
    
    public Collection<T> allData() {
        return vertices.keySet();
    }
    
    public void addEdge(T source, T destination, Object type) {
        vertexFor(source).addEdgeTo(destination, type);
    }
    
    public void removeEdge(Edge edge) {
        edge.getSource().removeEdgeTo(edge.getDestination());
    }
    
    public void removeEdge(T source, T destination) {
        for (Edge edge: vertexFor(source).getOutgoingEdges()) {
            if (edge.getDestination().getData() == destination) {
                vertexFor(source).removeEdgeTo(edge.getDestination());
                return;
            }
        }
    }
    
    public Vertex<T> vertexFor(T data) {
        Vertex vertex = vertices.get(data);
        
        if (vertex != null) return vertex;
        
        vertex = new Vertex(this, data);
        
        vertices.put(data, vertex);
        
        return vertex;
    }
    
    public void removeVertexFor(T data) {
        Vertex vertex = vertexFor(data);
        vertices.remove(data);
        vertex.removeAllEdges();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Directed-Graph:\n");
        
        for (T block: allData()) {
            buf.append(vertexFor(block));
        }
        
        return buf.toString();
    }
}