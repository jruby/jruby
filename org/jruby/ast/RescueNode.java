/*
 * RescueNode.java - description
 * Created on 01.03.2002, 23:52:26
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.ast;

import org.ablaf.ast.*;
import org.ablaf.common.*;

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RescueNode extends AbstractNode {
    private INode bodyNode;
    private IListNode rescueNodes;
    private INode elseNode;
    
    public RescueNode(ISourcePosition position, INode bodyNode, IListNode rescueNodes, INode elseNode) {
        super(position);
        
        this.bodyNode = bodyNode;
        this.rescueNodes = rescueNodes;
        this.elseNode = elseNode;
    }

    /*public RubyObject eval(Ruby ruby, RubyObject self) {
        // RubyObject previousException = ruby.getGlobalVar("$!");
        RescuedBlock: while (true) {
            try {
                // Execute recued Block
                RubyObject result = getHeadNode().eval(ruby, self);
    
                // If no exception is thrown execute else Block
                if (getElseNode() != null) {
                    return getElseNode().eval(ruby, self);
                }
    
                return result;
            } catch (RaiseException raExcptn) {
                ruby.setGlobalVar("$!", raExcptn.getActException());
                
                ruby.setSourceLine(getLine());
    
                Node body = getResqNode();
                while (body != null) {
                    if (isRescueHandled(ruby, raExcptn.getActException(), self, body)) {
                        try {
                            return body.eval(ruby, self);
                        } catch (RetryException rExcptn) {
                            ruby.setGlobalVar("$!", ruby.getNil());
                            continue RescuedBlock;
                        }
                    }
                    body = body.getHeadNode();
                }
                throw raExcptn;
            } finally {
                ruby.setGlobalVar("$!", ruby.getNil());
            }
        }
    }
    
    private boolean isRescueHandled(Ruby ruby, RubyException actExcptn, RubyObject self, Node node) {
        // TMP_PROTECT;
        
        if (node.getArgsNode() == null) {
            return actExcptn.kind_of(ruby.getExceptions().getStandardError()).isTrue();
        }
    
        RubyBlock tmpBlock = ArgsUtil.beginCallArgs(ruby);
        RubyPointer args = ArgsUtil.setupArgs(ruby, self, node.getArgsNode());
        ArgsUtil.endCallArgs(ruby, tmpBlock);
        
        for (int i = 0; i < args.size(); i++) {
            if (args.getRuby(i).kind_of(ruby.getClasses().getModuleClass()).isFalse()) {
                throw new TypeError(ruby, "class or module required for rescue clause");
            }
            if (actExcptn.kind_of((RubyModule)args.getRuby(i)).isTrue()) {
                return true;
            }
        }
        return false;
    }*/

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitRescueNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(INode bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the elseNode.
     * @return Returns a INode
     */
    public INode getElseNode() {
        return elseNode;
    }

    /**
     * Sets the elseNode.
     * @param elseNode The elseNode to set
     */
    public void setElseNode(INode elseNode) {
        this.elseNode = elseNode;
    }

    /**
     * Gets the rescueNodes.
     * @return Returns a IListNode
     */
    public IListNode getRescueNodes() {
        return rescueNodes;
    }

    /**
     * Sets the rescueNodes.
     * @param rescueNodes The rescueNodes to set
     */
    public void setRescueNodes(IListNode rescueNodes) {
        this.rescueNodes = rescueNodes;
    }

}