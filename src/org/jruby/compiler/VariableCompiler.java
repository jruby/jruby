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
    public void beginClass(ClosureCallback bodyPrep, StaticScope scope);
    public void beginClosure(ClosureCallback argsCallback, StaticScope scope);
    public void assignLocalVariable(int index);
    public void retrieveLocalVariable(int index);
    public void assignLastLine();
    public void retrieveLastLine();
    public void retrieveBackRef();
    public void assignLocalVariable(int index, int depth);
    public void retrieveLocalVariable(int index, int depth);
    public void checkMethodArity(int requiredArgs, int optArgs, int restArg);
    public void assignMethodArguments(
            Object requiredArgs,
            int requiredArgsCount,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            ClosureCallback restAssignment,
            ClosureCallback blockAssignment);
}
