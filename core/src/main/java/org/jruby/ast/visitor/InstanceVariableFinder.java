package org.jruby.ast.visitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ast.ClassNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.NilRestArgNode;
import org.jruby.ast.Node;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;

/**
 * Visitor to search AST nodes for instance variables. Certain nodes are
 * ignored during walking since they always create a new context with a new
 * self.
 * 
 * Example usage:
 * 
 * <code>
 * Node node = getNodeFromSomewhere();
 * InstanceVariableFinder finder = new InstanceVariableFinder();
 * node.accept(finder);
 * System.out.println("found: " + finder.getFoundVariables);
 * </code>
 *
 * @deprecated See {@link RubyModule#discoverInstanceVariables()} for the working version
 */
@Deprecated
public class InstanceVariableFinder extends AbstractNodeVisitor<Void> {

    @Override
    protected Void defaultVisit(Node iVisited) {
        visitChildren(iVisited);
        return null;
    }

    /** The set of instance variables found during walking. */
    private final Set<RubySymbol> foundVariables = new HashSet<>();
    
    /**
     * Walk a node and its children looking for instance variables using a new
     * InstanceVariableFinder. Return an array of the variable names found.
     * 
     * @param node the node to walk
     * @return an array of instance variable names found
     */
    public static Set<RubySymbol> findVariables(Node node) {
        InstanceVariableFinder ivf = new InstanceVariableFinder();
        node.accept(ivf);
        return ivf.getFoundVariables();
    }
    
    /**
     * Return the Set of all instance variables found during walking.
     * 
     * @return a Set of all instance variable names found
     */
    public Set<RubySymbol> getFoundVariables() {
        return foundVariables;
    }
    
    /**
     * ClassNode creates a new scope and self, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitClassNode(ClassNode iVisited) {
        return null;
    }

    /**
     * Add the name of the instance variable being assigned to our set of
     * instance variable names and continue to walk child nodes.
     * 
     * @return null
     */
    @Override
    public Void visitInstAsgnNode(InstAsgnNode iVisited) {
        foundVariables.add(iVisited.getName());
        List<Node> nodes = iVisited.childNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            node.accept(this);
        }
        return null;
    }

    /**
     * Add the name of the instance variable being retrieved to our set of
     * instance variable names and continue to walk child nodes.
     * 
     * @return null
     */
    @Override
    public Void visitInstVarNode(InstVarNode iVisited) {
        foundVariables.add(iVisited.getName());
        List<Node> nodes = iVisited.childNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
        }
        return null;
    }

    /**
     * ModuleNode creates a new scope and self, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitModuleNode(ModuleNode iVisited) {
        return null;
    }

    @Override
    public Void visitNilRestArgNode(NilRestArgNode iVisited) {
        return null;
    }

    /**
     * PreExeNode can't appear in methods, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitPreExeNode(PreExeNode iVisited) {
        return null;
    }

    /**
     * PostExeNode can't appear in methods, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitPostExeNode(PostExeNode iVisited) {
        return null;
    }
}
