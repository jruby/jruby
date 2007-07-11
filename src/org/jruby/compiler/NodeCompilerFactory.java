/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

package org.jruby.compiler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
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
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.YieldNode;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public class NodeCompilerFactory {
    public static final boolean SAFE = System.getProperty("jruby.jit.safe", "true").equals("true");
    public static final Set UNSAFE_CALLS;
    
    static {
        UNSAFE_CALLS = new HashSet();
    }
    
    public static void compile(Node node, MethodCompiler context) {
        switch (node.nodeId) {
        case NodeTypes.ALIASNODE:
            compileAlias(node, context);
            break;
        case NodeTypes.ANDNODE:
            compileAnd(node, context);
            break;
        case NodeTypes.ARRAYNODE:
            compileArray(node, context);
            break;
        case NodeTypes.ATTRASSIGNNODE:
            compileAttrAssign(node, context);
            break;
        case NodeTypes.BEGINNODE:
            compileBegin(node, context);
            break;
        case NodeTypes.BIGNUMNODE:
            compileBignum(node, context);
            break;
        case NodeTypes.BLOCKNODE:
            compileBlock(node, context);
            break;
        case NodeTypes.BREAKNODE:
            // Not safe yet; something weird with break-handling try/catch and calls like "foo bar {}"
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            compileBreak(node, context);
            break;
        case NodeTypes.CALLNODE:
            compileCall(node, context);
            break;
        case NodeTypes.CLASSNODE:
            if (SAFE) throw new NotCompilableException("Can't compile class definitions safely: " + node);
            compileClass(node, context);
            break;
        case NodeTypes.CLASSVARNODE:
            compileClassVar(node, context);
            break;
        case NodeTypes.CLASSVARASGNNODE:
            compileClassVarAsgn(node, context);
            break;
        case NodeTypes.CONSTDECLNODE:
            compileConstDecl(node, context);
            break;
        case NodeTypes.COLON2NODE:
            compileColon2(node, context);
            break;
        case NodeTypes.CONSTNODE:
            compileConst(node, context);
            break;
        case NodeTypes.DASGNNODE:
            compileDAsgn(node, context);
            break;
        case NodeTypes.DEFNNODE:
            compileDefn(node, context);
            break;
        case NodeTypes.DOTNODE:
            compileDot(node, context);
            break;
        case NodeTypes.DSTRNODE:
            compileDStr(node, context);
            break;
        case NodeTypes.DVARNODE:
            compileDVar(node, context);
            break;
        case NodeTypes.EVSTRNODE:
            compileEvStr(node, context);
            break;
        case NodeTypes.FALSENODE:
            compileFalse(node, context);
            break;
        case NodeTypes.FCALLNODE:
            compileFCall(node, context);
            break;
        case NodeTypes.FIXNUMNODE:
            compileFixnum(node, context);
            break;
        case NodeTypes.FLOATNODE:
            compileFloat(node, context);
            break;
        case NodeTypes.GLOBALASGNNODE:
            compileGlobalAsgn(node, context);
            break;
        case NodeTypes.GLOBALVARNODE:
            compileGlobalVar(node, context);
            break;
        case NodeTypes.HASHNODE:
            compileHash(node, context);
            break;
        case NodeTypes.IFNODE:
            compileIf(node, context);
            break;
        case NodeTypes.INSTASGNNODE:
            compileInstAsgn(node, context);
            break;
        case NodeTypes.INSTVARNODE:
            compileInstVar(node, context);
            break;
        case NodeTypes.ITERNODE:
            compileIter(node, context);
            break;
        case NodeTypes.LOCALASGNNODE:
            compileLocalAsgn(node, context);
            break;
        case NodeTypes.LOCALVARNODE:
            compileLocalVar(node, context);
            break;
        case NodeTypes.MATCHNODE:
            compileMatch(node, context);
            break;
        case NodeTypes.MATCH2NODE:
            compileMatch2(node, context);
            break;
        case NodeTypes.MATCH3NODE:
            compileMatch3(node, context);
            break;
        case NodeTypes.MODULENODE:
            if (SAFE) throw new NotCompilableException("Can't compile module definitions safely: " + node);
            compileModule(node, context);
            break;
        case NodeTypes.NEWLINENODE:
            compileNewline(node, context);
            break;
        case NodeTypes.NTHREFNODE:
            compileNthRef(node, context);
            break;
        case NodeTypes.NILNODE:
            compileNil(node, context);
            break;
        case NodeTypes.NOTNODE:
            compileNot(node, context);
            break;
        case NodeTypes.OPASGNNODE:
            compileOpAsgn(node, context);
            break;
        case NodeTypes.ORNODE:
            compileOr(node, context);
            break;
        case NodeTypes.REGEXPNODE:
            compileRegexp(node, context);
            break;
        case NodeTypes.RETURNNODE:
            compileReturn(node, context);
            break;
        case NodeTypes.SELFNODE:
            compileSelf(node, context);
            break;
        case NodeTypes.SPLATNODE:
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            compileSplat(node, context);
            break;
        case NodeTypes.STRNODE:
            compileStr(node, context);
            break;
        case NodeTypes.SVALUENODE:
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            compileSValue(node, context);
            break;
        case NodeTypes.SYMBOLNODE:
            compileSymbol(node, context);
            break;
        case NodeTypes.TRUENODE:
            compileTrue(node, context);
            break;
        case NodeTypes.VCALLNODE:
            compileVCall(node, context);
            break;
        case NodeTypes.WHILENODE:
            compileWhile(node, context);
            break;
        case NodeTypes.YIELDNODE:
            compileYield(node, context);
            break;
        case NodeTypes.ZARRAYNODE:
            compileZArray(node, context);
            break;
        default:
            throw new NotCompilableException("Can't compile node: " + node);
        }
    }
    
    public static void compileArguments(Node node, MethodCompiler context) {
        switch (node.nodeId) {
        case NodeTypes.ARRAYNODE:
            compileArrayArguments(node, context);
            break;
        default:
            throw new NotCompilableException("Can't compile argument node: " + node);
        }
    }
    
    public static NodeCompiler getAssignmentCompiler(Node node) {
        switch (node.nodeId) {
            // disabled for now; incomplete
        //case NodeTypes.MULTIPLEASGNNODE:
        //    return new MultipleAsgnNodeAsgnCompiler();
        }
        
        throw new NotCompilableException("Can't compile assignment node: " + node);
    }
    
    public static YARVNodesCompiler getYARVCompiler() {
        return new YARVNodesCompiler();
    }

    public static void compileAlias(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final AliasNode alias = (AliasNode)node;
        
        context.defineAlias(alias.getNewName(),alias.getOldName());
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.retrieveSelfClass();
            }
        };
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.createObjectArray(new Object[] {alias.getNewName()}, new ArrayCallback() {
                    public void nextValue(MethodCompiler context, Object sourceArray,
                                          int index) {
                        context.loadSymbol(alias.getNewName());
                    }
                });
            }
        };
        
        context.invokeDynamic("method_added", receiverCallback, argsCallback, CallType.FUNCTIONAL, null, false);
    }
    
    public static void compileAnd(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final AndNode andNode = (AndNode)node;
        
        compile(andNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(andNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalAnd(longCallback);
    }
    
    public static void compileArray(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArrayNode arrayNode = (ArrayNode)node;
        
        ArrayCallback callback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                compile(node, context);
            }
        };
        
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        context.createNewArray(arrayNode.isLightweight());
    }
    
    public static void compileAttrAssign(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        AttrAssignNode attrAssignNode = (AttrAssignNode)node;
        
        compile(attrAssignNode.getReceiverNode(), context);
        compileArguments(attrAssignNode.getArgsNode(), context);
        
        context.invokeAttrAssign(attrAssignNode.getName());
    }
    
    public static void compileBegin(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BeginNode beginNode = (BeginNode)node;
        
        compile(beginNode.getBodyNode(), context);
    }

    public static void compileBignum(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.createNewBignum(((BignumNode)node).getValue());
    }

    public static void compileBlock(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BlockNode blockNode = (BlockNode)node;
        
        for (Iterator iter = blockNode.childNodes().iterator(); iter.hasNext();) {
            Node n = (Node)iter.next();
            
            compile(n, context);
            
            if (iter.hasNext()) {
                // clear result from previous line
                context.consumeCurrentValue();
            }
        }
    }
    
    public static void compileBreak(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BreakNode breakNode = (BreakNode)node;
        
        if (breakNode.getValueNode() != null) {
            compile(breakNode.getValueNode(), context);
        } else {
            context.loadNil();
        }
        
        context.issueBreakEvent();
    }
    
    public static void compileCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final CallNode callNode = (CallNode)node;
        
        if (NodeCompilerFactory.SAFE) {
            if (NodeCompilerFactory.UNSAFE_CALLS.contains(callNode.getName())) {
                throw new NotCompilableException("Can't compile call safely: " + node);
            }
        }
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                NodeCompilerFactory.compile(callNode.getReceiverNode(), context);
            }
        };
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArguments(callNode.getArgsNode(), context);
            }
        };
                
        if (callNode.getIterNode() == null) {
            // no block, go for simple version
            if (callNode.getArgsNode() != null) {
                context.invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, null, false);
            } else {
                context.invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, null, false);
            }
        } else {
            // FIXME: Missing blockpassnode handling
            final IterNode iterNode = (IterNode) callNode.getIterNode();
            
            final ClosureCallback closureArg = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    NodeCompilerFactory.compile(iterNode, context);
                }
            };
            
            if (callNode.getArgsNode() != null) {
                context.invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, closureArg, false);
            } else {
                context.invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, closureArg, false);
            }
        }
    }
    
    public static void compileClass(Node node, MethodCompiler context) {
        /** needs new work
        context.lineNumber(node.getPosition());
        
        final ClassNode classNode = (ClassNode)node;
        
        final Node superNode = classNode.getSuperNode();
        
        final Node cpathNode = classNode.getCPath();
        
        ClosureCallback superCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (superNode != null) {
                    NodeCompilerFactory.compile(superNode, context);
                } else {
                    context.loadObject();
                }
            }
        };
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (classNode.getBodyNode() != null) {
                    NodeCompilerFactory.compile(classNode.getBodyNode(), context);
                }
                context.loadNil();
            }
        };
        
        ClosureCallback pathCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (cpathNode instanceof Colon2Node) {
                    Node leftNode = ((Colon2Node)cpathNode).getLeftNode();
                    if (leftNode != null) {
                        NodeCompilerFactory.compile(leftNode, context);
                    } else {
                        context.loadNil();
                    }
                } else if (cpathNode instanceof Colon3Node) {
                    context.loadObject();
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.defineClass(classNode.getCPath().getName(), classNode.getScope(), superCallback, pathCallback, bodyCallback); */
    }

    public static void compileClassVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarNode classVarNode = (ClassVarNode)node;
        
        context.retrieveClassVariable(classVarNode.getName());
    }

    public static void compileClassVarAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode)node;
        
        // FIXME: probably more efficient with a callback
        compile(classVarAsgnNode.getValueNode(), context);
        context.assignClassVariable(classVarAsgnNode.getName());
    }
    
    public static void compileConstDecl(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ConstDeclNode constDeclNode = (ConstDeclNode)node;
        
        if (constDeclNode.getConstNode() == null) {
            compile(constDeclNode.getValueNode(), context);
        
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constDeclNode.nodeId == NodeTypes.COLON2NODE) {
            compile(constDeclNode.getValueNode(), context);
        
            compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInObject(constDeclNode.getName());
        }
    }

    public static void compileConst(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ConstNode constNode = (ConstNode)node;
        
        context.retrieveConstant(constNode.getName());
    }
    
    public static void compileColon2(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        final Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if(leftNode == null) {
            context.loadObject();
            context.retrieveConstantFromModule(name);
        } else {
            final ClosureCallback receiverCallback = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    NodeCompilerFactory.compile(iVisited.getLeftNode(), context);
                }
            };
            
            BranchCallback moduleCallback = new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        receiverCallback.compile(context);
                        context.retrieveConstantFromModule(name);
                    }
                };

            BranchCallback notModuleCallback = new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        context.invokeDynamic(name, receiverCallback, null, CallType.FUNCTIONAL, null, false);
                    }
                };

            context.branchIfModule(receiverCallback, moduleCallback, notModuleCallback);
        }
    }
    
    public static void compileDAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        compile(dasgnNode.getValueNode(), context);
        
        context.assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }
    
    public static void compileDefn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final DefnNode defnNode = (DefnNode)node;
        final ArgsNode argsNode = defnNode.getArgsNode();
        
        NodeCompilerFactory.confirmNodeIsSafe(argsNode);
        
        ClosureCallback body = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (defnNode.getBodyNode() != null) {
                    NodeCompilerFactory.compile(defnNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        final ArrayCallback evalOptionalValue = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object object, int index) {
                ListNode optArgs = (ListNode)object;
                
                Node node = optArgs.get(index);

                compile(node, context);
            }
        };
        
        ClosureCallback args = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                int required = argsNode.getArgsCount();
                int restArg = argsNode.getRestArg();
                boolean hasOptArgs = argsNode.getOptArgs() != null;
                Arity arity = argsNode.getArity();
                
                context.lineNumber(argsNode.getPosition());
                
                if (argsNode.getBlockArgNode() != null) {
                    context.processBlockArgument(argsNode.getBlockArgNode().getCount());
                }
                
                if (hasOptArgs) {
                    if (restArg > -1) {
                        int opt = argsNode.getOptArgs().size();
                        context.processRequiredArgs(arity, required, opt, restArg);
                        
                        ListNode optArgs = argsNode.getOptArgs();
                        context.assignOptionalArgs(optArgs, required, opt, evalOptionalValue);
                        
                        context.processRestArg(required + opt, restArg);
                    } else {
                        int opt = argsNode.getOptArgs().size();
                        context.processRequiredArgs(arity, required, opt, restArg);
                        
                        ListNode optArgs = argsNode.getOptArgs();
                        context.assignOptionalArgs(optArgs, required, opt, evalOptionalValue);
                    }
                } else {
                    if (restArg > -1) {
                        context.processRequiredArgs(arity, required, 0, restArg);
                        
                        context.processRestArg(required, restArg);
                    } else {
                        context.processRequiredArgs(arity, required, 0, restArg);
                    }
                }
            }
        };
        
        context.defineNewMethod(defnNode.getName(), defnNode.getScope(), body, args);
    }
    
    public static void compileDot(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DotNode dotNode = (DotNode)node;

        compile(dotNode.getBeginNode(), context);
        compile(dotNode.getEndNode(), context);
        
        context.createNewRange(dotNode.isExclusive());
    }
    
    public static void compileDStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final DStrNode dstrNode = (DStrNode)node;
        
        ArrayCallback dstrCallback = new ArrayCallback() {
                public void nextValue(MethodCompiler context, Object sourceArray,
                                      int index) {
                    compile(dstrNode.get(index), context);
                }
            };
        context.createNewString(dstrCallback,dstrNode.size());
    }
    
    public static void compileDVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DVarNode dvarNode = (DVarNode)node;
        
        context.retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
    }
    
    public static void compileEvStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final EvStrNode evStrNode = (EvStrNode)node;
        
        compile(evStrNode.getBody(), context);
        context.asString();
    }
    
    public static void compileFalse(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadFalse();

        context.pollThreadEvents();
    }
    
    public static void compileFCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final FCallNode fcallNode = (FCallNode)node;
        
        if (NodeCompilerFactory.SAFE) {
            if (NodeCompilerFactory.UNSAFE_CALLS.contains(fcallNode.getName())) {
                throw new NotCompilableException("Can't compile call safely: " + node);
            }
        }
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArguments(fcallNode.getArgsNode(), context);
            }
        };
        
        if (fcallNode.getIterNode() == null) {
            // no block, go for simple version
            if (fcallNode.getArgsNode() != null) {
                context.invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, null, false);
            } else {
                context.invokeDynamic(fcallNode.getName(), null, null, CallType.FUNCTIONAL, null, false);
            }
        } else {
            // FIXME: Missing blockpasnode stuff here
            
            final IterNode iterNode = (IterNode) fcallNode.getIterNode();
            
            final ClosureCallback closureArg = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    NodeCompilerFactory.compile(iterNode, context);
                }
            };

            if (fcallNode.getArgsNode() != null) {
                context.invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, closureArg, false);
            } else {
                context.invokeDynamic(fcallNode.getName(), null, null, CallType.FUNCTIONAL, closureArg, false);
            }
        }
    }

    public static void compileFixnum(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        FixnumNode fixnumNode = (FixnumNode)node;
        
        context.createNewFixnum(fixnumNode.getValue());
    }
    
    public static void compileFloat(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        FloatNode floatNode = (FloatNode)node;
        
        context.createNewFloat(floatNode.getValue());
    }
    
    public static void compileGlobalAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
        
        compile(globalAsgnNode.getValueNode(), context);
                
        if (globalAsgnNode.getName().length() == 2) {
            // FIXME: This is not aware of lexical scoping
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                context.assignLastLine();
                return;
            case '~':
                assert false: "Parser shouldn't allow assigning to $~";
                return;
            }
        }
        
        context.assignGlobalVariable(globalAsgnNode.getName());
    }
    
    public static void compileGlobalVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalVarNode globalVarNode = (GlobalVarNode)node;
                
        if (globalVarNode.getName().length() == 2) {
            // FIXME: This is not aware of lexical scoping
            switch (globalVarNode.getName().charAt(1)) {
            case '_':
                context.retrieveLastLine();
                return;
            case '~':
                context.retrieveBackRef();
                return;
            }
        }
        
        context.retrieveGlobalVariable(globalVarNode.getName());
    }
    
    public static void compileHash(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        HashNode hashNode = (HashNode)node;
        
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            context.createEmptyHash();
            return;
        }
        
        ArrayCallback hashCallback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                                  int index) {
                ListNode listNode = (ListNode)sourceArray;
                int keyIndex = index * 2;
                compile(listNode.get(keyIndex), context);
                compile(listNode.get(keyIndex + 1), context);
            }
        };
        
        context.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }
    
    public static void compileIf(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final IfNode ifNode = (IfNode)node;
        
        compile(ifNode.getCondition(), context);
        
        BranchCallback trueCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                if (ifNode.getThenBody() != null) {
                    compile(ifNode.getThenBody(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        BranchCallback falseCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                if (ifNode.getElseBody() != null) {
                    compile(ifNode.getElseBody(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.performBooleanBranch(trueCallback, falseCallback);
    }
    
    public static void compileInstAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        
        compile(instAsgnNode.getValueNode(), context);
        context.assignInstanceVariable(instAsgnNode.getName());
    }
    
    public static void compileInstVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        InstVarNode instVarNode = (InstVarNode)node;
        
        context.retrieveInstanceVariable(instVarNode.getName());
    }
    
    public static void compileIter(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final IterNode iterNode = (IterNode)node;

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (iterNode.getBodyNode() != null) {
                    NodeCompilerFactory.compile(iterNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final ClosureCallback closureArgs = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (iterNode.getVarNode() != null) {
                    AssignmentCompiler.assign(iterNode.getVarNode(), 0, context);
                }
            }
        };
        
        context.createNewClosure(iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(), closureBody, closureArgs);
    }

    public static void compileLocalAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
        
        compile(localAsgnNode.getValueNode(), context);
        
        context.assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }
    
    public static void compileLocalVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        LocalVarNode localVarNode = (LocalVarNode)node;
        
        context.retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
    }
    
    public static void compileMatch(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        MatchNode matchNode = (MatchNode)node;

        compile(matchNode.getRegexpNode(), context);
        
        context.match();
    }
    
    public static void compileMatch2(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        Match2Node matchNode = (Match2Node)node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);
        
        context.match2();
    }
    public static void compileMatch3(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        Match3Node matchNode = (Match3Node)node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);
        
        context.match3();
    }
    
    public static void compileModule(Node node, MethodCompiler context) {
        /* needs work
        context.lineNumber(node.getPosition());
        
        final ModuleNode moduleNode = (ModuleNode)node;
        
        final Node cpathNode = moduleNode.getCPath();
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (moduleNode.getBodyNode() != null) {
                    NodeCompilerFactory.compile(moduleNode.getBodyNode(), context);
                }
                context.loadNil();
            }
        };
        
        ClosureCallback pathCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (cpathNode instanceof Colon2Node) {
                    Node leftNode = ((Colon2Node)cpathNode).getLeftNode();
                    if (leftNode != null) {
                        NodeCompilerFactory.compile(leftNode, context);
                    } else {
                        context.loadNil();
                    }
                } else if (cpathNode instanceof Colon3Node) {
                    context.loadObject();
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.defineModule(moduleNode.getCPath().getName(), moduleNode.getScope(), pathCallback, bodyCallback); */
    }

    public static void compileNewline(Node node, MethodCompiler context) {
        // TODO: add trace call
        
        NewlineNode newlineNode = (NewlineNode)node;
        
        compile(newlineNode.getNextNode(), context);
    }
    public static void compileNthRef(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        NthRefNode nthRefNode = (NthRefNode)node;
        
        context.nthRef(nthRefNode.getMatchNumber());
    }
    
    public static void compileNil(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadNil();
        
        context.pollThreadEvents();
    }
    
    public static void compileNot(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        NotNode notNode = (NotNode)node;
        
        compile(notNode.getConditionNode(), context);
        
        context.negateCurrentValue();
    }
    
    public static void compileOpAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        // FIXME: This is a little more complicated than it needs to be; do we see now why closures would be nice in Java?
        
        final OpAsgnNode opAsgnNode = (OpAsgnNode)node;
        
        final ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                NodeCompilerFactory.compile(opAsgnNode.getReceiverNode(), context); // [recv]
                context.duplicateCurrentValue(); // [recv, recv]
            }
        };
        
        BranchCallback doneBranch = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // get rid of extra receiver, leave the variable result present
                context.swapValues();
                context.consumeCurrentValue();
            }
        };
        
        // Just evaluate the value and stuff it in an argument array
        final ArrayCallback justEvalValue = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                    int index) {
                compile(((Node[])sourceArray)[index], context);
            }
        };
        
        BranchCallback assignBranch = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // eliminate extra value, eval new one and assign
                context.consumeCurrentValue();
                context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
                context.invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
            }
        };
        
        ClosureCallback receiver2Callback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.invokeDynamic(opAsgnNode.getVariableName(), receiverCallback, null, CallType.FUNCTIONAL, null, false); // [recv, varValue]
            }
        };
        
        if (opAsgnNode.getOperatorName() == "||") {
            // if lhs is true, don't eval rhs and assign
            receiver2Callback.compile(context);
            context.duplicateCurrentValue();
            context.performBooleanBranch(doneBranch, assignBranch);
        } else if (opAsgnNode.getOperatorName() == "&&") {
            // if lhs is true, eval rhs and assign
            receiver2Callback.compile(context);
            context.duplicateCurrentValue();
            context.performBooleanBranch(assignBranch, doneBranch);
        } else {
            // eval new value, call operator on old value, and assign
            ClosureCallback argsCallback = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
                }
            };
            context.invokeDynamic(opAsgnNode.getOperatorName(), receiver2Callback, argsCallback, CallType.FUNCTIONAL, null, false);
            context.createObjectArray(1);
            context.invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
        }

        context.pollThreadEvents();
    }
    
    public static void compileOr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final OrNode orNode = (OrNode)node;
        
        compile(orNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(orNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalOr(longCallback);
    }
    
    public static void compileRegexp(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        RegexpNode reNode = (RegexpNode)node;
        
        int opts = reNode.getOptions();
        String lang = ((opts & 16) == 16) ? "n" : null;
        if((opts & 48) == 48) { // param s
            lang = "s";
        } else if((opts & 32) == 32) { // param e
            lang = "e";
        } else if((opts & 64) != 0) { // param u
            lang = "u";
        }

        context.createNewRegexp(reNode.getValue(), reNode.getOptions(), lang);
    }
    
    public static void compileReturn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ReturnNode returnNode = (ReturnNode)node;
        
        compile(returnNode.getValueNode(), context);
        
        context.performReturn();
    }
    
    public static void compileRoot(Node node, ScriptCompiler context) {
        RootNode rootNode = (RootNode)node;
        
        context.startScript();
        
        // create method for toplevel of script
        MethodCompiler methodCompiler = context.startMethod("__file__", null);

        // try to compile the script's body
        try {
            Node nextNode = rootNode.getBodyNode();
            if (nextNode != null) {
                compile(nextNode, methodCompiler);
            }
        } catch (NotCompilableException nce) {
            // TODO: recover somehow? build a pure eval method?
            throw nce;
        }
        
        methodCompiler.endMethod();
        
        context.endScript();
    }
    
    public static void compileSelf(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.retrieveSelf();
    }    
    
    public static void compileSplat(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        SplatNode splatNode = (SplatNode)node;
        
        compile(splatNode.getValue(), context);
        
        context.splatCurrentValue();
    }
    
    public static void compileStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        StrNode strNode = (StrNode)node;
        
        context.createNewString(strNode.getValue());
    }
    
    public static void compileSValue(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        SValueNode svalueNode = (SValueNode)node;
        
        compile(svalueNode.getValue(), context);
        
        context.singlifySplattedValue();
    }
    
    public static void compileSymbol(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.createNewSymbol(((SymbolNode)node).getName());
    }    
    
    public static void compileTrue(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadTrue();
        
        context.pollThreadEvents();
    }

    public static void compileVCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        VCallNode vcallNode = (VCallNode)node;
        
        if (NodeCompilerFactory.SAFE) {
            if (NodeCompilerFactory.UNSAFE_CALLS.contains(vcallNode.getName())) {
                throw new NotCompilableException("Can't compile call safely: " + node);
            }
        }
        
        context.invokeDynamic(vcallNode.getName(), null, null, CallType.VARIABLE, null, false);
    }
    
    public static void compileWhile(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final WhileNode whileNode = (WhileNode)node;
        
        BranchCallback condition = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(whileNode.getConditionNode(), context);
            }
        };
        
        BranchCallback body = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // this could probably be more efficient, and just avoid popping values for each loop
                // when no values are being generated
                if (whileNode.getBodyNode() == null) {
                    context.loadNil();
                    return;
                }
                compile(whileNode.getBodyNode(), context);
            }
        };
        
        context.performBooleanLoop(condition, body, whileNode.evaluateAtStart());
        
        context.pollThreadEvents();
    }
    
    public static void compileYield(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        YieldNode yieldNode = (YieldNode)node;
        
        if (yieldNode.getArgsNode() != null) {
            compile(yieldNode.getArgsNode(), context);
        }
        
        context.yield(yieldNode.getArgsNode() != null, yieldNode.getCheckState());
    }
    
    public static void compileZArray(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.createEmptyArray();
    }
    
    public static void compileArrayArguments(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArrayNode arrayNode = (ArrayNode)node;
        
        ArrayCallback callback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                compile(node, context);
            }
        };
        
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        // leave as a normal array
    }
    
    /**
     * Check whether the target node can safely be compiled.
     * 
     * @param node 
     */
    public static void confirmNodeIsSafe(Node node) {
        switch (node.nodeId) {
        case NodeTypes.ARGSNODE:
            ArgsNode argsNode = (ArgsNode)node;
            // FIXME: We can't compile cases like def(a=(b=1)) because the variables
            // in the arg list get ordered differently than you might expect (b comes first)
            // So the code below searches through all opt args, ensuring none of them skip
            // indicies. A skipped index means there's a hidden local var/arg like b above
            // and so we shouldn't try to compile.
            if (argsNode.getOptArgs() != null && argsNode.getOptArgs().size() > 0) {
                int index = argsNode.getArgsCount() - 1;
                
                for (int i = 0; i < argsNode.getOptArgs().size(); i++) {
                    int newIndex = ((LocalAsgnNode)argsNode.getOptArgs().get(i)).getIndex() - 2;
                    
                    if (newIndex - index != 1) {
                        throw new NotCompilableException("Can't compile def with optional args that assign other variables at: " + node.getPosition());
                    }
                    index = newIndex;
                }
            }
            
            // Also do not compile anything with a block argument or "rest" argument
            if (argsNode.getBlockArgNode() != null) throw new NotCompilableException("Can't compile def with block arg at: " + node.getPosition());
            break;
        }
    }
}
