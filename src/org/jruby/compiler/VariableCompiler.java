/*
 * VariableCompiler.java
 * 
 * Created on Jul 13, 2007, 11:03:45 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;

import org.jruby.compiler.impl.SkinnyMethodAdapter;

/**
 *
 * @author headius
 */
public interface VariableCompiler {
    public SkinnyMethodAdapter getMethodAdapter();
    public void setMethodAdapter(SkinnyMethodAdapter sma);
    public void beginMethod(ClosureCallback argsCallback, StaticScope scope);
    public void beginClosure(ClosureCallback argsCallback, StaticScope scope);
    public void assignLocalVariable(int index);
    public void retrieveLocalVariable(int index);
    public void assignLastLine();
    public void retrieveLastLine();
    public void retrieveBackRef();
    public void assignLocalVariable(int index, int depth);
    public void retrieveLocalVariable(int index, int depth);
    public void processRequiredArgs(Arity arity, int requiredArgs, int optArgs, int restArg);
    public void assignOptionalArgs(Object object, int expectedArgsCount, int size, ArrayCallback optEval);
    public void processRestArg(int startIndex, int restArg);
    public void processBlockArgument(int index);
}
