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
    protected StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    protected int argsIndex;
    protected int tempVariableIndex;
    protected Arity arity;
    protected StaticScope scope;
    protected boolean specificArity;

    public AbstractVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
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
        // check arity
        Label arityError = new Label();
        Label noArityError = new Label();

        if (specificArity) {
            // do nothing; arity check is done before call
            // FIXME: arity check is not yet done before call :)
        } else {
            boolean needsError = false;
            if (restArg != -1) {
                if (requiredArgs > 0) {
                    needsError = true;
                    // just confirm minimum args provided
                    method.aload(argsIndex);
                    method.arraylength();
                    method.pushInt(requiredArgs);
                    method.if_icmplt(arityError);
                }
            } else if (optArgs > 0) {
                needsError = true;
                if (requiredArgs > 0) {
                    // confirm minimum args provided
                    method.aload(argsIndex);
                    method.arraylength();
                    method.pushInt(requiredArgs);
                    method.if_icmplt(arityError);
                }

                // confirm maximum not greater than optional
                method.aload(argsIndex);
                method.arraylength();
                method.pushInt(requiredArgs + optArgs);
                method.if_icmpgt(arityError);
            } else {
                needsError = true;
                // just confirm args length == required
                method.aload(argsIndex);
                method.arraylength();
                method.pushInt(requiredArgs);
                method.if_icmpne(arityError);
            }

            if (needsError) {
                method.go_to(noArityError);

                method.label(arityError);
                methodCompiler.loadRuntime();
                method.aload(argsIndex);
                method.pushInt(requiredArgs);
                method.pushInt(requiredArgs + optArgs);
                method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
                method.pop();

                method.label(noArityError);
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

                // normal assignment leaves the value; pop it.
                method.pop();
            }
        } else {
            if (requiredArgsCount > 0 || optArgsCount > 0 || restAssignment != null) {
                // load arguments array
                method.aload(argsIndex);

                // NOTE: This assumes arity has already been confirmed, and does not re-check

                // first, iterate over all required args
                int currentArgElement = 0;
                for (; currentArgElement < requiredArgsCount; currentArgElement++) {
                    // extract item from array
                    method.dup(); // dup the original array
                    method.pushInt(currentArgElement); // index for the item
                    method.arrayload();
                    requiredAssignment.nextValue(methodCompiler, requiredArgs, currentArgElement);

                    // normal assignment leaves the value; pop it.
                    method.pop();
                }

                // next, iterate over all optional args, until no more arguments
                for (int optArgElement = 0; optArgElement < optArgsCount; currentArgElement++, optArgElement++) {
                    Label noMoreArrayElements = new Label();
                    Label doneWithElement = new Label();

                    // confirm we're not past the end of the array
                    method.dup(); // dup the original array
                    method.arraylength();
                    method.pushInt(currentArgElement);
                    method.if_icmple(noMoreArrayElements); // if length <= start, end loop

                    // extract item from array
                    method.dup(); // dup the original array
                    method.pushInt(currentArgElement); // index for the item
                    method.arrayload();
                    optGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
                    method.go_to(doneWithElement);

                    // otherwise no items left available, use the code for default
                    method.label(noMoreArrayElements);
                    optNotGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);

                    // end of this element
                    method.label(doneWithElement);
                    // normal assignment leaves the value; pop it.
                    method.pop();
                }

                // if there's args left and we want them, assign to rest arg
                if (restAssignment != null) {
                    Label emptyArray = new Label();
                    Label readyForArgs = new Label();

                    // confirm we're not past the end of the array
                    method.dup(); // dup the original array
                    method.arraylength();
                    method.pushInt(currentArgElement);
                    method.if_icmple(emptyArray); // if length <= start, end loop

                    // assign remaining elements as an array for rest args
                    method.dup(); // dup the original array object
                    methodCompiler.loadRuntime();
                    method.pushInt(currentArgElement);
                    methodCompiler.invokeUtilityMethod("createSubarray", sig(RubyArray.class, IRubyObject[].class, Ruby.class, int.class));
                    method.go_to(readyForArgs);

                    // create empty array
                    method.label(emptyArray);
                    methodCompiler.createEmptyArray();

                    // assign rest args
                    method.label(readyForArgs);
                    restAssignment.call(methodCompiler);
                    //consume leftover assigned value
                    method.pop();
                }

                // done with arguments array
                method.pop();
            }
        }
        
        // block argument assignment, if there's a block arg
        if (blockAssignment != null) {
            methodCompiler.loadRuntime();
            method.aload(methodCompiler.getClosureIndex());

            methodCompiler.invokeUtilityMethod("processBlockArgument", sig(IRubyObject.class, params(Ruby.class, Block.class)));
            blockAssignment.call(methodCompiler);
            method.pop();
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

    protected void assignHeapLocal(CompilerCallback value, int depth, int index) {
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
    }

    protected void assignHeapLocal(int depth, int index) {
        
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
