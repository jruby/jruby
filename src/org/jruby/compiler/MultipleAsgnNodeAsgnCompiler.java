/*
 * MultipleAsgnNodeAsgnCompiler.java
 *
 * Created on January 31, 2007, 11:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import java.util.Collections;
import java.util.Iterator;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class MultipleAsgnNodeAsgnCompiler implements NodeCompiler {
    
    /** Creates a new instance of MultipleAsgnNodeAsgnCompiler */
    public MultipleAsgnNodeAsgnCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
        
        //int valueLen = value.getLength();
        int varLen = multipleAsgnNode.getHeadNode() == null ? 0 : multipleAsgnNode.getHeadNode().size();
        
        Iterator iter = multipleAsgnNode.getHeadNode() != null ? multipleAsgnNode.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
    }
    
}
