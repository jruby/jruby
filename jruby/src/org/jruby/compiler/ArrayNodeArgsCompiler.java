/*
 * ArrayNodeArgsCompiler.java
 *
 * Created on January 3, 2007, 6:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.ArrayNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class ArrayNodeArgsCompiler implements NodeCompiler {
    
    /** Creates a new instance of ArrayNodeArgsCompiler */
    public ArrayNodeArgsCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        ArrayNode arrayNode = (ArrayNode)node;
        
        ArrayCallback callback = new ArrayCallback() {
            public void nextValue(Compiler context, Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                NodeCompilerFactory.getCompiler(node).compile(node, context);
            }
        };
        
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        // leave as a normal array
    }
    
}
