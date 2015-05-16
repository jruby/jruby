package org.jruby.compiler;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;

/**
 * Blocks and methods both share same full build mechanism so they implement this to be buildable.
 */
public interface Compilable<T> {
    public void setCallCount(int count);
    public void completeBuild(T buildResult);
    public IRScope getIRScope();
    public InterpreterContext ensureInstrsReady();
    public String getClassName(ThreadContext context);
    public String getName();
    public String getFile();
    public int getLine();
    public RubyModule getImplementationClass();
}
