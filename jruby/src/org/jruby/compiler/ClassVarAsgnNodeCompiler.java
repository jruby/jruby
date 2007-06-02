package org.jruby.compiler;

import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.Node;

public class ClassVarAsgnNodeCompiler implements NodeCompiler {

    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode)node;
        
        // FIXME: probably more efficient with a callback
        NodeCompilerFactory.getCompiler(classVarAsgnNode.getValueNode()).compile(classVarAsgnNode.getValueNode(), context);
        context.assignClassVariable(classVarAsgnNode.getName());
    }

}
