/*
 * Created on Sep 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.IRuby;
import org.jruby.MetaClass;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BinaryOperatorNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException.JumpType;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class EvaluationState {
    public static IRubyObject eval(ThreadContext context, Node node, IRubyObject self) {
        IRuby runtime = context.getRuntime();
        if (node == null) return runtime.getNil();

        try {
            switch (node.nodeId) {
            case NodeTypes.ALIASNODE: {
                AliasNode iVisited = (AliasNode) node;
    
                if (context.getRubyClass() == null) {
                    throw runtime.newTypeError("no class to make alias");
                }
    
                context.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
                context.getRubyClass().callMethod("method_added", runtime.newSymbol(iVisited.getNewName()));
    
                return runtime.getNil();
            }
            case NodeTypes.ANDNODE: {
                BinaryOperatorNode iVisited = (BinaryOperatorNode) node;
    
                // add in reverse order
                IRubyObject result = eval(context, iVisited.getFirstNode(), self);
                if (!result.isTrue()) return result;
                return eval(context, iVisited.getSecondNode(), self);
            }
            case NodeTypes.ARGSCATNODE: {
                ArgsCatNode iVisited = (ArgsCatNode) node;
    
                IRubyObject args = eval(context, iVisited.getFirstNode(), self);
                IRubyObject secondArgs = splatValue(eval(context, iVisited.getSecondNode(), self));
                RubyArray list = args instanceof RubyArray ? (RubyArray) args : runtime.newArray(args);
    
                return list.concat(secondArgs);
            }
                //                case NodeTypes.ARGSNODE:
                //                EvaluateVisitor.argsNodeVisitor.execute(this, node);
                //                break;
                //                case NodeTypes.ARGUMENTNODE:
                //                EvaluateVisitor.argumentNodeVisitor.execute(this, node);
                //                break;
            case NodeTypes.ARRAYNODE: {
                ArrayNode iVisited = (ArrayNode) node;
                IRubyObject[] array = new IRubyObject[iVisited.size()];
                int i = 0;
                for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                    Node next = (Node) iterator.next();
    
                    array[i++] = eval(context, next, self);
                }
    
                return runtime.newArray(array);
            }
                //                case NodeTypes.ASSIGNABLENODE:
                //                EvaluateVisitor.assignableNodeVisitor.execute(this, node);
                //                break;
            case NodeTypes.BACKREFNODE: {
                BackRefNode iVisited = (BackRefNode) node;
                IRubyObject backref = context.getBackref();
                switch (iVisited.getType()) {
                case '~':
                    return backref;
                case '&':
                    return RubyRegexp.last_match(backref);
                case '`':
                    return RubyRegexp.match_pre(backref);
                case '\'':
                    return RubyRegexp.match_post(backref);
                case '+':
                    return RubyRegexp.match_last(backref);
                }
                break;
            }
            case NodeTypes.BEGINNODE: {
                BeginNode iVisited = (BeginNode) node;
    
                return eval(context, iVisited.getBodyNode(), self);
            }
            case NodeTypes.BIGNUMNODE: {
                BignumNode iVisited = (BignumNode) node;
                return RubyBignum.newBignum(runtime, iVisited.getValue());
            }
                //                case NodeTypes.BINARYOPERATORNODE:
                //                EvaluateVisitor.binaryOperatorNodeVisitor.execute(this, node);
                //                break;
                //                case NodeTypes.BLOCKARGNODE:
                //                EvaluateVisitor.blockArgNodeVisitor.execute(this, node);
                //                break;
            case NodeTypes.BLOCKNODE: {
                BlockNode iVisited = (BlockNode) node;
    
                IRubyObject result = runtime.getNil();
                for (Iterator iter = iVisited.iterator(); iter.hasNext();) {
                    result = eval(context, (Node) iter.next(), self);
                }
    
                return result;
            }
            case NodeTypes.BLOCKPASSNODE: {
                BlockPassNode iVisited = (BlockPassNode) node;
                IRubyObject proc = eval(context, iVisited.getBodyNode(), self);
                
    
                if (proc.isNil()) {
                    context.setNoBlock();
                    try {
                        return eval(context, iVisited.getIterNode(), self);
                    } finally {
                        context.clearNoBlock();
                    }
                }
    
                // If not already a proc then we should try and make it one.
                if (!(proc instanceof RubyProc)) {
                    proc = proc.convertToType("Proc", "to_proc", false);
    
                    if (!(proc instanceof RubyProc)) {
                        throw runtime.newTypeError("wrong argument type "
                                + proc.getMetaClass().getName() + " (expected Proc)");
                    }
                }
    
                // TODO: Add safety check for taintedness
    
                Block block = (Block) context.getCurrentBlock();
                if (block != null) {
                    IRubyObject blockObject = block.getBlockObject();
                    // The current block is already associated with the proc.  No need to create new
                    // block for it.  Just eval!
                    if (blockObject != null && blockObject == proc) {
                        try {
                            context.setBlockAvailable();
                            return eval(context, iVisited.getIterNode(), self);
                        } finally {
                            context.clearBlockAvailable();
                        }
                    }
                }
    
                context.preBlockPassEval(((RubyProc) proc).getBlock());
    
                try {
                    return eval(context, iVisited.getIterNode(), self);
                } finally {
                    context.postBlockPassEval();
                }
            }
            case NodeTypes.BREAKNODE: {
                BreakNode iVisited = (BreakNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
    
                JumpException je = new JumpException(JumpException.JumpType.BreakJump);
    
                je.setPrimaryData(result);
                je.setSecondaryData(node);
    
                throw je;
            }
            case NodeTypes.CALLNODE: {
                CallNode iVisited = (CallNode) node;
    
                context.beginCallArgs();
                IRubyObject receiver = null;
                IRubyObject[] args = null;
                try {
                    receiver = eval(context, iVisited.getReceiverNode(), self);
                    args = setupArgs(context, iVisited.getArgsNode(), self);
                } finally {
                    context.endCallArgs();
                }
                
                assert receiver.getMetaClass() != null : receiver.getClass().getName();
                // If reciever is self then we do the call the same way as vcall
                CallType callType = (receiver == self ? CallType.VARIABLE : CallType.NORMAL);
    
                return receiver.callMethod(iVisited.getName(), args, callType);
            }
            case NodeTypes.CASENODE: {
                CaseNode iVisited = (CaseNode) node;
                IRubyObject expression = null;
                if (iVisited.getCaseNode() != null) {
                    expression = eval(context, iVisited.getCaseNode(), self);
                }
    
                context.pollThreadEvents();
    
                IRubyObject result = runtime.getNil();
    
                Node firstWhenNode = iVisited.getFirstWhenNode();
                while (firstWhenNode != null) {
                    if (!(firstWhenNode instanceof WhenNode)) {
                        return eval(context, firstWhenNode, self);
                    }
    
                    WhenNode whenNode = (WhenNode) firstWhenNode;
    
                    if (whenNode.getExpressionNodes() instanceof ArrayNode) {
                        for (Iterator iter = ((ArrayNode) whenNode.getExpressionNodes()).iterator(); iter
                                .hasNext();) {
                            Node tag = (Node) iter.next();
    
                            context.setPosition(tag.getPosition());
                            if (isTrace(runtime)) {
                                callTraceFunction(context, "line", self);
                            }
    
                            // Ruby grammar has nested whens in a case body because of
                            // productions case_body and when_args.
                            if (tag instanceof WhenNode) {
                                RubyArray expressions = (RubyArray) eval(context, ((WhenNode) tag)
                                                .getExpressionNodes(), self);
    
                                for (int j = 0; j < expressions.getLength(); j++) {
                                    IRubyObject condition = expressions.entry(j);
    
                                    if ((expression != null && condition.callMethod("===", expression)
                                            .isTrue())
                                            || (expression == null && condition.isTrue())) {
                                        return eval(context, ((WhenNode) firstWhenNode).getBodyNode(), self);
                                    }
                                }
                                continue;
                            }
    
                            result = eval(context, tag, self);
    
                            if ((expression != null && result.callMethod("===", expression).isTrue())
                                    || (expression == null && result.isTrue())) {
                                return eval(context, whenNode.getBodyNode(), self);
                            }
                        }
                    } else {
                        result = eval(context, whenNode.getExpressionNodes(), self);
    
                        if ((expression != null && result.callMethod("===", expression).isTrue())
                                || (expression == null && result.isTrue())) {
                            return eval(context, ((WhenNode) firstWhenNode).getBodyNode(), self);
                        }
                    }
    
                    context.pollThreadEvents();
    
                    firstWhenNode = whenNode.getNextCase();
                }
    
                return runtime.getNil();
            }
            case NodeTypes.CLASSNODE: {
                ClassNode iVisited = (ClassNode) node;
                RubyClass superClass = getSuperClassFromNode(context, iVisited.getSuperNode(), self);
                Node classNameNode = iVisited.getCPath();
                String name = ((INameNode) classNameNode).getName();
                RubyModule enclosingClass = getEnclosingModule(context, classNameNode, self);
                RubyClass rubyClass = enclosingClass.defineOrGetClassUnder(name, superClass);
    
                if (context.getWrapper() != null) {
                    rubyClass.extendObject(context.getWrapper());
                    rubyClass.includeModule(context.getWrapper());
                }
                return evalClassDefinitionBody(context, iVisited.getBodyNode(), rubyClass, self);
            }
            case NodeTypes.CLASSVARASGNNODE: {
                ClassVarAsgnNode iVisited = (ClassVarAsgnNode) node;
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                RubyModule rubyClass = (RubyModule) context.peekCRef().getValue();
    
                if (rubyClass == null) {
                    rubyClass = self.getMetaClass();
                } else if (rubyClass.isSingleton()) {
                    rubyClass = (RubyModule) rubyClass.getInstanceVariable("__attached__");
                }
    
                rubyClass.setClassVar(iVisited.getName(), result);
    
                return result;
            }
            case NodeTypes.CLASSVARDECLNODE: {
    
                ClassVarDeclNode iVisited = (ClassVarDeclNode) node;
    
                // FIXME: shouldn't we use cref here?
                if (context.getRubyClass() == null) {
                    throw runtime.newTypeError("no class/module to define class variable");
                }
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                ((RubyModule) context.peekCRef().getValue()).setClassVar(iVisited.getName(),
                        result);
    
                return runtime.getNil();
            }
            case NodeTypes.CLASSVARNODE: {
                ClassVarNode iVisited = (ClassVarNode) node;
                RubyModule rubyClass = context.getRubyClass();
    
                if (rubyClass == null) {
                    return self.getMetaClass().getClassVar(iVisited.getName());
                } else if (!rubyClass.isSingleton()) {
                    return rubyClass.getClassVar(iVisited.getName());
                } else {
                    RubyModule module = (RubyModule) rubyClass.getInstanceVariable("__attached__");
    
                    if (module != null) {
                        return module.getClassVar(iVisited.getName());
                    }
                }
                break;
            }
            case NodeTypes.COLON2NODE: {
                Colon2Node iVisited = (Colon2Node) node;
                Node leftNode = iVisited.getLeftNode();
    
                // TODO: Made this more colon3 friendly because of cpath production
                // rule in grammar (it is convenient to think of them as the same thing
                // at a grammar level even though evaluation is).
                if (leftNode == null) {
                    return runtime.getObject().getConstantFrom(iVisited.getName());
                } else {
                    IRubyObject result = eval(context, iVisited.getLeftNode(), self);
                    if (result instanceof RubyModule) {
                        return ((RubyModule) result).getConstantFrom(iVisited.getName());
                    } else {
                        return result.callMethod(iVisited.getName());
                    }
                }
            }
            case NodeTypes.COLON3NODE: {
                Colon3Node iVisited = (Colon3Node) node;
                return runtime.getObject().getConstantFrom(iVisited.getName());
            }
            case NodeTypes.CONSTDECLNODE: {
                ConstDeclNode iVisited = (ConstDeclNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                IRubyObject module;
    
                if (iVisited.getPathNode() != null) {
                    module = eval(context, iVisited.getPathNode(), self);
                } else {
                    
    
                    // FIXME: why do we check RubyClass and then use CRef?
                    if (context.getRubyClass() == null) {
                        // TODO: wire into new exception handling mechanism
                        throw runtime.newTypeError("no class/module to define constant");
                    }
                    module = (RubyModule) context.peekCRef().getValue();
                }
    
                // FIXME: shouldn't we use the result of this set in setResult?
                ((RubyModule) module).setConstant(iVisited.getName(), result);
    
                return result;
            }
            case NodeTypes.CONSTNODE: {
                ConstNode iVisited = (ConstNode) node;
                return context.getConstant(iVisited.getName());
            }
            case NodeTypes.DASGNNODE: {
                DAsgnNode iVisited = (DAsgnNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                context.getCurrentDynamicVars().set(iVisited.getName(), result);
    
                return result;
            }
            case NodeTypes.DEFINEDNODE: {
                DefinedNode iVisited = (DefinedNode) node;
                String def = new DefinedVisitor(runtime).getDefinition(iVisited.getExpressionNode());
                if (def != null) {
                    return runtime.newString(def);
                } else {
                    return runtime.getNil();
                }
            }
            case NodeTypes.DEFNNODE: {
                DefnNode iVisited = (DefnNode) node;
                
                RubyModule containingClass = context.getRubyClass();
    
                if (containingClass == null) {
                    throw runtime.newTypeError("No class to add method.");
                }
    
                String name = iVisited.getName();
                if (containingClass == runtime.getObject() && name.equals("initialize")) {
                    runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
                }
    
                Visibility visibility = context.getCurrentVisibility();
                if (name.equals("initialize") || visibility.isModuleFunction()) {
                    visibility = Visibility.PRIVATE;
                }
    
                DefaultMethod newMethod = new DefaultMethod(containingClass, iVisited.getBodyNode(),
                        (ArgsNode) iVisited.getArgsNode(), visibility, context.peekCRef());
    
                iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));
    
                containingClass.addMethod(name, newMethod);
    
                if (context.getCurrentVisibility().isModuleFunction()) {
                    containingClass.getSingletonClass().addMethod(
                            name,
                            new WrapperCallable(containingClass.getSingletonClass(), newMethod,
                                    Visibility.PUBLIC));
                    containingClass.callMethod("singleton_method_added", runtime.newSymbol(name));
                }
    
                // 'class << state.self' and 'class << obj' uses defn as opposed to defs
                if (containingClass.isSingleton()) {
                    ((MetaClass) containingClass).getAttachedObject().callMethod(
                            "singleton_method_added", runtime.newSymbol(iVisited.getName()));
                } else {
                    containingClass.callMethod("method_added", runtime.newSymbol(name));
                }
    
                return runtime.getNil();
            }
            case NodeTypes.DEFSNODE: {
                DefsNode iVisited = (DefsNode) node;
                IRubyObject receiver = eval(context, iVisited.getReceiverNode(), self);
    
                if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                    throw runtime.newSecurityError("Insecure; can't define singleton method.");
                }
                if (receiver.isFrozen()) {
                    throw runtime.newFrozenError("object");
                }
                if (!receiver.singletonMethodsAllowed()) {
                    throw runtime.newTypeError("can't define singleton method \"" + iVisited.getName()
                            + "\" for " + receiver.getType());
                }
    
                RubyClass rubyClass = receiver.getSingletonClass();
    
                if (runtime.getSafeLevel() >= 4) {
                    ICallable method = (ICallable) rubyClass.getMethods().get(iVisited.getName());
                    if (method != null) {
                        throw runtime.newSecurityError("Redefining method prohibited.");
                    }
                }
    
                DefaultMethod newMethod = new DefaultMethod(rubyClass, iVisited.getBodyNode(),
                        (ArgsNode) iVisited.getArgsNode(), Visibility.PUBLIC, context
                                .peekCRef());
    
                iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));
    
                rubyClass.addMethod(iVisited.getName(), newMethod);
                receiver.callMethod("singleton_method_added", runtime.newSymbol(iVisited.getName()));
    
                return runtime.getNil();
            }
            case NodeTypes.DOTNODE: {
                DotNode iVisited = (DotNode) node;
                return RubyRange.newRange(runtime, eval(context, iVisited.getBeginNode(), self), eval(context, iVisited
                                .getEndNode(), self), iVisited.isExclusive());
            }
            case NodeTypes.DREGEXPNODE: {
                DRegexpNode iVisited = (DRegexpNode) node;
    
                StringBuffer sb = new StringBuffer();
                for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                    Node iterNode = (Node) iterator.next();
    
                    sb.append(eval(context, iterNode, self).toString());
                }
    
                return RubyRegexp.newRegexp(runtime, sb.toString(), iVisited.getOptions(), null);
            }
            case NodeTypes.DSTRNODE: {
                DStrNode iVisited = (DStrNode) node;
    
                StringBuffer sb = new StringBuffer();
                for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                    Node iterNode = (Node) iterator.next();
    
                    sb.append(eval(context, iterNode, self).toString());
                }
    
                return runtime.newString(sb.toString());
            }
            case NodeTypes.DSYMBOLNODE: {
                DSymbolNode iVisited = (DSymbolNode) node;
    
                StringBuffer sb = new StringBuffer();
                for (Iterator iterator = iVisited.getNode().iterator(); iterator.hasNext();) {
                    Node iterNode = (Node) iterator.next();
    
                    sb.append(eval(context, iterNode, self).toString());
                }
    
                return runtime.newSymbol(sb.toString());
            }
            case NodeTypes.DVARNODE: {
                DVarNode iVisited = (DVarNode) node;
                return context.getDynamicValue(iVisited.getName());
            }
            case NodeTypes.DXSTRNODE: {
                DXStrNode iVisited = (DXStrNode) node;
    
                StringBuffer sb = new StringBuffer();
                for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                    Node iterNode = (Node) iterator.next();
    
                    sb.append(eval(context, iterNode, self).toString());
                }
    
                return self.callMethod("`", runtime.newString(sb.toString()));
            }
            case NodeTypes.ENSURENODE: {
                EnsureNode iVisited = (EnsureNode) node;
    
                // save entering the try if there's nothing to ensure
                if (iVisited.getEnsureNode() != null) {
                    IRubyObject result = runtime.getNil();
    
                    try {
                        result = eval(context, iVisited.getBodyNode(), self);
                    } finally {
                        eval(context, iVisited.getEnsureNode(), self);
                    }
    
                    return result;
                }
    
                return eval(context, iVisited.getBodyNode(), self);
            }
            case NodeTypes.EVSTRNODE: {
                EvStrNode iVisited = (EvStrNode) node;
    
                return eval(context, iVisited.getBody(), self);
            }
            case NodeTypes.FALSENODE: {
                context.pollThreadEvents();
                return runtime.getFalse();
            }
            case NodeTypes.FCALLNODE: {
                FCallNode iVisited = (FCallNode) node;
                
    
                context.beginCallArgs();
                IRubyObject[] args;
                try {
                    args = setupArgs(context, iVisited.getArgsNode(), self);
                } finally {
                    context.endCallArgs();
                }
    
                return self.callMethod(iVisited.getName(), args, CallType.FUNCTIONAL);
            }
            case NodeTypes.FIXNUMNODE: {
                FixnumNode iVisited = (FixnumNode) node;
                return runtime.newFixnum(iVisited.getValue());
            }
            case NodeTypes.FLIPNODE: {
                FlipNode iVisited = (FlipNode) node;
                IRubyObject result = runtime.getNil();
    
                if (iVisited.isExclusive()) {
                    if (!context.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                        result = eval(context, iVisited.getBeginNode(), self).isTrue() ? runtime.getFalse()
                                : runtime.getTrue();
                        context.getFrameScope().setValue(iVisited.getCount(), result);
                        return result;
                    } else {
                        if (eval(context, iVisited.getEndNode(), self).isTrue()) {
                            context.getFrameScope().setValue(iVisited.getCount(), runtime.getFalse());
                        }
                        return runtime.getTrue();
                    }
                } else {
                    if (!context.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                        if (eval(context, iVisited.getBeginNode(), self).isTrue()) {
                            context.getFrameScope().setValue(
                                    iVisited.getCount(),
                                    eval(context, iVisited.getEndNode(), self).isTrue() ? runtime.getFalse()
                                            : runtime.getTrue());
                            return runtime.getTrue();
                        } else {
                            return runtime.getFalse();
                        }
                    } else {
                        if (eval(context, iVisited.getEndNode(), self).isTrue()) {
                            context.getFrameScope().setValue(iVisited.getCount(), runtime.getFalse());
                        }
                        return runtime.getTrue();
                    }
                }
            }
            case NodeTypes.FLOATNODE: {
                FloatNode iVisited = (FloatNode) node;
                return RubyFloat.newFloat(runtime, iVisited.getValue());
            }
            case NodeTypes.FORNODE: {
                ForNode iVisited = (ForNode) node;
                
    
                context.preForLoopEval(Block
                        .createBlock(iVisited.getVarNode(), iVisited.getCallable(), self));
    
                try {
                    while (true) {
                        try {
                            ISourcePosition position = context.getPosition();
                            context.beginCallArgs();
    
                            IRubyObject recv = null;
                            try {
                                recv = eval(context, iVisited.getIterNode(), self);
                            } finally {
                                context.setPosition(position);
                                context.endCallArgs();
                            }
    
                            return recv.callMethod("each", IRubyObject.NULL_ARRAY, CallType.NORMAL);
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.RETRY:
                                // do nothing, allow loop to retry
                                break;
                            default:
                                throw je;
                            }
                        }
                    }
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.BREAK:
                        IRubyObject breakValue = (IRubyObject) je.getPrimaryData();
    
                        return breakValue == null ? runtime.getNil() : breakValue;
                    default:
                        throw je;
                    }
                } finally {
                    context.postForLoopEval();
                }
            }
            case NodeTypes.GLOBALASGNNODE: {
                GlobalAsgnNode iVisited = (GlobalAsgnNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
    
                runtime.getGlobalVariables().set(iVisited.getName(), result);
    
                return result;
            }
            case NodeTypes.GLOBALVARNODE: {
                GlobalVarNode iVisited = (GlobalVarNode) node;
                return runtime.getGlobalVariables().get(iVisited.getName());
            }
            case NodeTypes.HASHNODE: {
                HashNode iVisited = (HashNode) node;
    
                Map hash = null;
                if (iVisited.getListNode() != null) {
                    hash = new HashMap(iVisited.getListNode().size() / 2);
    
                    for (Iterator iterator = iVisited.getListNode().iterator(); iterator.hasNext();) {
                        // insert all nodes in sequence, hash them in the final instruction
                        // KEY
                        IRubyObject key = eval(context, (Node) iterator.next(), self);
                        IRubyObject value = eval(context, (Node) iterator.next(), self);
    
                        hash.put(key, value);
                    }
                }
    
                if (hash == null) {
                    return RubyHash.newHash(runtime);
                }
    
                return RubyHash.newHash(runtime, hash, runtime.getNil());
            }
            case NodeTypes.IFNODE: {
                IfNode iVisited = (IfNode) node;
                IRubyObject result = eval(context, iVisited.getCondition(), self);
    
                if (result.isTrue()) {
                    return eval(context, iVisited.getThenBody(), self);
                } else {
                    return eval(context, iVisited.getElseBody(), self);
                }
            }
            case NodeTypes.INSTASGNNODE: {
                InstAsgnNode iVisited = (InstAsgnNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                self.setInstanceVariable(iVisited.getName(), result);
    
                return result;
            }
            case NodeTypes.INSTVARNODE: {
                InstVarNode iVisited = (InstVarNode) node;
                IRubyObject variable = self.getInstanceVariable(iVisited.getName());
    
                return variable == null ? runtime.getNil() : variable;
            }
                //                case NodeTypes.ISCOPINGNODE:
                //                EvaluateVisitor.iScopingNodeVisitor.execute(this, node);
                //                break;
            case NodeTypes.ITERNODE: {
                IterNode iVisited = (IterNode) node;
                
    
                context.preIterEval(Block.createBlock(iVisited.getVarNode(), iVisited.getCallable(), self));
                try {
                    while (true) {
                        try {
                            context.setBlockAvailable();
                            return eval(context, iVisited.getIterNode(), self);
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.RETRY:
                                // allow loop to retry
                                break;
                            default:
                                throw je;
                            }
                        } finally {
                            context.clearBlockAvailable();
                        }
                    }
                } catch (JumpException je) {
                    switch (je.getJumpType().getTypeId()) {
                    case JumpType.BREAK:
                        IRubyObject breakValue = (IRubyObject) je.getPrimaryData();
    
                        return breakValue == null ? runtime.getNil() : breakValue;
                    default:
                        throw je;
                    }
                } finally {
                    context.postIterEval();
                }
            }
            case NodeTypes.LOCALASGNNODE: {
                LocalAsgnNode iVisited = (LocalAsgnNode) node;
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
                context.getFrameScope().setValue(iVisited.getCount(), result);
                return result;
            }
            case NodeTypes.LOCALVARNODE: {
                LocalVarNode iVisited = (LocalVarNode) node;
                return context.getFrameScope().getValue(iVisited.getCount());
            }
            case NodeTypes.MATCH2NODE: {
                Match2Node iVisited = (Match2Node) node;
                IRubyObject recv = eval(context, iVisited.getReceiverNode(), self);
                IRubyObject value = eval(context, iVisited.getValueNode(), self);
    
                return ((RubyRegexp) recv).match(value);
            }
            case NodeTypes.MATCH3NODE: {
                Match3Node iVisited = (Match3Node) node;
                IRubyObject recv = eval(context, iVisited.getReceiverNode(), self);
                IRubyObject value = eval(context, iVisited.getValueNode(), self);
    
                if (value instanceof RubyString) {
                    return ((RubyRegexp) recv).match(value);
                } else {
                    return value.callMethod("=~", recv);
                }
            }
            case NodeTypes.MATCHNODE: {
                MatchNode iVisited = (MatchNode) node;
                return ((RubyRegexp) eval(context, iVisited.getRegexpNode(), self)).match2();
            }
            case NodeTypes.MODULENODE: {
                ModuleNode iVisited = (ModuleNode) node;
                Node classNameNode = iVisited.getCPath();
                String name = ((INameNode) classNameNode).getName();
                RubyModule enclosingModule = getEnclosingModule(context, classNameNode, self);
    
                if (enclosingModule == null) {
                    throw runtime.newTypeError("no outer class/module");
                }
    
                RubyModule module;
                if (enclosingModule == runtime.getObject()) {
                    module = runtime.getOrCreateModule(name);
                } else {
                    module = enclosingModule.defineModuleUnder(name);
                }
                return evalClassDefinitionBody(context, iVisited.getBodyNode(), module, self);
            }
            case NodeTypes.MULTIPLEASGNNODE: {
                MultipleAsgnNode iVisited = (MultipleAsgnNode) node;
                return AssignmentVisitor.assign(context, self, iVisited, eval(context,
                        iVisited.getValueNode(), self), false);
            }
            case NodeTypes.NEWLINENODE: {
                NewlineNode iVisited = (NewlineNode) node;
    
                // something in here is used to build up ruby stack trace...
                context.setPosition(iVisited.getPosition());
    
                if (isTrace(runtime)) {
                    callTraceFunction(context, "line", self);
                }
    
                // TODO: do above but not below for additional newline nodes
                return eval(context, iVisited.getNextNode(), self);
            }
            case NodeTypes.NEXTNODE: {
                NextNode iVisited = (NextNode) node;
    
                context.pollThreadEvents();
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
    
                // now used as an interpreter event
                JumpException je = new JumpException(JumpException.JumpType.NextJump);
    
                je.setPrimaryData(result);
                je.setSecondaryData(iVisited);
    
                //state.setCurrentException(je);
                throw je;
            }
            case NodeTypes.NILNODE:
                return runtime.getNil();
            case NodeTypes.NOTNODE: {
                NotNode iVisited = (NotNode) node;
    
                IRubyObject result = eval(context, iVisited.getConditionNode(), self);
                return result.isTrue() ? runtime.getFalse() : runtime.getTrue();
            }
            case NodeTypes.NTHREFNODE: {
                NthRefNode iVisited = (NthRefNode) node;
                return RubyRegexp.nth_match(iVisited.getMatchNumber(), context.getBackref());
            }
            case NodeTypes.OPASGNANDNODE: {
                BinaryOperatorNode iVisited = (BinaryOperatorNode) node;
    
                // add in reverse order
                IRubyObject result = eval(context, iVisited.getFirstNode(), self);
                if (!result.isTrue()) return result;
                return eval(context, iVisited.getSecondNode(), self);
            }
            case NodeTypes.OPASGNNODE: {
                OpAsgnNode iVisited = (OpAsgnNode) node;
                IRubyObject receiver = eval(context, iVisited.getReceiverNode(), self);
                IRubyObject value = receiver.callMethod(iVisited.getVariableName());
    
                if (iVisited.getOperatorName().equals("||")) {
                    if (value.isTrue()) {
                        return value;
                    }
                    value = eval(context, iVisited.getValueNode(), self);
                } else if (iVisited.getOperatorName().equals("&&")) {
                    if (!value.isTrue()) {
                        return value;
                    }
                    value = eval(context, iVisited.getValueNode(), self);
                } else {
                    value = value.callMethod(iVisited.getOperatorName(), eval(context,
                            iVisited.getValueNode(), self));
                }
    
                receiver.callMethod(iVisited.getVariableName() + "=", value);
    
                context.pollThreadEvents();
    
                return value;
            }
            case NodeTypes.OPASGNORNODE: {
                OpAsgnOrNode iVisited = (OpAsgnOrNode) node;
                String def = new DefinedVisitor(runtime).getDefinition(iVisited.getFirstNode());
    
                IRubyObject result = runtime.getNil();
                if (def != null) {
                    result = eval(context, iVisited.getFirstNode(), self);
                }
                if (!result.isTrue()) {
                    result = eval(context, iVisited.getSecondNode(), self);
                }
    
                return result;
            }
            case NodeTypes.OPELEMENTASGNNODE: {
                OpElementAsgnNode iVisited = (OpElementAsgnNode) node;
                IRubyObject receiver = eval(context, iVisited.getReceiverNode(), self);
    
                IRubyObject[] args = setupArgs(context, iVisited.getArgsNode(), self);
    
                IRubyObject firstValue = receiver.callMethod("[]", args);
    
                if (iVisited.getOperatorName().equals("||")) {
                    if (firstValue.isTrue()) {
                        return firstValue;
                    }
                    firstValue = eval(context, iVisited.getValueNode(), self);
                } else if (iVisited.getOperatorName().equals("&&")) {
                    if (!firstValue.isTrue()) {
                        return firstValue;
                    }
                    firstValue = eval(context, iVisited.getValueNode(), self);
                } else {
                    firstValue = firstValue.callMethod(iVisited.getOperatorName(), eval(context, iVisited
                                    .getValueNode(), self));
                }
    
                IRubyObject[] expandedArgs = new IRubyObject[args.length + 1];
                System.arraycopy(args, 0, expandedArgs, 0, args.length);
                expandedArgs[expandedArgs.length - 1] = firstValue;
                return receiver.callMethod("[]=", expandedArgs);
            }
            case NodeTypes.OPTNNODE: {
                OptNNode iVisited = (OptNNode) node;
    
                IRubyObject result = runtime.getNil();
                while (RubyKernel.gets(runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            result = eval(context, iVisited.getBodyNode(), self);
                            break;
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                // do nothing, this iteration restarts
                                break;
                            case JumpType.NEXT:
                                // recheck condition
                                break loop;
                            case JumpType.BREAK:
                                // end loop
                                return (IRubyObject) je.getPrimaryData();
                            default:
                                throw je;
                            }
                        }
                    }
                }
                return result;
            }
            case NodeTypes.ORNODE: {
                OrNode iVisited = (OrNode) node;
    
                IRubyObject result = eval(context, iVisited.getFirstNode(), self);
    
                if (!result.isTrue()) {
                    result = eval(context, iVisited.getSecondNode(), self);
                }
    
                return result;
            }
                //                case NodeTypes.POSTEXENODE:
                //                EvaluateVisitor.postExeNodeVisitor.execute(this, node);
                //                break;
            case NodeTypes.REDONODE: {
                context.pollThreadEvents();
    
                // now used as an interpreter event
                JumpException je = new JumpException(JumpException.JumpType.RedoJump);
    
                je.setSecondaryData(node);
    
                throw je;
            }
            case NodeTypes.REGEXPNODE: {
                RegexpNode iVisited = (RegexpNode) node;
    
                // FIXME: don't pass null
                return RubyRegexp.newRegexp(runtime, iVisited.getPattern(), null);
            }
            case NodeTypes.RESCUEBODYNODE: {
                RescueBodyNode iVisited = (RescueBodyNode) node;
                return eval(context, iVisited.getBodyNode(), self);
            }
            case NodeTypes.RESCUENODE: {
                RescueNode iVisited = (RescueNode)node;
                RescuedBlock : while (true) {
                    try {
                        // Execute rescue block
                        IRubyObject result = eval(context, iVisited.getBodyNode(), self);

                        // If no exception is thrown execute else block
                        if (iVisited.getElseNode() != null) {
                            if (iVisited.getRescueNode() == null) {
                                runtime.getWarnings().warn(iVisited.getElseNode().getPosition(), "else without rescue is useless");
                            }
                            result = eval(context, iVisited.getElseNode(), self);
                        }

                        return result;
                    } catch (RaiseException raiseJump) {
                        RubyException raisedException = raiseJump.getException();
                        // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                        // falsely set $! to nil and this sets it back to something valid.  This should 
                        // get fixed at the same time we address bug #1296484.
                        runtime.getGlobalVariables().set("$!", raisedException);

                        RescueBodyNode rescueNode = iVisited.getRescueNode();

                        while (rescueNode != null) {
                            Node  exceptionNodes = rescueNode.getExceptionNodes();
                            ListNode exceptionNodesList;
                            
                            if (exceptionNodes instanceof SplatNode) {                    
                                exceptionNodesList = (ListNode) eval(context, exceptionNodes, self);
                            } else {
                                exceptionNodesList = (ListNode) exceptionNodes;
                            }
                            
                            if (isRescueHandled(context, raisedException, exceptionNodesList, self)) {
                                try {
                                    return eval(context, rescueNode, self);
                                } catch (JumpException je) {
                                    if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                                        // should be handled in the finally block below
                                        //state.runtime.getGlobalVariables().set("$!", state.runtime.getNil());
                                        //state.threadContext.setRaisedException(null);
                                        continue RescuedBlock;
                                        
                                    } else {
                                        throw je;
                                    }
                                }
                            }
                            
                            rescueNode = rescueNode.getOptRescueNode();
                        }

                        // no takers; bubble up
                        throw raiseJump;
                    } finally {
                        // clear exception when handled or retried
                        runtime.getGlobalVariables().set("$!", runtime.getNil());
                    }
                }
            }
            case NodeTypes.RETRYNODE: {
                context.pollThreadEvents();
    
                JumpException je = new JumpException(JumpException.JumpType.RetryJump);
    
                throw je;
            }
            case NodeTypes.RETURNNODE: {
                ReturnNode iVisited = (ReturnNode) node;
    
                IRubyObject result = eval(context, iVisited.getValueNode(), self);
    
                JumpException je = new JumpException(JumpException.JumpType.ReturnJump);
    
                je.setPrimaryData(iVisited.getTarget());
                je.setSecondaryData(result);
                je.setTertiaryData(iVisited);
    
                throw je;
            }
            case NodeTypes.SCLASSNODE: {
                SClassNode iVisited = (SClassNode) node;
                IRubyObject receiver = eval(context, iVisited.getReceiverNode(), self);
    
                RubyClass singletonClass;
    
                if (receiver.isNil()) {
                    singletonClass = runtime.getNilClass();
                } else if (receiver == runtime.getTrue()) {
                    singletonClass = runtime.getClass("True");
                } else if (receiver == runtime.getFalse()) {
                    singletonClass = runtime.getClass("False");
                } else {
                    if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                        throw runtime.newSecurityError("Insecure: can't extend object.");
                    }
    
                    singletonClass = receiver.getSingletonClass();
                }
    
                
    
                if (context.getWrapper() != null) {
                    singletonClass.extendObject(context.getWrapper());
                    singletonClass.includeModule(context.getWrapper());
                }
    
                return evalClassDefinitionBody(context, iVisited.getBodyNode(), singletonClass, self);
            }
            case NodeTypes.SCOPENODE: {
                ScopeNode iVisited = (ScopeNode) node;
                
    
                context.preScopedBody(iVisited.getLocalNames());
                try {
                    return eval(context, iVisited.getBodyNode(), self);
                } finally {
                    context.postScopedBody();
                }
            }
            case NodeTypes.SELFNODE:
                return self;
            case NodeTypes.SPLATNODE: {
                SplatNode iVisited = (SplatNode) node;
                return splatValue(eval(context, iVisited.getValue(), self));
            }
                ////                case NodeTypes.STARNODE:
                ////                EvaluateVisitor.starNodeVisitor.execute(this, node);
                ////                break;
            case NodeTypes.STRNODE: {
                StrNode iVisited = (StrNode) node;
                return runtime.newString(iVisited.getValue());
            }
            case NodeTypes.SUPERNODE: {
                SuperNode iVisited = (SuperNode) node;
                
    
                if (context.getFrameLastClass() == null) {
                    throw runtime.newNameError("Superclass method '" + context.getFrameLastFunc()
                            + "' disabled.");
                }
    
                context.beginCallArgs();
    
                IRubyObject[] args = null;
                try {
                    args = setupArgs(context, iVisited.getArgsNode(), self);
                } finally {
                    context.endCallArgs();
                }
                return context.callSuper(args);
            }
            case NodeTypes.SVALUENODE: {
                SValueNode iVisited = (SValueNode) node;
                return aValueSplat(eval(context, iVisited.getValue(), self));
            }
            case NodeTypes.SYMBOLNODE: {
                SymbolNode iVisited = (SymbolNode) node;
                return runtime.newSymbol(iVisited.getName());
            }
            case NodeTypes.TOARYNODE: {
                ToAryNode iVisited = (ToAryNode) node;
                return aryToAry(eval(context, iVisited.getValue(), self));
            }
            case NodeTypes.TRUENODE: {
                context.pollThreadEvents();
                return runtime.getTrue();
            }
            case NodeTypes.UNDEFNODE: {
                UndefNode iVisited = (UndefNode) node;
                
    
                if (context.getRubyClass() == null) {
                    throw runtime
                            .newTypeError("No class to undef method '" + iVisited.getName() + "'.");
                }
                context.getRubyClass().undef(iVisited.getName());
    
                return runtime.getNil();
            }
            case NodeTypes.UNTILNODE: {
                UntilNode iVisited = (UntilNode) node;
    
                IRubyObject result = runtime.getNil();
                
                while (!(result = eval(context, iVisited.getConditionNode(), self)).isTrue()) {
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            result = eval(context, iVisited.getBodyNode(), self);
                            break loop;
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                continue;
                            case JumpType.NEXT:
                                break loop;
                            case JumpType.BREAK:
                                return (IRubyObject) je.getPrimaryData();
                            default:
                                throw je;
                            }
                        }
                    }
                }
                
                return result;
            }
            case NodeTypes.VALIASNODE: {
                VAliasNode iVisited = (VAliasNode) node;
                runtime.getGlobalVariables().alias(iVisited.getNewName(), iVisited.getOldName());
    
                return runtime.getNil();
            }
            case NodeTypes.VCALLNODE: {
                VCallNode iVisited = (VCallNode) node;
                return self.callMethod(iVisited.getMethodName(), IRubyObject.NULL_ARRAY,
                        CallType.VARIABLE);
            }
            case NodeTypes.WHENNODE:
                assert false;
                return null;
            case NodeTypes.WHILENODE: {
                WhileNode iVisited = (WhileNode) node;
    
                IRubyObject result = runtime.getNil();
                boolean firstTest = iVisited.evaluateAtStart();
                
                while (!firstTest || (result = eval(context, iVisited.getConditionNode(), self)).isTrue()) {
                    firstTest = true;
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            eval(context, iVisited.getBodyNode(), self);
                            break loop;
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                continue;
                            case JumpType.NEXT:
                                break loop;
                            case JumpType.BREAK:
                                return result;
                            default:
                                throw je;
                            }
                        }
                    }
                }
                
                return result;
            }
            case NodeTypes.XSTRNODE: {
                XStrNode iVisited = (XStrNode) node;
                return self.callMethod("`", runtime.newString(iVisited.getValue()));
            }
            case NodeTypes.YIELDNODE: {
                YieldNode iVisited = (YieldNode) node;
    
                IRubyObject result = eval(context, iVisited.getArgsNode(), self);
                if (iVisited.getArgsNode() == null) {
                    result = null;
                }
    
                return  context.yieldCurrentBlock(result, null, null,
                        iVisited.getCheckState());
                
            }
            case NodeTypes.ZARRAYNODE: {
                return runtime.newArray();
            }
            case NodeTypes.ZSUPERNODE: {
                
    
                if (context.getFrameLastClass() == null) {
                    throw runtime.newNameError("superclass method '" + context.getFrameLastFunc()
                            + "' disabled");
                }
    
                return context.callSuper(context.getFrameArgs());
            }
            }
        } catch (StackOverflowError sfe) {
            throw runtime.newSystemStackError("stack level too deep");
        }

        return runtime.getNil();
    }

    private static IRubyObject aryToAry(IRubyObject value) {
        if (value instanceof RubyArray) {
            return value;
        }

        if (value.respondsTo("to_ary")) {
            return value.convertToType("Array", "to_ary", false);
        }

        return value.getRuntime().newArray(value);
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private static IRubyObject evalClassDefinitionBody(ThreadContext context, ScopeNode iVisited, RubyModule type,
            IRubyObject self) {
        IRuby runtime = context.getRuntime();
        context.preClassEval(iVisited.getLocalNames(), type);

        try {
            if (isTrace(runtime)) {
                callTraceFunction(context, "class", type);
            }

            return eval(context, iVisited.getBodyNode(), type);
        } finally {
            context.postClassEval();

            if (isTrace(runtime)) {
                callTraceFunction(context, "end", null);
            }
        }
    }

    private static RubyClass getSuperClassFromNode(ThreadContext context, Node superNode, IRubyObject self) {
        if (superNode == null) {
            return null;
        }
        RubyClass superClazz;
        IRuby runtime = context.getRuntime();
        try {
            superClazz = (RubyClass) eval(context, superNode, self);
        } catch (Exception e) {
            if (superNode instanceof INameNode) {
                String name = ((INameNode) superNode).getName();
                throw runtime.newTypeError("undefined superclass '" + name + "'");
            }
            throw runtime.newTypeError("superclass undefined");
        }
        if (superClazz instanceof MetaClass) {
            throw runtime.newTypeError("can't make subclass of virtual class");
        }
        return superClazz;
    }

    /**
     * Helper method.
     *
     * test if a trace function is avaiable.
     *
     */
    private static boolean isTrace(IRuby runtime) {
        return runtime.getTraceFunction() != null;
    }

    private static void callTraceFunction(ThreadContext context, String event, IRubyObject zelf) {
        IRuby runtime = context.getRuntime();
        String name = context.getFrameLastFunc();
        RubyModule type = context.getFrameLastClass();
        runtime.callTraceFunction(event, context.getPosition(), zelf, name, type);
    }

    private static IRubyObject splatValue(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().newArray(value);
        }

        return arrayValue(value);
    }

    private static IRubyObject aValueSplat(IRubyObject value) {
        IRuby runtime = value.getRuntime();
        if (!(value instanceof RubyArray) || ((RubyArray) value).length().getLongValue() == 0) {
            return runtime.getNil();
        }

        RubyArray array = (RubyArray) value;

        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    private static RubyArray arrayValue(IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            IRuby runtime = value.getRuntime();
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can 
            // remove this hack too.
            if (value.getType().searchMethod("to_a").getImplementationClass() != runtime
                    .getKernel()) {
                newValue = value.convertToType("Array", "to_a", false);
                if (newValue.getType() != runtime.getClass("Array")) {
                    throw runtime.newTypeError("`to_a' did not return Array");
                }
            } else {
                newValue = runtime.newArray(value);
            }
        }

        return (RubyArray) newValue;
    }

    private static IRubyObject[] setupArgs(ThreadContext context, Node node,
            IRubyObject self) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
            ArrayNode argsArrayNode = (ArrayNode)node;
            ISourcePosition position = context.getPosition();
            int size = argsArrayNode.size();
            IRubyObject[] argsArray = new IRubyObject[size];
            // avoid using ArrayList unless we absolutely have to
            List argsList = null;
            // once we find a splat, stuff remaining args in argsList and combine afterwards
            boolean hasSplat = false;
            // index for the beginning of splatted args, used for combination later
            int splatBegins = 0;

            for (int i = 0; i < size; i++) {
                Node next = argsArrayNode.get(i);
                
                if (hasSplat) {
                    // once we've found a splat, we switch to an arraylist to handle growing
                    if (next instanceof SplatNode) {
                        argsList.addAll(((RubyArray) eval(context, next, self)).getList());
                    } else {
                        argsList.add(eval(context, next, self));
                    }
                } else {
                    if (next instanceof SplatNode) {
                        // switch to ArrayList, since we've got splatted args in the list
                        argsList = new ArrayList();
                        splatBegins = i;
                        hasSplat = true;
                        argsList.addAll(((RubyArray) eval(context, next, self)).getList());
                    } else {
                        argsArray[i] = eval(context, next, self);
                    }
                }
            }
            
            if (hasSplat) {
                // we had splatted arguments, combine unsplatted with list
                IRubyObject[] argsArray2 = (IRubyObject[])argsList.toArray(new IRubyObject[argsList.size()]);
                IRubyObject[] newArgsArray = new IRubyObject[splatBegins + argsArray2.length];
                System.arraycopy(argsArray, 0, newArgsArray, 0, splatBegins);
                System.arraycopy(argsArray2, 0, newArgsArray, splatBegins, argsArray2.length);
                
                argsArray = argsArray2;
            }

            context.setPosition(position);

            return argsArray;
        }

        return ArgsUtil.arrayify(eval(context, node, self));
    }

    private static RubyModule getEnclosingModule(ThreadContext context, Node node, IRubyObject self) {
        RubyModule enclosingModule = null;

        if (node instanceof Colon2Node) {
            IRubyObject result = eval(context, ((Colon2Node) node).getLeftNode(), self);

            if (result != null && !result.isNil()) {
                enclosingModule = (RubyModule) result;
            }
        } else if (node instanceof Colon3Node) {
            enclosingModule = context.getRuntime().getObject();
        }

        if (enclosingModule == null) {
            enclosingModule = (RubyModule) context.peekCRef().getValue();
        }

        return enclosingModule;
    }

    private static boolean isRescueHandled(ThreadContext context, RubyException currentException, ListNode exceptionNodes,
            IRubyObject self) {
        IRuby runtime = context.getRuntime();
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getClass("StandardError"));
        }

        context.beginCallArgs();

        IRubyObject[] args = null;
        try {
            args = setupArgs(context, exceptionNodes, self);
        } finally {
            context.endCallArgs();
        }

        for (int i = 0; i < args.length; i++) {
            if (!args[i].isKindOf(runtime.getClass("Module"))) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            if (args[i].callMethod("===", currentException).isTrue()) return true;
        }
        return false;
    }
}