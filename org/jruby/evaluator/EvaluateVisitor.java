/*
 * EvaluateVisitor.java - description
 * Created on 19.02.2002, 18:14:29
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

import org.ablaf.ast.INode;
import org.ablaf.common.*;
import org.ablaf.lexer.LexerFactory;
import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.types.IListNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.common.IErrors;
import org.jruby.exceptions.*;
import org.jruby.internal.runtime.methods.*;
import org.jruby.parser.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.IMethod;

//TODO this visitor often leads to very deep stacks.  If it happens to be a real problem, the trampoline method of tail call elimination could be used.
/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class EvaluateVisitor implements NodeVisitor {
    private Ruby ruby;
    private Builtins builtins;

    private IErrorHandler errorHandler;

    private RubyObject self;
    private RubyObject result;

    private ISourcePosition _curPos;

    public EvaluateVisitor(Ruby ruby, RubyObject self) {
        this.ruby = ruby;
        this.self = self;

        builtins = new Builtins(ruby);
    }

    public static EvaluateVisitor createVisitor(RubyObject self) {
        Ruby ruby = self.getRuby();
        if (ruby.getRubyTopSelf() == self) {
            EvaluateVisitor result = ruby.getRubyTopSelfEvaluateVisitor();
            result.reset();
            return result;
        } else {
            // FIXME: I don't know the evaluator good enough to rule this case out,
            // but I haven't seen it so far. (Anders)
            return new EvaluateVisitor(ruby, self);
        }
    }

    private void reset() {
        result = null;
        _curPos = null;
    }

    /**
     * Helper method.
     * 
     * test if a trace function is avaiable.
     * 
     */
    private boolean isTrace() {
        return ruby.getRuntime().getTraceFunction() != null;
    }

    private void callTraceFunction(String event, String file, int line, RubyObject self, String name, RubyObject type) {
        ruby.getRuntime().callTraceFunction(event, file, line, self, name, type);
    }

    public final RubyObject eval(final INode node) {
        result = ruby.getNil();
        /*
        try {
         */
        if (node != null) {
        /*
                ISourcePosition position = node.getPosition();
                if (position != null) {
                    _curPos = position;
                }
        */
            node.accept(this);
        }
        /*
        } catch (JumpException lJump) {
            throw lJump;
        } catch (RubyBugException lException) {
            throw lException;
        } catch (RuntimeException e) {
            System.err.println("native exception thrown when evaluating Node " + node + " at position: " + _curPos);
            //	
            e.printStackTrace();
            RubyBugException lBug = new RubyBugException(e.getClass().getName() + ": " + e.getMessage());
            //lBug.initCause(e);
            throw lBug;
        }
        */
        return result;
    }

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        if (ruby.getRubyClass() == null) {
            throw new TypeError(ruby, "no class to make alias");
        }

        ruby.getRubyClass().aliasMethod(iVisited.getNewName(), iVisited.getOldName());
        ruby.getRubyClass().funcall("method_added", builtins.toSymbol(iVisited.getNewName()));
    }

    /**
     * @see NodeVisitor#visitAndNode(AndNode)
     */
    public void visitAndNode(AndNode iVisited) {
        if (eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitArgsNode(ArgsNode)
     */
    public void visitArgsNode(ArgsNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitArrayNode(ArrayNode)
     */
    public final void visitArrayNode(final ArrayNode iVisited) {
        final ArrayList list = new ArrayList(iVisited.size());

        final Iterator iterator = iVisited.iterator();
        final int size = iVisited.size();
        for (int i = 0; i < size; i++) {
            final INode node = (INode) iterator.next();
            if (node instanceof ExpandArrayNode) {
                list.addAll(((RubyArray) eval(node)).getList());
            } else {
                list.add(eval(node));
            }
        }

        result = RubyArray.newArray(ruby, list);
    }

    /**
     * @see NodeVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
        if (ruby.getActFrame().getArgs().length != 1) {
            throw new ArgumentError(ruby, "wrong # of arguments(" + ruby.getActFrame().getArgs().length + "for 1)");
        }

        result = self.setInstanceVar(iVisited.getAttributeName(), (RubyObject) ruby.getActFrame().getArgs()[0]);
    }

    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        switch (iVisited.getType()) {
            case '&' :
                result = RubyRegexp.last_match(ruby.getBackref());
                break;
            case '`' :
                result = RubyRegexp.match_pre(ruby.getBackref());
                break;
            case '\'' :
                result = RubyRegexp.match_post(ruby.getBackref());
                break;
            case '+' :
                result = RubyRegexp.match_last(ruby.getBackref());
                break;
        }
    }

    /**
     * @see NodeVisitor#visitBeginNode(BeginNode)
     */
    public void visitBeginNode(BeginNode iVisited) {
        eval(iVisited.getBodyNode());
    }

    /**
     * @see NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public void visitBlockArgNode(BlockArgNode iVisited) {
        /*if (ruby.getScope().getLocalValues() == null) {
          throw new RuntimeException("BUG: unexpected block argument");
          } else*/
        /*if (ruby.isBlockGiven()) {
        // Create a new Proc object
        result = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
        
        // +++ Seems to be the wrong block by default (this fix seems to help)
        ((RubyProc) result).getBlock().setScope((Scope) ruby.getScope().getTop().getNext());
        // ---
        
        ruby.getScope().setValue(iVisited.getCount(), result);
        }*/
        // XXX See org.jruby.internal.runtime.methods.DefaultMethod.
    }

    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
        Iterator iter = iVisited.iterator();
        while (iter.hasNext()) {
            eval((INode) iter.next());
        }
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
        RubyObject block = eval(iVisited.getBodyNode());

        if (block.isNil()) {
            eval(iVisited.getIterNode());
            return;
        } else if (block instanceof RubyMethod) {
            block = ((RubyMethod)block).to_proc();
        } else if (!(block instanceof RubyProc)) {
            throw new TypeError(ruby, "wrong argument type " + block.getRubyClass().toName() + " (expected Proc)");
        }

        Block oldBlock = ruby.getBlock().getAct();
        ruby.getBlock().push(((RubyProc) block).getBlock());

        ruby.getIterStack().push(Iter.ITER_PRE);
        ruby.getActFrame().setIter(Iter.ITER_PRE);

        try {
            eval(iVisited.getIterNode());
        } finally {
            ruby.getIterStack().pop();
            ruby.getBlock().setAct(oldBlock);
        }
    }

    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
        throw new BreakJump();
    }

    /**
     * @see NodeVisitor#visitCDeclNode(CDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        if (ruby.getRubyClass() == null) {
            throw new TypeError(ruby, "no class/module to define constant");
        }

        ruby.getRubyClass().setConstant(iVisited.getName(), eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitCVAsgnNode(CVAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        ruby.getCBase().setClassVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitCVDeclNode(CVDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        if (ruby.getCBase() == null) {
            throw new TypeError(ruby, "no class/module to define class variable");
        }

        eval(iVisited.getValueNode());

        if (ruby.isVerbose() && ruby.getCBase().isSingleton()) {
            errorHandler.handleError(IErrors.WARN, iVisited.getPosition(), "Declaring singleton class variable.");
        }
        ruby.getCBase().declareClassVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitCVarNode(CVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
        if (ruby.getCBase() == null) {
            result = self.getRubyClass().getClassVar(iVisited.getName());
        } else if (!ruby.getCBase().isSingleton()) {
            result = ruby.getCBase().getClassVar(iVisited.getName());
        } else {
            result = ((RubyModule) ruby.getCBase().getInstanceVar("__attached__")).getClassVar(iVisited.getName());
        }
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        Block tmpBlock = ArgsUtil.beginCallArgs(ruby);

        RubyObject receiver = eval(iVisited.getReceiverNode());
        RubyObject[] args = ArgsUtil.setupArgs(ruby, this, iVisited.getArgsNode());

        ArgsUtil.endCallArgs(ruby, tmpBlock);

        result = receiver.getRubyClass().call(receiver, iVisited.getName(), args, 0);
    }

    /**
     * @see NodeVisitor#visitCaseNode(CaseNode)
     */
    public void visitCaseNode(CaseNode iVisited) {
        if (iVisited.getCaseNode() != null) {
            RubyObject expression = eval(iVisited.getCaseNode());

            Iterator iter = iVisited.getWhenNodes().iterator();
            while (iter.hasNext()) {
                WhenNode whenNode = (WhenNode) iter.next();

                ruby.setSourceLine(whenNode.getPosition().getLine());
                ruby.setSourceFile(whenNode.getPosition().getFile());
                if (isTrace()) {
                    callTraceFunction(
                        "line",
                        ruby.getSourceFile(),
                        ruby.getSourceLine(),
                        self,
                        ruby.getActFrame().getLastFunc(),
                        ruby.getActFrame().getLastClass());
                }

                RubyArray expressions = (RubyArray) eval(whenNode.getExpressionNodes());

                for (int i = 0; i < expressions.getLength(); i++) {
                    if (expressions.entry(i).funcall("===", expression).isTrue()) {
                        eval(whenNode);
                        return;
                    }
                }
            }
        } else {
            Iterator iter = iVisited.getWhenNodes().iterator();
            while (iter.hasNext()) {
                WhenNode whenNode = (WhenNode) iter.next();

                ruby.setSourceLine(whenNode.getPosition().getLine());
                ruby.setSourceFile(whenNode.getPosition().getFile());
                if (isTrace()) {
                    callTraceFunction(
                        "line",
                        ruby.getSourceFile(),
                        ruby.getSourceLine(),
                        self,
                        ruby.getActFrame().getLastFunc(),
                        ruby.getActFrame().getLastClass());
                }

                RubyArray expressions = (RubyArray) eval(whenNode.getExpressionNodes());

                for (int i = 0; i < expressions.getLength(); i++) {
                    if (expressions.entry(i).isTrue()) {
                        eval(whenNode);
                        return;
                    }
                }
            }
        }
        eval(iVisited.getElseNode());
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        if (ruby.getRubyClass() == null) {
            throw new TypeError(ruby, "no outer class/module");
        }

        RubyClass superClass = null;

        if (iVisited.getSuperNode() != null) {
            try {
                superClass = (RubyClass) eval(iVisited.getSuperNode());
            } catch (Exception excptn) {
                if (iVisited.getSuperNode() instanceof Colon2Node) {
                    throw new TypeError(ruby, "undefined superclass '" + ((Colon2Node) iVisited.getSuperNode()).getName() + "'");
                } else if (iVisited.getSuperNode() instanceof ConstNode) {
                    throw new TypeError(ruby, "undefined superclass '" + ((ConstNode) iVisited.getSuperNode()).getName() + "'");
                } else {
                    throw new TypeError(ruby, "undefined superclass");
                }
            }
            if (superClass != null && superClass.isSingleton()) {
                throw new TypeError(ruby, "can't make subclass of virtual class");
            }
        }

        RubyClass rubyClass = null;

        // if ((ruby_class == getRuby().getObjectClass()) && rb_autoload_defined(node.nd_cname())) {
        //     rb_autoload_load(node.nd_cname());
        // }

        if (ruby.getRubyClass().isConstantDefined(iVisited.getClassName())) {
            RubyObject type = ruby.getRubyClass().getConstant(iVisited.getClassName());

            if (!(type instanceof RubyClass)) {
                throw new TypeError(ruby, iVisited.getClassName() + " is not a class");
            } else {
                rubyClass = (RubyClass)type;
            }

            if (superClass != null && rubyClass.getSuperClass().getRealClass() != superClass) {
                // FIXME add defineClassId again.
                rubyClass = ruby.defineClass(iVisited.getClassName(), superClass);

                rubyClass.setClassPath(ruby.getRubyClass(), iVisited.getClassName());
                ruby.getRubyClass().setConstant(iVisited.getClassName(), rubyClass);
            } else {
                if (ruby.getSafeLevel() >= 4) {
                    throw new RubySecurityException(ruby, "extending class prohibited");
                }
                // rb_clear_cache();
            }
        } else {
            if (superClass == null) {
                superClass = ruby.getClasses().getObjectClass();
            }
            // FIXME see above
            rubyClass = ruby.defineClass(iVisited.getClassName(), superClass);
            ruby.getRubyClass().setConstant(iVisited.getClassName(), rubyClass);
            rubyClass.setClassPath(ruby.getRubyClass(), iVisited.getClassName());
        }

        if (ruby.getWrapper() != null) {
            rubyClass.extendObject(ruby.getWrapper());
            rubyClass.includeModule(ruby.getWrapper());
        }

        evalClassDefinitionBody(iVisited.getBodyNode(), rubyClass);
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
        // Java package support hack
        Colon2Node node = iVisited;
        ArrayList packageList = new ArrayList(10);
        while (node.getLeftNode() instanceof Colon2Node) {
            node = (Colon2Node)node.getLeftNode();
            packageList.add(node.getName().toLowerCase());
        }
        if (node.getLeftNode() instanceof ConstNode) {
            StringBuffer packageName = new StringBuffer();
            packageName.append(((ConstNode)node.getLeftNode()).getName().toLowerCase());
            ListIterator iter = packageList.listIterator(packageList.size());
            while (iter.hasPrevious()) {
                packageName.append('.').append(iter.previous());
            }
            packageName.append('.');
            try {
                String javaName = packageName.append(iVisited.getName()).toString();
                Class javaClass = ruby.getJavaSupport().loadJavaClass(javaName);
                result = ruby.getJavaSupport().loadClass(javaClass, null);
                return;
            } catch (NameError excptn) {
            }
            String javaName = packageName.append(iVisited.getName().toLowerCase()).toString();
            if (Package.getPackage(javaName) != null) {
                // create JavaPackage module.
                // return;
            }
        }         
        // Java package support hack end;
        eval(iVisited.getLeftNode());
        if (result instanceof RubyModule) {
            result = ((RubyModule) result).getConstant(iVisited.getName());
        } else {
            result = result.funcall(iVisited.getName());
        }
    }

    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
        result = ruby.getClasses().getObjectClass().getConstant(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        result = ruby.getActFrame().getNamespace().getConstant(self, iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitDAsgnCurrNode(DAsgnCurrNode)
     */
    public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
        eval(iVisited.getValueNode());
        RubyVarmap.assignCurrent(ruby, iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        RubyVarmap.assign(ruby, iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegxNode)
     */
    public void visitDRegxNode(DRegexpNode iVisited) {
        StringBuffer sb = new StringBuffer();

        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            INode node = (INode) iterator.next();
            sb.append(eval(node));
        }

        result = RubyRegexp.newRegexp(ruby, sb.toString(), iVisited.getOptions());
    }

    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public final void visitDStrNode(final DStrNode iVisited) {
        final StringBuffer sb = new StringBuffer();

        final Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            INode node = (INode) iterator.next();
            sb.append(eval(node));
        }

        result = builtins.toString(sb.toString());
    }

    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
        result = ruby.getDynamicVars().getRef(ruby, iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public void visitDXStrNode(DXStrNode iVisited) {
        StringBuffer sb = new StringBuffer();

        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            INode node = (INode) iterator.next();
            sb.append(eval(node));
        }

        result = self.funcall("`", builtins.toString(sb.toString()));
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
        String def = new DefinedVisitor(ruby, self).getDefinition(iVisited.getExpressionNode());
        if (def != null) {
            result = builtins.toString(def);
        }
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     * @fixme Create new method store class.
     */
    public void visitDefnNode(DefnNode iVisited) {
        RubyModule rubyClass = ruby.getRubyClass();
        if (rubyClass == null) {
            throw new TypeError(ruby, "No class to add method.");
        }

        //if (ruby_class == getRuby().getObjectClass() && node.nd_mid() == init) {
        // warn("redefining Object#initialize may cause infinite loop");
        //}
        //if (node.nd_mid() == __id__ || node.nd_mid() == __send__) {
        // warn("redefining `%s' may cause serious problem", ((RubyId)node.nd_mid()).toName());
        //}
        // ruby_class.setFrozen(true);

        IMethod method = rubyClass.searchMethod(iVisited.getName());
        // RubyObject origin = body.getOrigin();

        // if (body != null){
        // if (ruby_verbose.isTrue() && ruby_class == origin && body.nd_cnt() == 0) {
        //     rom.rb_warning("discarding old %s", ((RubyId)node.nd_mid()).toName());
        // }
        // if (node.nd_noex() != 0) { /* toplevel */
        /* should upgrade to rb_warn() if no super was called inside? */
        //     rom.rb_warning("overriding global function `%s'", ((RubyId)node.nd_mid()).toName());
        // }
        //          }

        int noex;

        if (ruby.isScope(Constants.SCOPE_PRIVATE) || iVisited.getName().equals("initialize")) {
            noex = Constants.NOEX_PRIVATE;
        } else if (ruby.isScope(Constants.SCOPE_PROTECTED)) {
            noex = Constants.NOEX_PROTECTED;
        } else if (rubyClass == ruby.getClasses().getObjectClass()) {
            noex = iVisited.getNoex();
        } else {
            noex = Constants.NOEX_PUBLIC;
        }

        if (method != null && method.getImplementationClass() == rubyClass && (method.getNoex() & Constants.NOEX_UNDEF) != 0) {
            noex |= Constants.NOEX_UNDEF;
        }

        // FIXME Create new method store class.
        // ScopeNode body = copyNodeScope((ScopeNode)iVisited.getBodyNode(), ruby.getNamespace());
        DefaultMethod newMethod = new DefaultMethod(iVisited.getBodyNode(), (ArgsNode) iVisited.getArgsNode(), ruby.getNamespace());

        rubyClass.addMethod(iVisited.getName(), newMethod, noex);

        if (ruby.getActMethodScope() == Constants.SCOPE_MODFUNC) {
            rubyClass.getSingletonClass().addMethod(iVisited.getName(), newMethod, Constants.NOEX_PUBLIC);
            rubyClass.funcall("singleton_method_added", builtins.toSymbol(iVisited.getName()));
        }

        if (rubyClass.isSingleton()) {
            rubyClass.getInstanceVar("__attached__").funcall("singleton_method_added", builtins.toSymbol(iVisited.getName()));
        } else {
            rubyClass.funcall("method_added", builtins.toSymbol(iVisited.getName()));
        }
    }

    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     * @fixme see above in visitDefnNode
     */
    public void visitDefsNode(DefsNode iVisited) {
        RubyObject receiver = eval(iVisited.getReceiverNode());

        if (ruby.getSafeLevel() >= 4 && !receiver.isTaint()) {
            throw new RubySecurityException(ruby, "Insecure; can't define singleton method.");
        }
        /*if (FIXNUM_P(recv) || SYMBOL_P(recv)) {
          rb_raise(rb_eTypeError, "can't define singleton method \"%s\" for %s",
          rb_id2name(node.nd_mid()), rb_class2name(CLASS_OF(recv)));
          }*/ // not needed in jruby

        if (receiver.isFrozen()) {
            throw new RubyFrozenException(ruby, "object");
        }
        RubyClass rubyClass = receiver.getSingletonClass();

        IMethod method = (IMethod) rubyClass.getMethods().get(iVisited.getName());
        if (method != null) {
            if (ruby.getSafeLevel() >= 4) {
                throw new RubySecurityException(ruby, "Redefining method prohibited.");
            }
            /*if (RTEST(ruby_verbose)) {
              rb_warning("redefine %s", rb_id2name(node.nd_mid()));
              }*/
        }

        // FIXME see above in visitDefnNode
        DefaultMethod newMethod = new DefaultMethod(iVisited.getBodyNode(), (ArgsNode) iVisited.getArgsNode(), ruby.getNamespace());

        rubyClass.addMethod(iVisited.getName(), newMethod, Constants.NOEX_PUBLIC | (method != null ? method.getNoex() & Constants.NOEX_UNDEF : 0));
        receiver.funcall("singleton_method_added", builtins.toSymbol(iVisited.getName()));

        result = ruby.getNil();
    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
        result = RubyRange.newRange(ruby, eval(iVisited.getBeginNode()), eval(iVisited.getEndNode()), iVisited.isExclusive());
    }

    /**
     * @see NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public void visitEnsureNode(EnsureNode iVisited) {
        try {
            result = eval(iVisited.getBodyNode());
        } finally {
            if (iVisited.getEnsureNode() != null) {
                // XXX
                RubyObject oldResult = result;
                eval(iVisited.getEnsureNode());
                result = oldResult;
            }
        }
    }

    /**
     * @see NodeVisitor#visitEvStrNode(EvStrNode)
     * @fixme Move the variable stuff to a Runtime method
     */
    public final void visitEvStrNode(final EvStrNode iVisited) {
        if (iVisited.getEvaluatedNode() == null) {
            // FIXME Move the variable stuff to a Runtime method

            RubyParserConfiguration config = new RubyParserConfiguration();
            config.setLocalVariables(ruby.getScope().getLocalNames());
            config.setBlockVariables(RubyVarmap.getNames(ruby));
            ruby.getParser().init(config);

            IRubyParserResult result = (IRubyParserResult) ruby.getParser().parse(LexerFactory.getInstance().getSource("#{}", iVisited.getValue()));

            int oldLen = ruby.getScope().getLocalNames() != null ? ruby.getScope().getLocalNames().size() : 0;
            int newLen = result.getLocalVariables() != null ? result.getLocalVariables().size() : 0;
            if (newLen > oldLen) {
                ruby.getScope().setLocalNames(result.getLocalVariables());
                ruby.getScope().getLocalValues().addAll(Collections.nCopies(newLen - oldLen, ruby.getNil()));
            }

            iVisited.setEvaluatedNode(result.getAST());
        }

        eval(iVisited.getEvaluatedNode());
    }

    /**
     * @see NodeVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
        Block tmpBlock = ArgsUtil.beginCallArgs(ruby);
        RubyObject[] args = ArgsUtil.setupArgs(ruby, this, iVisited.getArgsNode());
        ArgsUtil.endCallArgs(ruby, tmpBlock);

        result = self.getRubyClass().call(self, iVisited.getName(), args, 1);
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        result = ruby.getFalse();
    }

    /**
     * @see NodeVisitor#visitFlipNode(FlipNode)
     */
    public void visitFlipNode(FlipNode iVisited) {
        if (iVisited.isExclusive()) {
            if (ruby.getScope().getValue(iVisited.getCount()).isFalse()) {
                //Benoit: I don't understand why the result is inversed
                result = eval(iVisited.getBeginNode()).isTrue() ? ruby.getFalse() : ruby.getTrue();
                ruby.getScope().setValue(iVisited.getCount(), result);
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    ruby.getScope().setValue(iVisited.getCount(), ruby.getFalse());
                }
                result = ruby.getTrue();
            }
        } else {
            if (ruby.getScope().getValue(iVisited.getCount()).isFalse()) {
                if (eval(iVisited.getBeginNode()).isTrue()) {
                    //Benoit: I don't understand why the result is inversed
                    ruby.getScope().setValue(iVisited.getCount(), eval(iVisited.getEndNode()).isTrue() ? ruby.getFalse() : ruby.getTrue());
                    result = ruby.getTrue();
                } else {
                    result = ruby.getFalse();
                }
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    ruby.getScope().setValue(iVisited.getCount(), ruby.getFalse());
                }
                result = ruby.getTrue();
            }
        }
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
        ruby.getBlock().push(iVisited.getVarNode(), new EvaluateMethod(iVisited.getBodyNode()), self);
        ruby.getIterStack().push(Iter.ITER_PRE);

        try {
            while (true) {
                try {
                    String file = ruby.getSourceFile();
                    int line = ruby.getSourceLine();

                    // XXX ruby.getBlock().flags &= ~RubyBlock.BLOCK_D_SCOPE;

                    Block tmpBlock = ArgsUtil.beginCallArgs(ruby);
                    RubyObject recv = eval(iVisited.getIterNode());
                    ArgsUtil.endCallArgs(ruby, tmpBlock);

                    ruby.setSourceFile(file);
                    ruby.setSourceLine(line);
                    result = recv.getRubyClass().call(recv, "each", null, 0);

                    return;
                } catch (RetryException rExcptn) {
                }
            }
        } catch (ReturnException rExcptn) {
            result = rExcptn.getReturnValue();
        } catch (BreakJump bExcptn) {
            result = ruby.getNil();
        } finally {
            ruby.getIterStack().pop();
            ruby.getBlock().pop();
        }
    }

    /**
     * @see NodeVisitor#visitGAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        ruby.setGlobalVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitGVarNode(GVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        result = ruby.getGlobalVar(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitHashNode(HashNode)
     */
    public void visitHashNode(HashNode iVisited) {
        RubyHash hash = RubyHash.newHash(ruby);

        if (iVisited.getListNode() != null) {
            Iterator iterator = iVisited.getListNode().iterator();
            while (iterator.hasNext()) {
                RubyObject key = eval((INode) iterator.next());
                if (iterator.hasNext()) {
                    hash.aset(key, eval((INode) iterator.next()));
                } else {
                    // XXX
                    throw new RuntimeException("[BUG] odd number list for Hash");
                    // XXX
                }
            }
        }
        result = hash;
    }

    /**
     * @see NodeVisitor#visitIAsgnNode(InstVarAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        self.setInstanceVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitIVarNode(IVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        result = self.getInstanceVar(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitIfNode(IfNode)
     */
    public void visitIfNode(IfNode iVisited) {
        if (eval(iVisited.getCondition()).isTrue()) {
            eval(iVisited.getThenBody());
        } else {
            eval(iVisited.getElseBody());
        }
    }

    /**
     * @see NodeVisitor#visitIterNode(IterNode)
     */
    public void visitIterNode(IterNode iVisited) {
        ruby.getBlock().push(iVisited.getVarNode(), new EvaluateMethod(iVisited.getBodyNode()), self);
        ruby.getIterStack().push(Iter.ITER_PRE);
        try {
            while (true) {
                try {
                    result = eval(iVisited.getIterNode());
                    return;
                } catch (RetryException rExcptn) {
                }
            }
        } catch (ReturnException rExcptn) {
            result = rExcptn.getReturnValue();
        } catch (BreakJump bExcptn) {
            result = ruby.getNil();
        } finally {
            ruby.getIterStack().pop();
            ruby.getBlock().pop();
        }
    }

    /**
     * @see NodeVisitor#visitLAsgnNode(LAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        ruby.getScope().setValue(iVisited.getCount(), result);
    }

    /**
     * @see NodeVisitor#visitLVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        result = ruby.getScope().getValue(iVisited.getCount());
    }

    /**
     * @see NodeVisitor#visitMAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        result = new AssignmentVisitor(ruby, self).assign(iVisited, eval(iVisited.getValueNode()), false);
    }

    /**
     * @see NodeVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
        result = ((RubyRegexp) eval(iVisited.getReceiverNode())).match(eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
        RubyObject receiver = eval(iVisited.getReceiverNode());
        RubyObject value = eval(iVisited.getValueNode());
        if (value instanceof RubyString) {
            result = ((RubyRegexp) receiver).match(value);
        } else {
            result = value.funcall("=~", receiver);
        }
    }

    /**
     * @see NodeVisitor#visitMatchNode(MatchNode)
     */
    public void visitMatchNode(MatchNode iVisited) {
        result = ((RubyRegexp) eval(iVisited.getRegexpNode())).match2();
    }

    /**
     * @see NodeVisitor#visitModuleNode(ModuleNode)
     */
    public void visitModuleNode(ModuleNode iVisited) {
        if (ruby.getRubyClass() == null) {
            throw new TypeError(ruby, "no outer class/module");
        }

        if ((ruby.getRubyClass() == ruby.getClasses().getObjectClass()) && ruby.isAutoloadDefined(iVisited.getName())) {
            // getRuby().rb_autoload_load(node.nd_cname());
        }

        RubyModule module = null;

        if (ruby.getRubyClass().isConstantDefined(iVisited.getName())) {
            module = (RubyModule) ruby.getRubyClass().getConstant(iVisited.getName());

            /*if (!(module instanceof RubyModule)) {
              throw new RubyTypeException(moduleName.toName() + " is not a module");
            
              }*/

            if (ruby.getSafeLevel() >= 4) {
                throw new RubySecurityException(ruby, "Extending module prohibited.");
            }
        } else {
            module = ruby.defineModule(iVisited.getName());
            ruby.getRubyClass().setConstant(iVisited.getName(), module);
            module.setClassPath(ruby.getRubyClass(), iVisited.getName());
        }

        if (ruby.getWrapper() != null) {
            module.getSingletonClass().includeModule(ruby.getWrapper());
            module.includeModule(ruby.getWrapper());
        }

        evalClassDefinitionBody(iVisited.getBodyNode(), module);
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        if (iVisited.getPosition() != null) {
            ruby.setSourceFile(iVisited.getPosition().getFile());
            ruby.setSourceLine(iVisited.getPosition().getLine());
            
            if (isTrace()) {
                callTraceFunction(
                    "line",
                    ruby.getSourceFile(),
                    ruby.getSourceLine(),
                    self,
                    ruby.getActFrame().getLastFunc(),
                    ruby.getActFrame().getLastClass());
            }
        }

        eval(iVisited.getNextNode());
    }

    /**
     * @see NodeVisitor#visitNextNode(NextNode)
     */
    public void visitNextNode(NextNode iVisited) {
        throw new NextJump();
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitNotNode(NotNode)
     */
    public void visitNotNode(NotNode iVisited) {
        result = eval(iVisited.getConditionNode()).isTrue() ? ruby.getFalse() : ruby.getTrue();
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
        result = RubyRegexp.nth_match(iVisited.getMatchNumber(), ruby.getBackref());
    }

    /**
     * @see NodeVisitor#visitOpAsgn1Node(OpAsgn1Node)
     */
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        RubyObject receiver = eval(iVisited.getReceiverNode());

        RubyObject[] args = ArgsUtil.setupArgs(ruby, this, iVisited.getArgsNode());

        RubyObject firstValue = receiver.funcall("[]", args);

        if (iVisited.getOperatorName().equals("||")) {
            if (firstValue.isTrue()) {
                result = firstValue;
                return;
            } else {
                firstValue = eval(iVisited.getValueNode());
            }
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (firstValue.isFalse()) {
                result = firstValue;
                return;
            } else {
                firstValue = eval(iVisited.getValueNode());
            }
        } else {
            firstValue = firstValue.funcall(iVisited.getOperatorName(), eval(iVisited.getValueNode()));
        }

        RubyObject[] expandedArgs = new RubyObject[args.length + 1];
        System.arraycopy(args, 0, expandedArgs, 0, args.length);
        expandedArgs[expandedArgs.length - 1] = firstValue;
        result = receiver.funcall("[]=", expandedArgs);
    }

    /**
     * @see NodeVisitor#visitOpAsgn2Node(OpAsgn2Node)
     */
    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        RubyObject receiver = eval(iVisited.getReceiverNode());
        RubyObject value = receiver.funcall(iVisited.getVariableName(), (RubyObject[]) null);

        if (iVisited.getOperatorName().equals("||")) {
            if (value.isTrue()) {
                result = value;
                return;
            } else {
                value = eval(iVisited.getValueNode());
            }
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (value.isFalse()) {
                result = value;
                return;
            } else {
                value = eval(iVisited.getValueNode());
            }
        } else {
            value = value.funcall(iVisited.getOperatorName(), eval(iVisited.getValueNode()));
        }

        receiver.funcall(iVisited.getVariableName() + "=", value);

        result = value;
    }

    /**
     * @see NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        if (eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        if (eval(iVisited.getFirstNode()).isFalse()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitOptNNode(OptNNode)
     */
    public void visitOptNNode(OptNNode iVisited) {
        while (RubyKernel.gets(ruby, ruby.getRubyTopSelf(), new RubyObject[0]).isTrue()) {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        }
    }

    /**
     * @see NodeVisitor#visitOrNode(OrNode)
     */
    public void visitOrNode(OrNode iVisited) {
        if (eval(iVisited.getFirstNode()).isFalse()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public void visitPostExeNode(PostExeNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitRedoNode(RedoNode)
     */
    public void visitRedoNode(RedoNode iVisited) {
        throw new RedoJump();
    }

    /**
     * @see NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        eval(iVisited.getBodyNode());
    }

    /**
     * @see NodeVisitor#visitRescueNode(RescueNode)
     */
    public void visitRescueNode(RescueNode iVisited) {
        RescuedBlock : while (true) {
            try {
                // Execute rescue block
                eval(iVisited.getBodyNode());

                // If no exception is thrown execute else block
                if (iVisited.getElseNode() != null) {
                    eval(iVisited.getElseNode());
                }

                return;
            } catch (RaiseException raiseJump) {
                ruby.setGlobalVar("$!", raiseJump.getActException());

                // ruby.setSourceLine(getLine());

                Iterator iter = iVisited.getRescueNodes().iterator();
                while (iter.hasNext()) {
                    RescueBodyNode rescueNode = (RescueBodyNode) iter.next();
                    if (isRescueHandled(raiseJump.getActException(), rescueNode.getExceptionNodes())) {
                        try {
                            eval(rescueNode);
                            return;
                        } catch (RetryException retryJump) {
                            ruby.setGlobalVar("$!", ruby.getNil());
                            continue RescuedBlock;
                        }
                    }
                }

                throw raiseJump;
            } finally {
                ruby.setGlobalVar("$!", ruby.getNil());
            }
        }
    }

    /**
     * @see NodeVisitor#visitRestArgsNode(RestArgsNode)
     */
    public void visitRestArgsNode(RestArgsNode iVisited) {
        result = builtins.toArray(eval(iVisited.getArgumentNode()));
    }

    /**
     * @see NodeVisitor#visitRetryNode(RetryNode)
     */
    public void visitRetryNode(RetryNode iVisited) {
        throw new RetryException();
    }

    /**
     * @see NodeVisitor#visitReturnNode(ReturnNode)
     */
    public void visitReturnNode(ReturnNode iVisited) {
        throw new ReturnException(eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
        RubyObject receiver = eval(iVisited.getReceiverNode());

        RubyClass singletonClass = null;

        if (receiver.isNil()) {
            singletonClass = ruby.getClasses().getNilClass();
        } else if (receiver == ruby.getTrue()) {
            singletonClass = ruby.getClasses().getTrueClass();
        } else if (receiver == ruby.getFalse()) {
            singletonClass = ruby.getClasses().getFalseClass();
        } else {
            if (ruby.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw new RubySecurityException(ruby, "Insecure: can't extend object.");
            }

            if (receiver.getRubyClass().isSingleton()) {
                ruby.getMethodCache().clear();
            }

            singletonClass = receiver.getSingletonClass();
        }

        if (ruby.getWrapper() != null) {
            singletonClass.extendObject(ruby.getWrapper());
            singletonClass.includeModule(ruby.getWrapper());
        }

        evalClassDefinitionBody(iVisited.getBodyNode(), singletonClass);
    }

    /**
     * @see NodeVisitor#visitScopeNode(ScopeNode)
     */
    public void visitScopeNode(ScopeNode iVisited) {
        ruby.getActFrame().tmpPush();
        ruby.getScope().push(iVisited.getLocalNames());

        Namespace savedNamespace = null;
        /*if (iVisited.getNamespace() != null) {
          savedNamespace = ruby.getNamespace();
          ruby.setNamespace(iVisited.getNamespace());
          ruby.getRubyFrame().setNamespace(iVisited.getNamespace());
          }*/

        try {
            eval(iVisited.getBodyNode());
        } finally {
            if (savedNamespace != null) {
                ruby.setNamespace(savedNamespace);
            }

            ruby.getScope().pop();
            ruby.getActFrame().tmpPop();
        }
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        result = self;
    }

    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
        result = builtins.toString(iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
        if (ruby.getActFrame().getLastClass() == null) {
            throw new NameError(ruby, "Superclass method '" + ruby.getActFrame().getLastFunc() + "' disabled.");
        }

        Block tmpBlock = ArgsUtil.beginCallArgs(ruby);
        RubyObject[] args = ArgsUtil.setupArgs(ruby, this, iVisited.getArgsNode());
        ArgsUtil.endCallArgs(ruby, tmpBlock);

        ruby.getIterStack().push(ruby.getActIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
        try {
            result = ruby.getActFrame().getLastClass().getSuperClass().call(ruby.getActFrame().getSelf(), ruby.getActFrame().getLastFunc(), args, 3);
        } finally {
            ruby.getIterStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        result = ruby.getTrue();
    }

    /**
     * @see NodeVisitor#visitUndefNode(UndefNode)
     */
    public void visitUndefNode(UndefNode iVisited) {
        if (ruby.getRubyClass() == null) {
            throw new TypeError(ruby, "No class to undef method '" + iVisited.getName() + "'.");
        }

        ruby.getRubyClass().undef(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitUntilNode(UntilNode)
     */
    public void visitUntilNode(UntilNode iVisited) {
        while (eval(iVisited.getConditionNode()).isFalse()) {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        }
    }

    /**
     * @see NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public void visitVAliasNode(VAliasNode iVisited) {
        ruby.aliasGlobalVar(iVisited.getOldName(), iVisited.getNewName());
    }

    /**
     * @see NodeVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
        result = self.getRubyClass().call(self, iVisited.getMethodName(), null, 2);
    }

    /**
     * @see NodeVisitor#visitWhenNode(WhenNode)
     */
    public void visitWhenNode(WhenNode iVisited) {
        eval(iVisited.getBodyNode());
    }

    /**
     * @see NodeVisitor#visitWhileNode(WhileNode)
     */
    public void visitWhileNode(WhileNode iVisited) {
        while (eval(iVisited.getConditionNode()).isTrue()) {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        }
    }

    /**
     * @see NodeVisitor#visitXStrNode(XStrNode)
     */
    public void visitXStrNode(XStrNode iVisited) {
        result = self.funcall("`", builtins.toString(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
        eval(iVisited.getArgsNode());
        if (iVisited.getArgsNode() instanceof ExpandArrayNode && ((RubyArray) result).getLength() == 1) {
            result = ((RubyArray) result).entry(0);
        }
        result = ruby.yield0(result, null, null, false);
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
        result = builtins.newArray();
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        if (ruby.getActFrame().getLastClass() == null) {
            throw new NameError(ruby, "superclass method '" + ruby.getActFrame().getLastFunc() + "' disabled");
        }

        RubyObject[] args = ruby.getActFrame().getArgs();

        ruby.getIterStack().push(ruby.getActIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);
        try {
            result = ruby.getActFrame().getLastClass().getSuperClass().call(ruby.getActFrame().getSelf(), ruby.getActFrame().getLastFunc(), args, 3);
        } finally {
            ruby.getIterStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        result = RubyBignum.newBignum(ruby, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
        result = RubyFixnum.newFixnum(ruby, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
        result = RubyFloat.newFloat(ruby, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
        result = RubyRegexp.newRegexp(ruby, builtins.toString(iVisited.getValue()), iVisited.getOptions());
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        result = builtins.toSymbol(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitExpandArrayNode(ExpandArrayNode)
     */
    public void visitExpandArrayNode(ExpandArrayNode iVisited) {
        eval(iVisited.getExpandNode());
        if (!(result instanceof RubyArray)) {
            if (result.isNil()) {
                result = RubyArray.newArray(ruby, 0);
            } else {
                result = RubyArray.newArray(ruby, result);
            }
        }
        // result.convertToType("Array", "to_ary", false);
    }

    /** Evaluates the body in a class or module definition statement.
     * 
     */
    private void evalClassDefinitionBody(ScopeNode iVisited, RubyModule type) {
        /* String file = ruby.getSourceFile();
           int line = ruby.getSourceLine(); */

        ruby.getActFrame().tmpPush();
        ruby.pushClass(type);
        ruby.getScope().push(iVisited.getLocalNames());
        RubyVarmap.push(ruby);

        ruby.setNamespace(new Namespace(type, ruby.getNamespace()));
        ruby.getActFrame().setNamespace(ruby.getNamespace());

        RubyObject oldSelf = self;

        try {
            if (isTrace()) {
                callTraceFunction(
                    "class",
                    ruby.getSourceFile(),
                    ruby.getSourceLine(),
                    type,
                    ruby.getActFrame().getLastFunc(),
                    ruby.getActFrame().getLastClass());
            }

            self = type;
            eval(iVisited.getBodyNode());
        } finally {
            self = oldSelf;

            ruby.setNamespace(ruby.getNamespace().getParent());
            RubyVarmap.pop(ruby);
            ruby.getScope().pop();
            ruby.popClass();
            ruby.getActFrame().tmpPop();

            if (isTrace()) {
                callTraceFunction(
                    "end",
                    ruby.getSourceFile(),
                    ruby.getSourceLine(),
                    null,
                    ruby.getActFrame().getLastFunc(),
                    ruby.getActFrame().getLastClass());
            }
        }
    }

    private ScopeNode copyNodeScope(ScopeNode node, Namespace namespace) {
        // node.getNamespace().cloneNamespace()
        ScopeNode copy = new ScopeNode(node.getPosition(), null, node.getBodyNode());

        if (node.getLocalNames() != null) {
            copy.setLocalNames(new ArrayList(node.getLocalNames()));
        }

        return copy;
    }

    private boolean isRescueHandled(RubyException actExcptn, IListNode exceptionNodes) {
        // TMP_PROTECT;

        if (exceptionNodes == null) {
            return actExcptn.isKindOf(ruby.getExceptions().getStandardError());
        }

        Block tmpBlock = ArgsUtil.beginCallArgs(ruby);
        RubyObject[] args = ArgsUtil.setupArgs(ruby, this, exceptionNodes);
        ArgsUtil.endCallArgs(ruby, tmpBlock);

        for (int i = 0; i < args.length; i++) {
            if (args[i].kind_of(ruby.getClasses().getModuleClass()).isFalse()) {
                throw new TypeError(ruby, "class or module required for rescue clause");
            }
            if (actExcptn.kind_of((RubyModule) args[i]).isTrue()) {
                return true;
            }
        }
        return false;
    }
}
