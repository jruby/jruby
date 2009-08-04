/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.SplatNode;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.lexer.yacc.Token;

public class ParserSupport19 extends ParserSupport {
    @Override
    public AssignableNode assignable(Token lhs, Node value) {
        checkExpression(value);

        switch (lhs.getType()) {
            case Tokens.kSELF:
                throw new SyntaxException(PID.CANNOT_CHANGE_SELF, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't change the value of self");
            case Tokens.kNIL:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to nil", "nil");
            case Tokens.kTRUE:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to true", "true");
            case Tokens.kFALSE:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to false", "false");
            case Tokens.k__FILE__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __FILE__", "__FILE__");
            case Tokens.k__LINE__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __LINE__", "__LINE__");
            case Tokens.k__ENCODING__:
                throw new SyntaxException(PID.INVALID_ASSIGNMENT, lhs.getPosition(),
                        lexer.getCurrentLine(), "Can't assign to __ENCODING__", "__ENCODING__");
            case Tokens.tIDENTIFIER:
                // ENEBO: 1.9 has CURR nodes for local/block variables.  We don't.  I believe we follow proper logic
                return currentScope.assign(lhs.getPosition(), (String) lhs.getValue(), makeNullNil(value));
            case Tokens.tCONSTANT:
                if (isInDef() || isInSingle()) {
                    throw new SyntaxException(PID.DYNAMIC_CONSTANT_ASSIGNMENT, lhs.getPosition(),
                            lexer.getCurrentLine(), "dynamic constant assignment");
                }
                return new ConstDeclNode(lhs.getPosition(), (String) lhs.getValue(), null, value);
            case Tokens.tIVAR:
                return new InstAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
            case Tokens.tCVAR:
                return new ClassVarAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
            case Tokens.tGVAR:
                return new GlobalAsgnNode(lhs.getPosition(), (String) lhs.getValue(), value);
        }

        throw new SyntaxException(PID.BAD_IDENTIFIER, lhs.getPosition(), lexer.getCurrentLine(),
                "identifier " + (String) lhs.getValue() + " is not valid to set", lhs.getValue());
    }

    @Override
    protected void getterIdentifierError(ISourcePosition position, String identifier) {
        throw new SyntaxException(PID.BAD_IDENTIFIER, position, "identifier " +
                identifier + " is not valid to get", identifier);
    }

    /**
     * If node is a splat and it is splatting a literal array then return the literal array.
     * Otherwise return null.  This allows grammar to not splat into a Ruby Array if splatting
     * a literal array.
     */
    public Node splat_array(Node node) {
        if (node instanceof SplatNode) node = ((SplatNode) node).getValue();
        if (node instanceof ArrayNode) return node;
        return null;
    }

    public Node arg_append(Node node1, Node node2) {
        if (node1 == null) return new ArrayNode(node2.getPosition(), node2);
        if (node1 instanceof ListNode) return ((ListNode) node1).add(node2);
        if (node1 instanceof BlockPassNode) return arg_append(((BlockPassNode) node1).getBodyNode(), node2);
        if (node1 instanceof ArgsPushNode) {
            ArgsPushNode pushNode = (ArgsPushNode) node1;
            Node body = pushNode.getSecondNode();

            return new ArgsCatNode(pushNode.getPosition(), pushNode.getFirstNode(),
                    new ArrayNode(body.getPosition(), body).add(node2));
        }

        return new ArgsPushNode(position(node1, node2), node1, node2);
    }

    // ENEBO: Totally weird naming (in MRI is not allocated and is a local var name)
    public boolean is_local_id(Token identifier) {
        String name = (String) identifier.getValue();
        
        return getCurrentScope().getLocalScope().isDefined(name) < 0;
    }

    public ListNode list_append(Node list, Node item) {
        if (list == null) return new ArrayNode(item.getPosition(), item);
        if (!(list instanceof ListNode)) return new ArrayNode(list.getPosition(), list).add(item);

        return ((ListNode) list).add(item);
    }

    public Node new_bv(Token identifier) {
        if (!is_local_id(identifier)) {
            getterIdentifierError(identifier.getPosition(), (String) identifier.getValue());
        }
        shadowing_lvar(identifier);
        arg_var(identifier);

        return null;
    }

    public int arg_var(Token identifier) {
        return getCurrentScope().addVariableThisScope((String) identifier.getValue());
    }
    
    public void shadowing_lvar(Token identifier) {
        String name = (String) identifier.getValue();

        if (getCurrentScope().isDefined(name) > 0) {
            if (warnings.isVerbose()) warnings.warning(ID.STATEMENT_NOT_REACHED, identifier.getPosition(), "shadowing outer local variable - " + name);
        }
    }

    public ListNode list_concat(Node first, Node second) {
        if (first instanceof ListNode) {
            if (second instanceof ListNode) {
                return ((ListNode) first).addAll((ListNode) second);
            } else {
                return ((ListNode) first).addAll(second);
            }
        }

        return new ArrayNode(first.getPosition(), first).add(second);
    }
}
