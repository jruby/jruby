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

package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.VariableCompiler;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;

import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public abstract class AbstractVariableCompiler implements VariableCompiler {
    protected SkinnyMethodAdapter method;
    protected BaseBodyCompiler methodCompiler;
    protected int argsIndex;
    protected int tempVariableIndex;
    protected Arity arity;
    protected StaticScope scope;
    protected boolean specificArity;

    public AbstractVariableCompiler(
            BaseBodyCompiler methodCompiler,
            SkinnyMethodAdapter method,
            StaticScope scope,
            boolean specificArity,
            int argsIndex,
            int firstTempIndex) {
        this.methodCompiler = methodCompiler;
        this.method = method;
        this.argsIndex = argsIndex;
        this.tempVariableIndex = firstTempIndex;
        this.scope = scope;
        this.specificArity = specificArity;
    }
    
    public SkinnyMethodAdapter getMethodAdapter() {
        return this.method;
    }

    public void setMethodAdapter(SkinnyMethodAdapter sma) {
        this.method = sma;
    }

    public void assignLastLine() {
        methodCompiler.loadRuntime();
        method.swap();
        methodCompiler.loadThreadContext();
        method.swap();
        method.invokestatic(p(RuntimeHelpers.class), "setLastLine", sig(IRubyObject.class, Ruby.class, ThreadContext.class, IRubyObject.class));
    }

    public void assignLastLine(CompilerCallback value) {
        methodCompiler.loadRuntime();
        methodCompiler.loadThreadContext();
        value.call(methodCompiler);
        method.invokestatic(p(RuntimeHelpers.class), "setLastLine", sig(IRubyObject.class, Ruby.class, ThreadContext.class, IRubyObject.class));
    }

    public void retrieveLastLine() {
        methodCompiler.loadRuntime();
        methodCompiler.loadThreadContext();
        method.invokestatic(p(RuntimeHelpers.class), "getLastLine", sig(IRubyObject.class, Ruby.class, ThreadContext.class));
    }

    public void assignBackRef() {
        methodCompiler.loadRuntime();
        method.swap();
        methodCompiler.loadThreadContext();
        method.swap();
        method.invokestatic(p(RuntimeHelpers.class), "setBackref", sig(IRubyObject.class, Ruby.class, ThreadContext.class, IRubyObject.class));
    }    

    public void assignBackRef(CompilerCallback value) {
        methodCompiler.loadRuntime();
        methodCompiler.loadThreadContext();
        value.call(methodCompiler);
        method.invokestatic(p(RuntimeHelpers.class), "setBackref", sig(IRubyObject.class, Ruby.class, ThreadContext.class, IRubyObject.class));
    }    

    public void retrieveBackRef() {
        methodCompiler.loadRuntime();
        methodCompiler.loadThreadContext();
        method.invokestatic(p(RuntimeHelpers.class), "getBackref", sig(IRubyObject.class, Ruby.class, ThreadContext.class));
    }

    public void checkMethodArity(int requiredArgs, int optArgs, int restArg) {
        if (specificArity) {
            // do nothing; arity check is done before call
        } else {
            boolean needsError = false;
            if (restArg != -1) {
                if (requiredArgs > 0) {
                    needsError = true;
                    // just confirm minimum args provided
                    methodCompiler.loadRuntime();
                    method.aload(argsIndex);
                    method.pushInt(requiredArgs);
                    method.pushInt(-1);
                }
            } else if (optArgs > 0) {
                needsError = true;
                methodCompiler.loadRuntime();
                method.aload(argsIndex);
                method.pushInt(requiredArgs);
                method.pushInt(requiredArgs + optArgs);
            } else {
                needsError = true;
                // just confirm args length == required
                methodCompiler.loadRuntime();
                method.aload(argsIndex);
                method.pushInt(requiredArgs);
                method.pushInt(requiredArgs);
            }

            if (needsError) {
                method.invokestatic(p(Arity.class), "raiseArgumentError", sig(void.class, Ruby.class, IRubyObject[].class, int.class, int.class));
            }
        }
    }

    public void assignMethodArguments(
            Object requiredArgs,
            int requiredArgsCount,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            CompilerCallback restAssignment,
            CompilerCallback blockAssignment) {
        if (specificArity) {
            int currentArgElement = 0;
            for (; currentArgElement < scope.getRequiredArgs(); currentArgElement++) {
                method.aload(argsIndex + currentArgElement);
                requiredAssignment.nextValue(methodCompiler, requiredArgs, currentArgElement);
            }
        } else {
            if (requiredArgsCount > 0 || optArgsCount > 0 || restAssignment != null) {
                // first, iterate over all required args
                int currentArgElement = 0;
                for (; currentArgElement < requiredArgsCount; currentArgElement++) {
                    // extract item from array
                    method.aload(argsIndex);
                    method.pushInt(currentArgElement); // index for the item
                    method.arrayload();
                    requiredAssignment.nextValue(methodCompiler, requiredArgs, currentArgElement);
                }

                if (optArgsCount > 0) {
                    // prepare labels for opt logic
                    Label doneWithOpt = new Label();
                    Label[] optLabels = new Label[optArgsCount];
                    for (int i = 0; i < optLabels.length; i ++) optLabels[i] = new Label();

                    // next, iterate over all optional args, until no more arguments
                    for (int optArgElement = 0; optArgElement < optArgsCount; currentArgElement++, optArgElement++) {
                        method.aload(argsIndex);
                        method.pushInt(currentArgElement); // index for the item
                        methodCompiler.invokeUtilityMethod("elementOrNull", sig(IRubyObject.class, IRubyObject[].class, int.class));
                        method.dup();
                        method.ifnull(optLabels[optArgElement]);

                        optGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
                    }
                    method.go_to(doneWithOpt);

                    // now logic for each optional value
                    for (int optArgElement = 0; optArgElement < optArgsCount; optArgElement++) {
                        // otherwise no items left available, use the code for default
                        method.label(optLabels[optArgElement]);
                        optNotGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
                    }

                    // pop extra failed value from first cycle and we're done
                    method.pop();
                    method.label(doneWithOpt);
                }

                // if there's args left and we want them, assign to rest arg
                if (restAssignment != null) {
                    // assign remaining elements as an array for rest args (or empty array)
                    method.aload(argsIndex);
                    methodCompiler.loadRuntime();
                    method.pushInt(currentArgElement);
                    methodCompiler.invokeUtilityMethod("createSubarray", sig(RubyArray.class, IRubyObject[].class, Ruby.class, int.class));
                    restAssignment.call(methodCompiler);
                }
            }
        }
        
        // block argument assignment, if there's a block arg
        if (blockAssignment != null) {
            methodCompiler.loadRuntime();
            method.aload(methodCompiler.getClosureIndex());

            methodCompiler.invokeUtilityMethod("processBlockArgument", sig(IRubyObject.class, params(Ruby.class, Block.class)));
            blockAssignment.call(methodCompiler);
        }
    }

    public void assignMethodArguments19(
            Object preArgs,
            int preArgsCount,
            Object postArgs,
            int postArgsCount,
            int postArgsIndex,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            CompilerCallback restAssignment,
            CompilerCallback blockAssignment) {
        
        if (specificArity) {
            int currentArgElement = 0;
            for (; currentArgElement < scope.getRequiredArgs(); currentArgElement++) {
                method.aload(argsIndex + currentArgElement);
                requiredAssignment.nextValue(methodCompiler, preArgs, currentArgElement);
            }
        } else {
            if (preArgsCount > 0 || postArgsCount > 0 || optArgsCount > 0 || restAssignment != null) {
                // first, iterate over all pre args
                int currentArgElement = 0;
                for (; currentArgElement < preArgsCount; currentArgElement++) {
                    // extract item from array
                    method.aload(argsIndex);
                    method.pushInt(currentArgElement); // index for the item
                    // this could probably be more efficient, bailing out on assigning args past the end?
                    methodCompiler.loadNil();
                    methodCompiler.invokeUtilityMethod("elementOrNil", sig(IRubyObject.class, IRubyObject[].class, int.class, IRubyObject.class));
                    requiredAssignment.nextValue(methodCompiler, preArgs, currentArgElement);
                }

                // then optional args
                if (optArgsCount > 0) {
                    // prepare labels for opt logic
                    Label doneWithOpt = new Label();
                    Label[] optLabels = new Label[optArgsCount];
                    for (int i = 0; i < optLabels.length; i ++) optLabels[i] = new Label();

                    // next, iterate over all optional args, until no more arguments
                    for (int optArgElement = 0; optArgElement < optArgsCount; currentArgElement++, optArgElement++) {
                        method.aload(argsIndex);
                        method.pushInt(currentArgElement); // index for the item
                        methodCompiler.invokeUtilityMethod("elementOrNull", sig(IRubyObject.class, IRubyObject[].class, int.class));
                        method.dup();
                        method.ifnull(optLabels[optArgElement]);

                        optGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
                    }
                    method.go_to(doneWithOpt);

                    // now logic for each optional value
                    for (int optArgElement = 0; optArgElement < optArgsCount; optArgElement++) {
                        // otherwise no items left available, use the code for default
                        method.label(optLabels[optArgElement]);
                        optNotGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
                    }

                    // pop extra failed value from first cycle and we're done
                    method.pop();
                    method.label(doneWithOpt);
                }

                // if rest args, excluding post args
                if (restAssignment != null) {
                    // assign remaining elements as an array for rest args (or empty array)
                    method.aload(argsIndex);
                    methodCompiler.loadRuntime();
                    method.pushInt(currentArgElement);
                    method.pushInt(postArgsCount);
                    methodCompiler.invokeUtilityMethod("createSubarray", sig(RubyArray.class, IRubyObject[].class, Ruby.class, int.class, int.class));
                    restAssignment.call(methodCompiler);
                }

                // finally, post args
                for (; currentArgElement < postArgsCount; currentArgElement++) {
                    // extract item from array
                    method.aload(argsIndex);
                    method.pushInt(currentArgElement); // index for the item
                    // this could probably be more efficient, bailing out on assigning args past the end?
                    methodCompiler.loadNil();
                    methodCompiler.invokeUtilityMethod("elementOrNil", sig(IRubyObject.class, IRubyObject[].class, int.class, IRubyObject.class));
                    requiredAssignment.nextValue(methodCompiler, postArgs, currentArgElement);
                }
            }
        }

        // block argument assignment, if there's a block arg
        if (blockAssignment != null) {
            methodCompiler.loadRuntime();
            method.aload(methodCompiler.getClosureIndex());

            methodCompiler.invokeUtilityMethod("processBlockArgument", sig(IRubyObject.class, params(Ruby.class, Block.class)));
            blockAssignment.call(methodCompiler);
        }
    }
        
    public int grabTempLocal() {
        return tempVariableIndex++;
    }

    public void setTempLocal(int index) {
        method.astore(index);
    }

    public void getTempLocal(int index) {
        method.aload(index);
    }

    public void releaseTempLocal() {
        tempVariableIndex--;
    }

    protected void assignHeapLocal(CompilerCallback value, int depth, int index, boolean expr) {
        switch (index) {
        case 0:
            unwrapParentScopes(depth);
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 1:
            unwrapParentScopes(depth);
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 2:
            unwrapParentScopes(depth);
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 3:
            unwrapParentScopes(depth);
            value.call(methodCompiler);
            method.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        default:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.pushInt(index);
            value.call(methodCompiler);
            method.pushInt(depth);
            method.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
        }

        if (!expr) {
            // not an expression, don't want result; pop it
            method.pop();
        }
    }

    protected void assignHeapLocal(int depth, int index, boolean expr) {
        
        switch (index) {
        case 0:
            unwrapParentScopes(depth);
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 1:
            unwrapParentScopes(depth);
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 2:
            unwrapParentScopes(depth);
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        case 3:
            unwrapParentScopes(depth);
            method.swap();
            method.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, params(IRubyObject.class)));
            break;
        default:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.swap();
            method.pushInt(index);
            method.pushInt(depth);
            method.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, params(IRubyObject.class, Integer.TYPE, Integer.TYPE)));
        }

        if (!expr) {
            // not an expression, don't want result; pop it
            method.pop();
        }
    }

    protected void retrieveHeapLocal(int depth, int index) {
        switch (index) {
        case 0:
            unwrapParentScopes(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueZeroDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 1:
            unwrapParentScopes(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueOneDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 2:
            unwrapParentScopes(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueTwoDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        case 3:
            unwrapParentScopes(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueThreeDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
            break;
        default:
            method.aload(methodCompiler.getDynamicScopeIndex());
            method.pushInt(index);
            method.pushInt(depth);
            methodCompiler.loadNil();
            method.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, params(Integer.TYPE, Integer.TYPE, IRubyObject.class)));
        }
    }

    protected void unwrapParentScopes(int depth) {
        // unwrap scopes to appropriate depth
        method.aload(methodCompiler.getDynamicScopeIndex());
        while (depth > 0) {
            method.invokevirtual(p(DynamicScope.class), "getNextCapturedScope", sig(DynamicScope.class));
            depth--;
        }
    }
}
