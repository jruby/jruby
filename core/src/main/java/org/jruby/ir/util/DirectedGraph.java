package org.jruby.ir.util;

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
public class DirectedGraph<T> {
    private Map<T, Vertex<T>> vertices = new HashMap<T, Vertex<T>>();
    private Set<Edge<T>> edges = new HashSet<Edge<T>>();
    private ArrayList inOrderVerticeData = new ArrayList();
    int vertexIDCounter = 0;

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

    /**
     * @return data in the order it was added to this graph.
     */
    public Collection<T> getInorderData() {
        return inOrderVerticeData;
    }

    public void addEdge(T source, T destination, Object type) {
        vertexFor(source).addEdgeTo(destination, type);
    }

    public void removeEdge(Edge edge) {
        edge.getSource().removeEdgeTo(edge.getDestination());
    }

    public void removeEdge(T source, T destination) {
        if (findVertexFor(source) != null) {
            for (Edge edge: vertexFor(source).getOutgoingEdges()) {
                if (edge.getDestination().getData() == destination) {
                    vertexFor(source).removeEdgeTo(edge.getDestination());
                    return;
                }
            }
        }
    }

    public Vertex<T> findVertexFor(T data) {
        return vertices.get(data);
    }

    /**
     * @return vertex for given data. If vertex is not present it creates vertex and returns it.
     */
    public Vertex<T> vertexFor(T data) {
        Vertex vertex = vertices.get(data);

        if (vertex != null) return vertex;

        vertex = new Vertex(this, data, vertexIDCounter++);
        inOrderVerticeData.add(data);

        vertices.put(data, vertex);

        return vertex;
    }

    public void removeVertexFor(T data) {
        if (findVertexFor(data) != null) {
            Vertex vertex = vertexFor(data);
            vertices.remove(data);
            inOrderVerticeData.remove(data);
            vertex.removeAllEdges();
        }
    }

    /**
     * @return the number of vertices in the graph.
     */
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
