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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.ablaf.ast.INode;
import org.ablaf.common.IErrorHandler;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.util.ListNodeUtil;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.common.IErrors;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AssignmentVisitor extends AbstractVisitor {
    private Ruby ruby;
    private ThreadContext threadContext;
    private IRubyObject self;

    private IErrorHandler errorHandler;

    private IRubyObject value;
    private boolean check;
    private IRubyObject result;

    public AssignmentVisitor(Ruby ruby, IRubyObject self) {
        this.ruby = ruby;
        this.self = self;
        this.threadContext = ruby.getCurrentContext();
    }

    public IRubyObject assign(INode node, IRubyObject value, boolean check) {
        this.value = value;
        this.check = check;

        acceptNode(node);

        return result;
    }

    /**
     * @see AbstractVisitor#visitNode(INode)
     */
    protected void visitNode(INode iVisited) {
        Asserts.notReached();
    }

    /**
     * @see AbstractVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        EvaluateVisitor evaluator = EvaluateVisitor.createVisitor(self);

        IRubyObject receiver = evaluator.eval(iVisited.getReceiverNode());

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.getMetaClass().call(receiver, iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
        } else { // element set
            RubyArray args = (RubyArray) evaluator.eval(iVisited.getArgsNode());
            args.append(value);
            receiver.getMetaClass().call(receiver, iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
        }
    }

    /**
     * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        threadContext.getRubyClass().setClassVar(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        if (ruby.isVerbose() && threadContext.getRubyClass().isSingleton()) {
            errorHandler.handleError(IErrors.WARN, "Declaring singleton class variable.");
        }
        threadContext.getRubyClass().declareClassVar(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        threadContext.getRubyClass().defineConstant(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        threadContext.getCurrentDynamicVars().set(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        ruby.getGlobalVariables().set(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        self.setInstanceVariable(iVisited.getName(), value);
    }

    /**
     * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        threadContext.getScopeStack().setValue(iVisited.getCount(), value);
    }

    /**
     * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        // Make sure value is an array.
        if (value == null) {
            value = RubyArray.newArray(ruby, 0);
        } else if (!(value instanceof RubyArray)) {
            IRubyObject newValue = value.convertToType("Array", "to_ary", false);
            if (newValue.isNil()) {
                newValue = RubyArray.newArray(ruby, value);
            }
            value = newValue;
        }

        // Assign the values.
        int valueLen = ((RubyArray)value).getLength();
        int varLen = ListNodeUtil.getLength(iVisited.getHeadNode());

        Iterator iter = iVisited.getHeadNode() != null ? iVisited.getHeadNode().iterator() : Collections.EMPTY_LIST.iterator();
        for (int i = 0; i < valueLen && iter.hasNext(); i++) {
            new AssignmentVisitor(ruby, self).assign((INode)iter.next(), ((RubyArray)value).entry(i), check);
        }

        if (check && varLen > valueLen) {
            throw new ArgumentError(ruby, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        if (iVisited.getArgsNode() != null) {
            if (iVisited.getArgsNode() instanceof StarNode) {
                // no check for '*'
            } else if (varLen < valueLen) {
                ArrayList newList = new ArrayList(((RubyArray)value).getList().subList(varLen, valueLen));
                new AssignmentVisitor(ruby, self).assign(iVisited.getArgsNode(), RubyArray.newArray(ruby, newList), check);
            } else {
                new AssignmentVisitor(ruby, self).assign(iVisited.getArgsNode(), RubyArray.newArray(ruby, 0), check);
            }
        } else if (check && valueLen < varLen) {
            throw new ArgumentError(ruby, "Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (iter.hasNext()) {
            new AssignmentVisitor(ruby, self).assign((INode)iter.next(), ruby.getNil(), check);
        }

        result = value;
    }
}
