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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class StackBasedVariableCompiler implements VariableCompiler {
    private static final CodegenUtils cg = CodegenUtils.cg;
    private SkinnyMethodAdapter method;
    private StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    private int argsIndex; // the index where an IRubyObject[] representing incoming arguments can be found
    private Arity arity;

    public StackBasedVariableCompiler(StandardASMCompiler.AbstractMethodCompiler methodCompiler, SkinnyMethodAdapter method, int argsIndex) {
        this.methodCompiler = methodCompiler;
        this.method = method;
        this.argsIndex = argsIndex;
    }
    
    public SkinnyMethodAdapter getMethodAdapter() {
        return this.method;
    }

    public void setMethodAdapter(SkinnyMethodAdapter sma) {
        this.method = sma;
    }

    public void beginMethod(ClosureCallback argsCallback, StaticScope scope) {
        // fill in all vars with nol so compiler is happy about future accesses
        methodCompiler.loadNil();
        for (int i = 0; i < scope.getNumberOfVariables(); i++) {
            assignLocalVariable(i);
        }
        method.pop();
        
        if (argsCallback != null) {
            argsCallback.compile(methodCompiler);
        }
    }

    public void beginClass(ClosureCallback bodyPrep, StaticScope scope) {
        throw new NotCompilableException("ERROR: stack-based variables should not be compiling class bodies");
    }

    public void beginClosure(ClosureCallback argsCallback, StaticScope scope) {
        // load args[0] which will be the IRubyObject representing block args
        method.aload(argsIndex);
        method.ldc(new Integer(0));
        method.arrayload();

        // load nil into all vars to avoid null/nil checking
        methodCompiler.loadNil();
        for (int i = 0; i < scope.getNumberOfVariables(); i++) {
            assignLocalVariable(i);
        }
        method.pop();
        
        if (argsCallback != null) {
            argsCallback.compile(methodCompiler);
        }
    }

    public void assignLocalVariable(int index) {
        method.dup();

        method.astore(10 + index);
    }

    public void assignLocalVariable(int index, int depth) {
        if (depth == 0) {
            assignLocalVariable(index);
        } else {
            throw new NotCompilableException("Stack-based local variables are not applicable to nested scopes");
        }
    }

    public void retrieveLocalVariable(int index) {
        method.aload(10 + index);
    }

    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
        } else {
            throw new NotCompilableException("Stack-based local variables are not applicable to nested scopes");
        }
    }

    public void assignLastLine() {
        method.dup();

        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentFrame", cg.sig(Frame.class));
        method.swap();
        method.invokevirtual(cg.p(Frame.class), "setLastLine", cg.sig(Void.TYPE, cg.params(IRubyObject.class)));
    }

    public void retrieveLastLine() {
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentFrame", cg.sig(Frame.class));
        method.invokevirtual(cg.p(Frame.class), "getLastLine", cg.sig(IRubyObject.class));
    }

    public void retrieveBackRef() {
        methodCompiler.loadThreadContext();
        methodCompiler.invokeThreadContext("getCurrentFrame", cg.sig(Frame.class));
        method.invokevirtual(cg.p(Frame.class), "getBackRef", cg.sig(IRubyObject.class));
    }

    public void processRequiredArgs(Arity arity, int requiredArgs, int optArgs, int restArg) {
        // check arity
        methodCompiler.loadThreadContext();
        methodCompiler.loadRuntime();
        method.aload(argsIndex);
        method.arraylength();
        method.ldc(new Integer(requiredArgs));
        method.ldc(new Integer(optArgs));
        method.ldc(new Integer(restArg));
        methodCompiler.invokeUtilityMethod("handleArgumentSizes", cg.sig(Void.TYPE, ThreadContext.class, Ruby.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE));

        Label noArgs = new Label();

        // check if args is null
        method.aload(argsIndex);
        method.ifnull(noArgs);

        // check if args length is zero
        method.aload(argsIndex);
        method.arraylength();
        method.ifeq(noArgs);
        
        if (requiredArgs + optArgs == 0 && restArg == 0) {
            // only restarg, just jump to noArgs and it will be processed separately
            method.go_to(noArgs);
        } else {
            // load args array
            method.aload(argsIndex);

            // test whether total args count or actual args given is lower, for copying to dynamic scope
            for (int i = 0; i < requiredArgs; i++) {
                method.dup();
                method.ldc(new Integer(i));
                method.arrayload();
                method.astore(10 + i);
            }
            method.pop();
        }

        method.label(noArgs);

        // push down the argument count of this method
        this.arity = arity;
    }

    public void assignOptionalArgs(Object object, int expectedArgsCount, int size, ArrayCallback optEval) {
        // NOTE: By the time we're here, arity should have already been checked. We proceed without boundschecking.
        // opt args are handled with a switch; the key is how many args we have coming in, and the cases are
        // each opt arg index. The cases fall-through, so remaining opt args are handled.
        throw new NotCompilableException("Stack-based local variables are not applicable to optional arguments (yet)");
        
//        method.aload(argsIndex);
//        method.arraylength();
//
//        Label defaultLabel = new Label();
//        Label[] labels = new Label[size];
//
//        for (int i = 0; i < size; i++) {
//            labels[i] = new Label();
//        }
//
//        method.tableswitch(expectedArgsCount, expectedArgsCount + size - 1, defaultLabel, labels);
//
//        for (int i = 0; i < size; i++) {
//            method.label(labels[i]);
//            optEval.nextValue(methodCompiler, object, i);
//            method.pop();
//        }
//
//        method.label(defaultLabel);
    }

    public void processRestArg(int startIndex, int restArg) {
        throw new NotCompilableException("Stack-based local variables are not applicable to rest arguments (yet)");
//        methodCompiler.loadRuntime();
//        method.aload(argsIndex);
//        method.ldc(new Integer(startIndex));
//
//        methodCompiler.invokeUtilityMethod("processRestArg", cg.sig(void.class, cg.params(Ruby.class, IRubyObject[].class, int.class, IRubyObject[].class, int.class)));
    }

    public void processBlockArgument(int index) {
        throw new NotCompilableException("block args do not compile yet");
    }
}
