/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.ast.util;

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import org.jruby.runtime.Visibility;
import org.jruby.util.Asserts;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
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
import org.jruby.ast.DAsgnCurrNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.ExpandArrayNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
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
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgsNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.IListNode;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class AstPersistenceDelegates {
    private static Map delegates = null;
    private static ClassLoader cl;
    
    private AstPersistenceDelegates() {
    }

    public static Map get() {
        if (delegates == null) {
            init();
        }
        return delegates;
    }
    
    private static void init() {
        delegates = new HashMap();
        cl = new Runnable() {
            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
            }
        }.getClass().getClassLoader();
        
        // create Node delegates
        add(load("AliasNode"), "newName", "oldName");
        add(load("AndNode"), "firstNode", "secondNode");
        add(load("ArgsNode"), new String[] {"position", "argsCount", "optArgs", "restArg", "blockArgNode"});
        addListNode(load("ArrayNode"), new String[] {"position"});
        add(load("AttrSetNode"), "attributeName");
        add(load("BackRefNode"), "type");
        add(load("BeginNode"), "bodyNode");
        add(load("BignumNode"), "value");
        add(load("BlockArgNode"), "count");
        addListNode(load("BlockNode"), new String[] {"position"});
        add(load("BlockPassNode"), "bodyNode");
        add(load("BreakNode"));
        add(load("CallNode"), "receiverNode", "name", "argsNode");
        add(load("CaseNode"), "caseNode", "whenNode", "elseNode");
        add(load("ClassNode"), "className", "bodyNode", "superNode");
        add(load("ClassVarAsgnNode"), "name", "valueNode");
        add(load("ClassVarDeclNode"), "name", "valueNode");
        add(load("ClassVarNode"), "name");
        add(load("Colon2Node"), "leftNode", "name");
        add(load("Colon3Node"), "name");
        add(load("ConstDeclNode"), "name", "valueNode");
        add(load("ConstNode"), "name");
        add(load("DAsgnCurrNode"), "name", "valueNode");
        add(load("DAsgnNode"), "name", "valueNode");
        add(load("DefinedNode"), "expressionNode");
        add(load("DefnNode"), new String[] {"position", "name", "argsNode", "bodyNode", "visibility"});
        add(load("DefsNode"), new String[] {"position", "receiverNode", "name", "argsNode", "bodyNode"});
        add(load("DotNode"), "beginNode", "endNode", "exclusive");
        addListNode(load("DRegexpNode"), new String[] {"position", "options", "once"});
        addListNode(load("DStrNode"), new String[] {"position"});
        add(load("DVarNode"), "name");
        addListNode(load("DXStrNode"), new String[] {"position"});
        add(load("EnsureNode"), "bodyNode", "ensureNode");
        add(load("EvStrNode"), "value");
        add(load("ExpandArrayNode"), "expandNode");
        add(load("FalseNode"));
        add(load("FCallNode"), "name", "argsNode");
        add(load("FixnumNode"), "value");
        add(load("FlipNode"), "beginNode", "endNode", "exclusive");
        add(load("FloatNode"), "value");
        add(load("ForNode"), "varNode", "bodyNode", "iterNode");
        add(load("GlobalAsgnNode"), "name", "valueNode");
        add(load("GlobalVarNode"), "name");
        add(load("HashNode"), "listNode");
        add(load("IfNode"), "condition", "thenBody", "elseBody");
        add(load("InstAsgnNode"), "name", "valueNode");
        add(load("InstVarNode"), "name");
        add(load("IterNode"), "varNode", "bodyNode", "iterNode");
        add(load("LocalAsgnNode"), "count", "valueNode");
        add(load("LocalVarNode"), "count");
        add(load("Match2Node"), "receiverNode", "valueNode");
        add(load("Match3Node"), "receiverNode", "valueNode");
        add(load("MatchNode"), "regexpNode");
        add(load("ModuleNode"), "name", "bodyNode");
        add(load("MultipleAsgnNode"), "headNode", "argsNode");
        add(load("NewlineNode"), "nextNode");
        add(load("NextNode"));
        add(load("NilNode"));
        add(load("NotNode"), "conditionNode");
        add(load("NthRefNode"), "matchNumber");
        add(load("OpAsgnAndNode"), "firstNode", "secondNode");
        add(load("OpAsgnNode"), new String[] {"position", "receiverNode", "valueNode", "variableName", "operatorName"});
        add(load("OpAsgnOrNode"), "firstNode", "secondNode");
        add(load("OpElementAsgnNode"), new String[] {"position", "receiverNode", "operatorName", "argsNode", "valueNode"});
        add(load("OptNNode"), "bodyNode");
        add(load("OrNode"), "firstNode", "secondNode");
        add(load("PostExeNode"));
        add(load("RedoNode"));
        add(load("RegexpNode"), "value", "options");
        addListNode(load("RescueBodyNode"), new String[] {"position", "exceptionNodes", "bodyNode"});
        add(load("RescueNode"), "bodyNode", "rescueNodes", "elseNode");
        add(load("RestArgsNode"), "argumentNode");
        add(load("RetryNode"));
        add(load("ReturnNode"), "valueNode");
        add(load("SClassNode"), "receiverNode", "bodyNode");
        add(load("ScopeNode"), "localNames", "bodyNode");
        add(load("SelfNode"));
        add(load("StrNode"), "value");
        add(load("SuperNode"), "argsNode");
        add(load("SymbolNode"), "name");
        add(load("TrueNode"));
        add(load("UndefNode"), "name");
        add(load("UntilNode"), "conditionNode", "bodyNode");
        add(load("VAliasNode"), "oldName", "newName");
        add(load("VCallNode"), "methodName");
        add(load("WhenNode"), "expressionNodes", "bodyNode");
        add(load("WhileNode"), "conditionNode", "bodyNode");
        add(load("XStrNode"), "value");
        add(load("YieldNode"), "argsNode");
        add(load("ZArrayNode"));
        add(load("ZSuperNode"));
        
        add(loadClass("org.ablaf.internal.lexer.DefaultLexerPosition"), new String[] {"file", "line", "column"});
        addVisibility();
    }
    
    private static void add(Class type) {
        add(type, new String[] {"position"});
    }

    private static void add(Class type, String first) {
        add(type, new String[] {"position", first});
    }

    private static void add(Class type, String first, String second) {
        add(type, new String[] {"position", first, second});
    }

    private static void add(Class type, String first, String second, String third) {
        add(type, new String[] {"position", first, second, third});
    }
    
    private static void add(Class type, String[] properties) {
        delegates.put(type, new DefaultPersistenceDelegate(properties));
    }

    private static void addListNode(Class type, String[] properties) {
        delegates.put(type, new DefaultPersistenceDelegate(properties) {
            /**
             * @see java.beans.PersistenceDelegate#initialize(Class, Object, Object, Encoder)
             */
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                IListNode listNode = (IListNode)oldInstance;
                Iterator iter = listNode.iterator();
                while (iter.hasNext()) {
                    out.writeStatement(new Statement(oldInstance, "add", new Object[] {iter.next()}));
                }
                super.initialize(type, oldInstance, newInstance, out);
            }
        });
    }
    
    private static void addVisibility() {
        final Field[] fields = Visibility.class.getFields();
        delegates.put(loadClass("org.jruby.runtime.Visibility"), new PersistenceDelegate() {
            /**
             * @see java.beans.PersistenceDelegate#instantiate(Object, Encoder)
             */
            protected Expression instantiate(Object oldInstance, Encoder out) {
                for (int i = 0; i < 4; i++) {
                    try {
                        if (fields[i].get(null) == oldInstance) {
                            return new Expression(fields[i], "get", new Object[] {null});
                        }
                    } catch (IllegalAccessException e) {
                        Asserts.notReached("IllegalAccessException: " + e.getMessage());
                    }
                }
                Asserts.notReached();
                return null;
            }
        });
    }
    
    public static Class load(String name) {
        return loadClass("org.jruby.ast." + name);
    }

    public static Class loadClass(String name) {
        try {
            return cl.loadClass(name);
        } catch (ClassNotFoundException cnfExcptn) {
            Asserts.notReached("ClassNotFoundException: " + cnfExcptn);
            return null;
        }
    }
}