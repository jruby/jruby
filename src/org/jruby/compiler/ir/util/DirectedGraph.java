package org.jruby.compiler.ir.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Meant to be single-threaded.  More work on whole impl if not.
 */
public class DirectedGraph<T extends DataInfo> {
    private Map<T, Vertex<T>> vertices = new HashMap<T, Vertex<T>>();
    private Set<Edge<T>> edges = new HashSet<Edge<T>>();
    
    public Collection<Vertex<T>> vertices() {
        return vertices.values();
    }
    
    public Collection<Edge<T>> edges() {
        return edges;
    }
    
    public Iterable<Edge<T>> edgesOfType(Object type) {
        return new EdgeTypeIterable<T>(edges, type);
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
    
    public int size() {
        return allData().size();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        
        ArrayList<Vertex<T>> verts = new ArrayList<Vertex<T>>(vertices.values());
        Collections.sort(verts);
        for (Vertex<T> vertex: verts) {
            buf.append(vertex);
        }
        
        return buf.toString();
    }
}