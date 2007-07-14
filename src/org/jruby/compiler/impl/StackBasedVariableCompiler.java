/*
 * HeapBasedVariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:23:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
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
    
    public void beginMethod(ClosureCallback argsCallback, StaticScope scope) {
        // fill in all vars with null so compiler is happy about future accesses
        method.aconst_null();
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
        throw new NotCompilableException("Stack-based local variables are not applicable to $_ or $~ variables");
    }

    public void retrieveLastLine() {
        throw new NotCompilableException("Stack-based local variables are not applicable to $_ or $~ variables");
    }

    public void retrieveBackRef() {
        throw new NotCompilableException("Stack-based local variables are not applicable to $_ or $~ variables");
    }

    public void processRequiredArgs(Arity arity, int requiredArgs, int optArgs, int restArg) {
        // check arity
        methodCompiler.loadRuntime();
        method.aload(argsIndex);
        method.arraylength();
        method.ldc(new Integer(requiredArgs));
        method.ldc(new Integer(optArgs));
        method.ldc(new Integer(restArg));
        methodCompiler.invokeUtilityMethod("raiseArgumentError", cg.sig(Void.TYPE, cg.params(Ruby.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE)));

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
                method.astore(10 + i + 2);
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
