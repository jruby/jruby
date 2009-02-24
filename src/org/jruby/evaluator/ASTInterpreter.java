/*
 ******************************************************************************
 * BEGIN LICENSE BLOCK *** Version: CPL 1.0/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Common Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * Copyright (C) 2006 Charles Oliver Nutter <headius@headius.com>
 * Copytight (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"), in
 * which case the provisions of the GPL or the LGPL are applicable instead of
 * those above. If you wish to allow use of your version of this file only under
 * the terms of either the GPL or the LGPL, and not to allow others to use your
 * version of this file under the terms of the CPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and other
 * provisions required by the GPL or the LGPL. If you do not delete the
 * provisions above, a recipient may use your version of this file under the
 * terms of any one of the CPL, the GPL or the LGPL. END LICENSE BLOCK ****
 ******************************************************************************/

package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Frame;
import org.jruby.runtime.InterpretedBlock;
import org.jruby.util.TypeConverter;

public class ASTInterpreter {
    @Deprecated
    public static IRubyObject eval(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block block) {
        assert self != null : "self during eval must never be null";
        
        // TODO: Make into an assert once I get things like blockbodynodes to be implicit nil
        if (node == null) return runtime.getNil();
        
        try {
            return node.interpret(runtime, context, self, block);
        } catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        }
    }
    
    /**
     * Evaluate the given string under the specified binding object. If the binding is not a Proc or Binding object
     * (RubyProc or RubyBinding) throw an appropriate type error.
     * @param context TODO
     * @param evalString The string containing the text to be evaluated
     * @param binding The binding object under which to perform the evaluation
     * @param file The filename to use when reporting errors during the evaluation
     * @param lineNumber is the line number to pretend we are starting from
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalWithBinding(ThreadContext context, IRubyObject src, Binding binding) {
        Ruby runtime = src.getRuntime();
        DynamicScope evalScope = binding.getDynamicScope().getEvalScope();
        
        // FIXME:  This determine module is in a strange location and should somehow be in block
        evalScope.getStaticScope().determineModule();

        Frame lastFrame = context.preEvalWithBinding(binding);
        try {
            // Binding provided for scope, use it
            IRubyObject newSelf = binding.getSelf();
            RubyString source = src.convertToString();
            Node node = runtime.parseEval(source.getByteList(), binding.getFile(), evalScope, binding.getLine());

            return node.interpret(runtime, context, newSelf, binding.getFrame().getBlock());
        } catch (JumpException.BreakJump bj) {
            throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
        } catch (JumpException.RedoJump rj) {
            throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, (IRubyObject)rj.getValue(), "unexpected redo");
        } catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        } finally {
            context.postEvalWithBinding(binding, lastFrame);
        }
    }

    /**
     * Evaluate the given string.
     * @param context TODO
     * @param evalString The string containing the text to be evaluated
     * @param file The filename to use when reporting errors during the evaluation
     * @param lineNumber that the eval supposedly starts from
     * @return An IRubyObject result from the evaluation
     * @deprecated Call with a RubyString now.
     */
    public static IRubyObject evalSimple(ThreadContext context, IRubyObject self, IRubyObject src, String file, int lineNumber) {
        RubyString source = src.convertToString();
        return evalSimple(context, self, source, file, lineNumber);
    }

    /**
     * Evaluate the given string.
     * @param context TODO
     * @param evalString The string containing the text to be evaluated
     * @param file The filename to use when reporting errors during the evaluation
     * @param lineNumber that the eval supposedly starts from
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalSimple(ThreadContext context, IRubyObject self, RubyString src, String file, int lineNumber) {
        // this is ensured by the callers
        assert file != null;

        Ruby runtime = src.getRuntime();
        String savedFile = context.getFile();
        int savedLine = context.getLine();

        // no binding, just eval in "current" frame (caller's frame)
        RubyString source = src.convertToString();
        
        DynamicScope evalScope = context.getCurrentScope().getEvalScope();
        evalScope.getStaticScope().determineModule();
        
        try {
            Node node = runtime.parseEval(source.getByteList(), file, evalScope, lineNumber);
            
            return node.interpret(runtime, context, self, Block.NULL_BLOCK);
        } catch (JumpException.BreakJump bj) {
            throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
        } catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        } finally {
            // restore position
            context.setFile(savedFile);
            context.setLine(savedLine);
        }
    }


    public static void callTraceFunction(Ruby runtime, ThreadContext context, RubyEvent event) {
        String name = context.getFrameName();
        RubyModule type = context.getFrameKlazz();
        runtime.callEventHooks(context, event, context.getFile(), context.getLine(), name, type);
    }
    
    public static IRubyObject pollAndReturn(ThreadContext context, IRubyObject result) {
        context.pollThreadEvents();

        return result;
    }
    
    public static IRubyObject multipleAsgnArrayNode(Ruby runtime, ThreadContext context, MultipleAsgnNode iVisited, ArrayNode node, IRubyObject self, Block aBlock) {
        IRubyObject[] array = new IRubyObject[node.size()];

        for (int i = 0; i < node.size(); i++) {
            array[i] = node.get(i).interpret(runtime,context, self, aBlock);
        }
        return AssignmentVisitor.multiAssign(runtime, context, self, iVisited, RubyArray.newArrayNoCopyLight(runtime, array), false);
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    public static IRubyObject evalClassDefinitionBody(Ruby runtime, ThreadContext context, StaticScope scope, 
            Node bodyNode, RubyModule type, IRubyObject self, Block block) {
        context.preClassEval(scope, type);

        try {
            if (runtime.hasEventHooks()) {
                callTraceFunction(runtime, context, RubyEvent.CLASS);
            }

            if (bodyNode == null) return runtime.getNil();
            return bodyNode.interpret(runtime, context, type, block);
        } finally {
            if (runtime.hasEventHooks()) {
                callTraceFunction(runtime, context, RubyEvent.END);
            }
            
            context.postClassEval();
        }
    }

    public static String getArgumentDefinition(Ruby runtime, ThreadContext context, Node node, String type, IRubyObject self, Block block) {
        if (node == null) return type;
            
        if (node instanceof ArrayNode) {
            ArrayNode list = (ArrayNode) node;
            int size = list.size();

            for (int i = 0; i < size; i++) {
                if (list.get(i).definition(runtime, context, self, block) == null) return null;
            }
        } else if (node.definition(runtime, context, self, block) == null) {
            return null;
        }

        return type;
    }
    
    public static Block getBlock(Ruby runtime, ThreadContext context, IRubyObject self, Block currentBlock, Node blockNode) {
        if (blockNode == null) return Block.NULL_BLOCK;
        
        if (blockNode instanceof IterNode) {
            return getIterNodeBlock(blockNode, context,self);
        } else if (blockNode instanceof BlockPassNode) {
            return getBlockPassBlock(blockNode, runtime,context, self, currentBlock);
        }
         
        assert false: "Trying to get block from something which cannot deliver";
        return null;
    }

    private static Block getBlockPassBlock(Node blockNode, Ruby runtime, ThreadContext context, IRubyObject self, Block currentBlock) {
        Node bodyNode = ((BlockPassNode) blockNode).getBodyNode();
        IRubyObject proc;
        if (bodyNode == null) {
            proc = runtime.getNil();
        } else {
            proc = bodyNode.interpret(runtime, context, self, currentBlock);
        }

        return RuntimeHelpers.getBlockFromBlockPassBody(proc, currentBlock);
    }

    private static Block getIterNodeBlock(Node blockNode, ThreadContext context, IRubyObject self) {
        IterNode iterNode = (IterNode) blockNode;

        StaticScope scope = iterNode.getScope();
        scope.determineModule();

        // Create block for this iter node
        // FIXME: We shouldn't use the current scope if it's not actually from the same hierarchy of static scopes
        return InterpretedBlock.newInterpretedClosure(context, iterNode.getBlockBody(), self);
    }

    /* Something like cvar_cbase() from eval.c, factored out for the benefit
     * of all the classvar-related node evaluations */
    public static RubyModule getClassVariableBase(ThreadContext context, Ruby runtime) {
        StaticScope scope = context.getCurrentScope().getStaticScope();
        RubyModule rubyClass = scope.getModule();
        if (rubyClass.isSingleton() || rubyClass == runtime.getDummy()) {
            scope = scope.getPreviousCRefScope();
            rubyClass = scope.getModule();
            if (scope.getPreviousCRefScope() == null) {
                runtime.getWarnings().warn(ID.CVAR_FROM_TOPLEVEL_SINGLETON_METHOD, "class variable access from toplevel singleton method");
            }            
        }
        return rubyClass;
    }

    @Deprecated
    public static String getDefinition(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        try {
            context.setWithinDefined(true);
            return node.definition(runtime, context, self, aBlock);
        } finally {
            context.setWithinDefined(false);
        }
    }

    public static IRubyObject[] setupArgs(Ruby runtime, ThreadContext context, Node node, IRubyObject self, Block aBlock) {
        if (node == null) return IRubyObject.NULL_ARRAY;

        if (node instanceof ArrayNode) {
            ArrayNode argsArrayNode = (ArrayNode) node;
            String savedFile = context.getFile();
            int savedLine = context.getLine();
            int size = argsArrayNode.size();
            IRubyObject[] argsArray = new IRubyObject[size];

            for (int i = 0; i < size; i++) {
                argsArray[i] = argsArrayNode.get(i).interpret(runtime, context, self, aBlock);
            }

            context.setFile(savedFile);
            context.setLine(savedLine);

            return argsArray;
        }

        return ArgsUtil.convertToJavaArray(node.interpret(runtime,context, self, aBlock));
    }

    @Deprecated
    public static IRubyObject aValueSplat(Ruby runtime, IRubyObject value) {
        if (!(value instanceof RubyArray) || ((RubyArray) value).length().getLongValue() == 0) {
            return runtime.getNil();
        }

        RubyArray array = (RubyArray) value;

        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    @Deprecated
    public static RubyArray arrayValue(Ruby runtime, IRubyObject value) {
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can 
            // remove this hack too.
            if (value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                value = value.callMethod(runtime.getCurrentContext(), "to_a");
                if (!(value instanceof RubyArray)) throw runtime.newTypeError("`to_a' did not return Array");
                return (RubyArray)value;
            } else {
                return runtime.newArray(value);
            }
        }
        return (RubyArray)tmp;
    }

    @Deprecated
    public static IRubyObject aryToAry(Ruby runtime, IRubyObject value) {
        if (value instanceof RubyArray) return value;

        if (value.respondsTo("to_ary")) {
            return TypeConverter.convertToType(value, runtime.getArray(), "to_ary", false);
        }

        return runtime.newArray(value);
    }

    @Deprecated
    public static RubyArray splatValue(Ruby runtime, IRubyObject value) {
        if (value.isNil()) {
            return runtime.newArray(value);
        }

        return arrayValue(runtime, value);
    }

    // Used by the compiler to simplify arg processing
    @Deprecated
    public static RubyArray splatValue(IRubyObject value, Ruby runtime) {
        return splatValue(runtime, value);
    }
    @Deprecated
    public static IRubyObject aValueSplat(IRubyObject value, Ruby runtime) {
        return aValueSplat(runtime, value);
    }
    @Deprecated
    public static IRubyObject aryToAry(IRubyObject value, Ruby runtime) {
        return aryToAry(runtime, value);
    }
}
