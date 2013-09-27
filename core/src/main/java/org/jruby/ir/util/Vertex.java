package org.jruby.ir.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public class Vertex<T> implements Comparable<Vertex<T>> {
    private DirectedGraph graph;
    private T data;
    private Set<Edge<T>> incoming = null;
    private Set<Edge<T>> outgoing = null;
    int id;

    public Vertex(DirectedGraph graph, T data, int id) {
        this.graph = graph;
        this.data = data;
        this.id = id;
    }

    public void addEdgeTo(Vertex destination) {
        addEdgeTo(destination, null);
    }

    public void addEdgeTo(Vertex destination, Object type) {
        Edge edge = new Edge<T>(this, destination, type);
        getOutgoingEdges().add(edge);
        destination.getIncomingEdges().add(edge);
        graph.edges().add(edge);
    }

    public void addEdgeTo(T destination) {
        addEdgeTo(destination, null);
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
                if(outDegree() == 0) {
                    outgoing = null;
                }
                if(destination.inDegree() == 0) {
                    destination.incoming = null;
                }
                return true;
            }
        }

        return false;
    }

    public void removeAllIncomingEdges() {
        for (Edge edge: getIncomingEdges()) {
            edge.getSource().getOutgoingEdges().remove(edge);
            graph.edges().remove(edge);
        }
        incoming = null;
    }

    public void removeAllOutgoingEdges() {
        for (Edge edge: getOutgoingEdges()) {
            edge.getDestination().getIncomingEdges().remove(edge);
            graph.edges().remove(edge);
        }
        outgoing = null;
    }

    public void removeAllEdges() {
        removeAllIncomingEdges();
        removeAllOutgoingEdges();
    }

    public int inDegree() {
        return (incoming == null) ? 0 : incoming.size();
    }

    public int outDegree() {
        return (outgoing == null) ? 0 : outgoing.size();
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
        Edge<T> edge = getFirstEdge(getIncomingEdges().iterator());

        return edge == null ? null : edge.getSource().getData();
    }

    public T getIncomingSourceDataOfType(Object type) {
        Edge<T> edge = getFirstEdge(getIncomingEdgesOfType(type).iterator());

        return edge == null ? null : edge.getSource().getData();
    }

    public Iterable<T> getIncomingSourcesData() {
        return new DataIterable<T>(getIncomingEdges(), null, true, true);
    }

    public Iterable<T> getIncomingSourcesDataOfType(Object type) {
        return new DataIterable<T>(getIncomingEdges(), type, true, false);
    }

    public Iterable<T> getIncomingSourcesDataNotOfType(Object type) {
        return new DataIterable<T>(getIncomingEdges(), type, true, true);
    }

    public Iterable<Edge<T>> getOutgoingEdgesNotOfType(Object type) {
        return new EdgeTypeIterable<T>(getOutgoingEdges(), type, true);
    }

    public Iterable<T> getOutgoingDestinationsData() {
        return new DataIterable<T>(getOutgoingEdges(), null, false, true);
    }

    public Iterable<T> getOutgoingDestinationsDataOfType(Object type) {
        return new DataIterable<T>(getOutgoingEdges(), type, false, false);
    }

    public Iterable<T> getOutgoingDestinationsDataNotOfType(Object type) {
        return new DataIterable<T>(getOutgoingEdges(), type, false, true);
    }

    public T getOutgoingDestinationData() {
        Edge<T> edge = getFirstEdge(getOutgoingEdges().iterator());

        return edge == null ? null : edge.getDestination().getData();
    }

    public T getOutgoingDestinationDataOfType(Object type) {
        Edge<T> edge = getFirstEdge(getOutgoingEdgesOfType(type).iterator());

        return edge == null ? null : edge.getDestination().getData();
    }

    private Edge<T> getFirstEdge(Iterator<Edge<T>> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    public Edge<T> getIncomingEdgeOfType(Object type) {
        return getFirstEdge(getIncomingEdgesOfType(type).iterator());
    }

    public Edge<T> getOutgoingEdgeOfType(Object type) {
        return getFirstEdge(getOutgoingEdgesOfType(type).iterator());
    }

    public Edge<T> getIncomingEdge() {
        return getFirstEdge(getIncomingEdgesNotOfType(null).iterator());
    }

    public Edge<T> getOutgoingEdge() {
        return getFirstEdge(getOutgoingEdgesNotOfType(null).iterator());
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

    public int getID() {
        return data instanceof ExplicitVertexID ? ((ExplicitVertexID) data).getID() : id;
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
                buf.append(iterator.next().getDestination().getID()).append(",");
            }
            buf.append(iterator.next().getDestination().getID()).append("]");
        }

        edges = getIncomingEdges();
        size = edges.size();

        if (size > 0) {
            if (found) buf.append(", ");
            buf.append("<[");
            Iterator<Edge<T>> iterator = edges.iterator();

            for (int i = 0; i < size - 1; i++) {
                buf.append(iterator.next().getSource().getID()).append(",");
            }
            buf.append(iterator.next().getSource().getID()).append("]");
        }
        buf.append("\n");

        return buf.toString();
    }

    @Override
    public int compareTo(Vertex<T> that) {
        if (getID() == that.getID()) return 0;
        if (getID() < that.getID()) return -1;
        return 1;
    }
}
