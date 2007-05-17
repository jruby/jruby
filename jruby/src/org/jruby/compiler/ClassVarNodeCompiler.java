package org.jruby.compiler;

import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Node;

public class ClassVarNodeCompiler implements NodeCompiler {

    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarNode classVarNode = (ClassVarNode)node;
        
        context.retrieveClassVariable(classVarNode.getName());
    }

}
