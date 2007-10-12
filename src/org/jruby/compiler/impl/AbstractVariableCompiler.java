/*
 * HeapBasedVariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:23:05 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public abstract class AbstractVariableCompiler implements VariableCompiler {
    protected static final CodegenUtils cg = CodegenUtils.cg;
    protected SkinnyMethodAdapter method;
    protected StandardASMCompiler.AbstractMethodCompiler methodCompiler;
    protected int argsIndex;
    protected int closureIndex;
    protected Arity arity;

    public AbstractVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
            SkinnyMethodAdapter method,
            int argsIndex,
            int closureIndex) {
        this.methodCompiler = methodCompiler;
        this.method = method;
        this.argsIndex = argsIndex;
        this.closureIndex = closureIndex;
    }
    
    public SkinnyMethodAdapter getMethodAdapter() {
        return this.method;
    }

    public void setMethodAdapter(SkinnyMethodAdapter sma) {
        this.method = sma;
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

    public void checkMethodArity(int requiredArgs, int optArgs, int restArg) {
        // check arity
        Label arityError = new Label();
        Label noArityError = new Label();

        if (restArg != -1) {
            if (requiredArgs > 0) {
                // just confirm minimum args provided
                method.aload(argsIndex);
                method.arraylength();
                method.ldc(requiredArgs);
                method.if_icmplt(arityError);
            }
        } else if (optArgs > 0) {
            if (requiredArgs > 0) {
                // confirm minimum args provided
                method.aload(argsIndex);
                method.arraylength();
                method.ldc(requiredArgs);
                method.if_icmplt(arityError);
            }

            // confirm maximum not greater than optional
            method.aload(argsIndex);
            method.arraylength();
            method.ldc(requiredArgs + optArgs);
            method.if_icmpgt(arityError);
        } else {
            // just confirm args length == required
            method.aload(argsIndex);
            method.arraylength();
            method.ldc(requiredArgs);
            method.if_icmpne(arityError);
        }

        method.go_to(noArityError);

        method.label(arityError);
        methodCompiler.loadRuntime();
        method.aload(argsIndex);
        method.ldc(requiredArgs);
        method.ldc(requiredArgs + optArgs);
        method.invokestatic(cg.p(Arity.class), "checkArgumentCount", cg.sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
        method.pop();

        method.label(noArityError);
    }

    public void assignMethodArguments(
            Object requiredArgs,
            int requiredArgsCount,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            ClosureCallback restAssignment,
            ClosureCallback blockAssignment) {
        // load arguments array
        method.aload(argsIndex);
        
        // NOTE: This assumes arity has already been confirmed, and does not re-check
        
        // first, iterate over all required args
        int currentArgElement = 0;
        for (; currentArgElement < requiredArgsCount; currentArgElement++) {
            // extract item from array
            method.dup(); // dup the original array
            method.ldc(new Integer(currentArgElement)); // index for the item
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
            method.ldc(new Integer(currentArgElement));
            method.if_icmple(noMoreArrayElements); // if length <= start, end loop

            // extract item from array
            method.dup(); // dup the original array
            method.ldc(new Integer(currentArgElement)); // index for the item
            method.arrayload();
            optGivenAssignment.nextValue(methodCompiler, optArgs, optArgElement);
            method.go_to(doneWithElement);

            // otherwise no items left available, use the code from nilCallback
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
            method.ldc(new Integer(currentArgElement));
            method.if_icmple(emptyArray); // if length <= start, end loop

            // assign remaining elements as an array for rest args
            method.dup(); // dup the original array object
            methodCompiler.loadRuntime();
            method.ldc(currentArgElement);
            methodCompiler.invokeUtilityMethod("createSubarray", cg.sig(RubyArray.class, IRubyObject[].class, Ruby.class, int.class));
            method.go_to(readyForArgs);

            // create empty array
            method.label(emptyArray);
            methodCompiler.createEmptyArray();

            // assign rest args
            method.label(readyForArgs);
            restAssignment.compile(methodCompiler);
            //consume leftover assigned value
            method.pop();
        }
        
        // done with arguments array
        method.pop();
        
        // block argument assignment, if there's a block arg
        if (blockAssignment != null) {
            methodCompiler.loadRuntime();
            method.aload(closureIndex);

            methodCompiler.invokeUtilityMethod("processBlockArgument", cg.sig(IRubyObject.class, cg.params(Ruby.class, Block.class)));
            blockAssignment.compile(methodCompiler);
            method.pop();
        }
    }
}
