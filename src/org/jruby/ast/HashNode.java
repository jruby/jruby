/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

/**
 * A Literal Hash that can represent either a {a=&amp;b, c=&amp;d} type expression or the list 
 * of default values in a method call.
 */
public class HashNode extends Node {
    private final ListNode listNode;
    
    public HashNode(ISourcePosition position, ListNode listNode) {
        super(position);
        this.listNode = listNode;
    }

    public NodeType getNodeType() {
        return NodeType.HASHNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitHashNode(this);
    }

    /**
     * Gets the listNode.
     * @return Returns a IListNode
     */
    public ListNode getListNode() {
        return listNode;
    }
    
    public List<Node> childNodes() {
        return createList(listNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyHash hash;

        ListNode list = this.listNode;
        if (list != null) {
            int size = list.size();

            // Enebo: Innefficient impl here should not matter since this interp will not be used in 9k
            hash = size <= 10 && !runtime.is2_0() ?
                    RubyHash.newSmallHash(runtime) :
                    RubyHash.newHash(runtime);

            for (int i = 0; i < size;) {
                // insert all nodes in sequence, hash them in the final instruction
                // KEY
                Node keyNode = list.get(i++);
                Node valueNode = list.get(i++);

                if (valueNode instanceof NilImplicitNode) {
                    IRubyObject kwargsVar = keyNode.interpret(runtime, context, self, aBlock);
                    IRubyObject kwargsHash = TypeConverter.convertToType19(kwargsVar, runtime.getHash(), "to_hash");

                    hash.merge_bang19(context, kwargsHash, aBlock);
                    continue;
                } 
                    
                IRubyObject key = keyNode.interpret(runtime, context, self, aBlock);
                IRubyObject value = valueNode.interpret(runtime, context, self, aBlock);

                if (size <= 10) {
                    asetSmall(runtime, hash, key, value);
                } else {
                    aset(runtime, hash, key, value);
                }
            }
        } else {
            hash = RubyHash.newSmallHash(runtime);
        }
      
        return hash;
    }
    
    protected void aset(Ruby runtime, RubyHash hash, IRubyObject key, IRubyObject value) {
        hash.fastASetCheckString(runtime, key, value);
    }

    protected void asetSmall(Ruby runtime, RubyHash hash, IRubyObject key, IRubyObject value) {
        hash.fastASetSmallCheckString(runtime, key, value);
    }
}
