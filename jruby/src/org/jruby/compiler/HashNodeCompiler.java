/*
 * HashNodeCompiler.java
 *
 * Created on March 9, 2007, 10:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.ast.HashNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;

/**
 *
 * @author headius
 */
public class HashNodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of HashNodeCompiler */
    public HashNodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        
        HashNode hashNode = (HashNode)node;
        
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            context.createEmptyHash();
            return;
        }
        
        ArrayCallback hashCallback = new ArrayCallback() {
            public void nextValue(Compiler context, Object sourceArray,
                                  int index) {
                ListNode listNode = (ListNode)sourceArray;
                int keyIndex = index * 2;
                NodeCompilerFactory.getCompiler(listNode.get(keyIndex)).compile(listNode.get(keyIndex), context);
                NodeCompilerFactory.getCompiler(listNode.get(keyIndex + 1)).compile(listNode.get(keyIndex + 1), context);
            }
        };
        
        context.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }
}
