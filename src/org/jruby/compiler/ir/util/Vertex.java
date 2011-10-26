package org.jruby.compiler.ir.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jruby.compiler.ir.representations.BasicBlock;

/**
 *
 */
public class Vertex {
    private DirectedGraph graph;
    private BasicBlock basicBlock;
    private Set<Edge> incoming = null;
    private Set<Edge> outgoing = null;
    
    public Vertex(DirectedGraph graph, BasicBlock basicBlock) {
        this.graph = graph;
        this.basicBlock = basicBlock;
    }

    public void addEdgeTo(Vertex destination, Object type) {
        Edge edge = new Edge(this, destination, type);
        getOutgoingEdges().add(edge);
        destination.getIncomingEdges().add(edge);
        graph.edges().add(edge);
    }
    
    public void addEdgeTo(BasicBlock destination, Object type) {
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
    
    public Iterator<Edge> getIncomingEdgesOfType(Object type) {
        return new EdgeTypeIterator(getIncomingEdges(), type);
    }
    
    public Iterator<Edge> getOutgoingEdgesOfType(Object type) {
        return new EdgeTypeIterator(getOutgoingEdges(), type);
    }
    
    public Set<Edge> getIncomingEdges() {
        if (incoming == null) incoming = new HashSet<Edge>();
        
        return incoming;
    }
    
    public Set<Edge> getOutgoingEdges() {
        if (outgoing == null) outgoing = new HashSet<Edge>();
        
        return outgoing;
    }    
    
    public BasicBlock getBasicBlock() {
        return basicBlock;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Vertex ");
        
        buf.append(basicBlock.getID()).append(":\nincoming edges\n");
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
