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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.unsafe.UnsafeFactory;

/**
 * Represents a rescue statement
 */
public class RescueNode extends Node {
    private final Node bodyNode;
    private final RescueBodyNode rescueNode;
    private final Node elseNode;
    
    public RescueNode(ISourcePosition position, Node bodyNode, RescueBodyNode rescueNode, Node elseNode) {
        super(position);
        this.bodyNode = bodyNode;
        this.rescueNode = rescueNode;
        this.elseNode = elseNode;
    }

    public NodeType getNodeType() {
        return NodeType.RESCUENODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitRescueNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the elseNode.
     * @return Returns a Node
     */
    public Node getElseNode() {
        return elseNode;
    }

    /**
     * Gets the first rescueNode.
     * @return Returns a Node
     */
    public RescueBodyNode getRescueNode() {
        return rescueNode;
    }
    
    public List<Node> childNodes() {
        return Node.createList(rescueNode, bodyNode, elseNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return interpretWithJavaExceptions(runtime, context, self, aBlock);
    }

    private IRubyObject interpretWithJavaExceptions(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject result = null;
        boolean exceptionRaised = false;
        RescuedBlock : while (true) {
            IRubyObject globalExceptionState = runtime.getGlobalVariables().get("$!");
            boolean anotherExceptionRaised = false;
            try {
                result = executeBody(runtime, context, self, aBlock);
                break;
            } catch (RaiseException raiseJump) {
                try {
                    result = handleException(runtime, context, self, aBlock, raiseJump);
                    exceptionRaised = true;
                    break;
                } catch (JumpException.RetryJump rj) {
                    // let RescuedBlock continue
                } catch (RaiseException je) {
                    anotherExceptionRaised = true;
                    throw je;
                }
            } catch (JumpException.FlowControlException flow) {
                // just rethrow
                throw flow;
            } catch (Throwable t) {
                if (t instanceof Unrescuable) {
                    UnsafeFactory.getUnsafe().throwException(t);
                }
                try {
                    result = handleJavaException(runtime, context, self, aBlock, t);
                    exceptionRaised = true;
                    break;
                } catch (JumpException.RetryJump rj) {
                    // let RescuedBlock continue
                } catch (RaiseException je) {
                    anotherExceptionRaised = true;
                    throw je;
                }
            } finally {
                // clear exception when handled or retried
                if (!anotherExceptionRaised)
                    runtime.getGlobalVariables().set("$!", globalExceptionState);
            }
        }

        // If no exception is thrown execute else block
        if (elseNode != null && !exceptionRaised) {
            if (rescueNode == null) {
                runtime.getWarnings().warn(ID.ELSE_WITHOUT_RESCUE, elseNode.getPosition(), "else without rescue is useless");
            }
            result = elseNode.interpret(runtime, context, self, aBlock);
        }
        return result;
    }

    private IRubyObject handleException(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock, RaiseException raiseJump) {
        RubyException raisedException = raiseJump.getException();
        // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
        // falsely set $! to nil and this sets it back to something valid.  This should 
        // get fixed at the same time we address bug #1296484.
        runtime.getGlobalVariables().set("$!", raisedException);

        RescueBodyNode cRescueNode = rescueNode;

        while (cRescueNode != null) {
            IRubyObject[] exceptions = getExceptions(cRescueNode, runtime, context, self, aBlock);

            if (RuntimeHelpers.isExceptionHandled(raisedException, exceptions, context).isTrue()) {
                return cRescueNode.interpret(runtime,context, self, aBlock);
            }

            cRescueNode = cRescueNode.getOptRescueNode();
        }

        // no takers; bubble up
        throw raiseJump;
    }
    
    private IRubyObject handleJavaException(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock, Throwable throwable) {
        RescueBodyNode cRescueNode = rescueNode;

        while (cRescueNode != null) {
            IRubyObject[] exceptions = getExceptions(cRescueNode, runtime, context, self, aBlock);

            if (RuntimeHelpers.isJavaExceptionHandled(throwable, exceptions, context).isTrue()) {
                runtime.getGlobalVariables().set("$!", JavaUtil.convertJavaToUsableRubyObject(runtime, throwable));
                return cRescueNode.interpret(runtime, context, self, aBlock);
            }

            cRescueNode = cRescueNode.getOptRescueNode();
        }

        // no takers; bubble up
        UnsafeFactory.getUnsafe().throwException(throwable);
        throw new RuntimeException("Unsafe.throwException failed");
    }

    private IRubyObject executeBody(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        // FIXME: Make bodyNode non-null in parser
        if (bodyNode == null) {
            return runtime.getNil();
        }
        // Execute rescue block
        return bodyNode.interpret(runtime, context, self, aBlock);
    }

    private IRubyObject[] getExceptions(RescueBodyNode cRescueNode, Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        Node exceptionNodes = cRescueNode.getExceptionNodes();
        IRubyObject[] exceptions;

        if (exceptionNodes == null) {
            exceptions = new IRubyObject[]{runtime.getStandardError()};
        } else {
            exceptions = ASTInterpreter.setupArgs(runtime, context, exceptionNodes, self, aBlock);
        }
        return exceptions;
    }
}
