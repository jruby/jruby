package org.jruby.compiler.ir.util;

/**
 *
 */
public class Edge {
    private Vertex source;
    private Vertex destination;
    private Object type;
    
    public Edge(Vertex source, Vertex destination, Object type) {
        this.source = source;
        this.destination = destination;
        this.type = type;

    }
    
    public Vertex getDestination() {
        return destination;
    }

    public Vertex getSource() {
        return source;
    }

    public Object getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "<" + source.getBasicBlock().getID() + " --> " + 
                destination.getBasicBlock().getID() + "> (" + type + ")";        
    }
}
