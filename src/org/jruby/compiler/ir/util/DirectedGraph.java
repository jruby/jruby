package org.jruby.compiler.ir.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jruby.compiler.ir.representations.BasicBlock;

/**
 * Meant to be single-threaded.  More work on whole impl if not.
 */
public class DirectedGraph {
    private Map<BasicBlock, Vertex> vertices = new HashMap<BasicBlock, Vertex>();
    private Set<Edge> edges = new HashSet<Edge>();
    
    public Collection<Vertex> vertices() {
        return vertices.values();
    }
    
    public Collection<Edge> edges() {
        return edges;
    }
    
    public Collection<BasicBlock> basicBlocks() {
        return vertices.keySet();
    }
    
    public void addEdge(BasicBlock source, BasicBlock destination, Object type) {
        vertexFor(source).addEdgeTo(destination, type);
    }
    
    public void removeEdge(Edge edge) {
        edge.getSource().removeEdgeTo(edge.getDestination());
    }
    
    public void removeEdge(BasicBlock source, BasicBlock destination) {
        for (Edge edge: vertexFor(source).getOutgoingEdges()) {
            if (edge.getDestination().getBasicBlock() == destination) {
                vertexFor(source).removeEdgeTo(edge.getDestination());
                return;
            }
        }
    }
    
    public Vertex vertexFor(BasicBlock basicBlock) {
        Vertex vertex = vertices.get(basicBlock);
        
        if (vertex != null) return vertex;
        
        vertex = new Vertex(this, basicBlock);
        
        vertices.put(basicBlock, vertex);
        
        return vertex;
    }
    
    public void removeVertexFor(BasicBlock basicBlock) {
        Vertex vertex = vertexFor(basicBlock);
        vertices.remove(basicBlock);
        vertex.removeAllEdges();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Directed-Graph:\n");
        
        for (BasicBlock block: basicBlocks()) {
            buf.append(vertexFor(block));
        }
        
        return buf.toString();
    }
}