/*
 * AssignmentVisitor.java - description
 * Created on 13.03.2002, 15:54:53
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
package org.jruby.evaluator;

import java.util.*;

import org.ablaf.ast.*;
import org.ablaf.common.*;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;
import org.jruby.ast.util.*;
import org.jruby.ast.visitor.*;
import org.jruby.common.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AssignmentVisitor extends AbstractVisitor {
    private Ruby ruby;
    private RubyObject self;

    private IErrorHandler errorHandler;

    private RubyObject value;
    private boolean check;
    private RubyObject result;

    public AssignmentVisitor(Ruby ruby, RubyObject self) {
        this.ruby = ruby;
        this.self = self;
    }

    public RubyObject assign(INode node, RubyObject value, boolean check) {
        this.value = value;
        this.check = check;

        acceptNode(node);

        return result;
    }

  	/**
     * @see AbstractVisitor#visitNode(INode)
     */
    protected void visitNode(INode iVisited) {
        // assert false;
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        EvaluateVisitor evaluator = new EvaluateVisitor(ruby, self);
        
        RubyObject receiver = evaluator.eval(iVisited.getReceiverNode());

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.getRubyClass().call(receiver, iVisited.getName(), new RubyPointer(new RubyObject[] {value}), 0);
        } else { // element set
            RubyArray args = (RubyArray)evaluator.eval(iVisited.getArgsNode());
            args.push(value);
            receiver.getRubyClass().call(receiver, iVisited.getName(), new RubyPointer(args.toJavaArray()), 0);
        }
    }

    /**
     * @see NodeVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        ruby.getCBase().setClassVar(iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        if (ruby.isVerbose() && ruby.getCBase().isSingleton()) {
            errorHandler.handleError(IErrors.WARN, "Declaring singleton class variable.");
        }
        ruby.getCBase().declareClassVar(iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        ruby.getRubyClass().defineConstant(iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitDAsgnCurrNode(DAsgnCurrNode)
     */
    public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
        RubyVarmap.assignCurrent(ruby, iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        RubyVarmap.assign(ruby, iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        ruby.setGlobalVar(iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        self.setInstanceVar(iVisited.getName(), value);
    }

    /**
     * @see NodeVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        ruby.getScope().setValue(iVisited.getCount(), value);
    }

    /**
     * @see NodeVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        // Make sure value is an array.
        if (value == null) {
            value = RubyArray.newArray(ruby, 0);
        } else if (!(value instanceof RubyArray)) {
            RubyObject newValue = value.convertToType("Array", "to_ary", false);
            if (newValue.isNil()) {
                newValue = RubyArray.newArray(ruby, value);
            }
            value = newValue;
        }
        
        // Assign the values.
        int valueLen = ((RubyArray)value).getLength();
        int varLen = ListNodeUtil.getLength(iVisited.getHeadNode());
        
        Iterator iter = iVisited.getHeadNode() != null ? iVisited.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        int i = 0;
        for (; i < valueLen && iter.hasNext(); i++) {
            new AssignmentVisitor(ruby, self).assign((INode)iter.next(), ((RubyArray)value).entry(i), check);
        }

        if (check && varLen > valueLen) {
            throw new ArgumentError(ruby, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (iVisited.getArgsNode() != null) {
            if (iVisited.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                List newList = ((RubyArray)value).getList().subList(varLen, valueLen);
                new AssignmentVisitor(ruby, self).assign(iVisited.getArgsNode(), RubyArray.newArray(ruby, newList), check);
            } else {
                new AssignmentVisitor(ruby, self).assign(iVisited.getArgsNode(), RubyArray.newArray(ruby, 0), check);
            }
        } else if (check && valueLen < varLen) {
            throw new ArgumentError(ruby, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            i++;
            new AssignmentVisitor(ruby, self).assign((INode)iter.next(), ruby.getNil(), check);
        }

        result = value;
    }
}