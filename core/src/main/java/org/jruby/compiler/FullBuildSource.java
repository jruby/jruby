package org.jruby.compiler;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;

/**
 * Blocks and methods both share same full build mechanism so they implement this to be buildable.
 */
public interface FullBuildSource {
    public void setCallCount(int count);
    public void switchToFullBuild(InterpreterContext interpreterContext);
    public IRScope getIRScope();
    public InterpreterContext ensureInstrsReady();
    public String getName();
    public String getFile();
    public int getLine();
    public RubyModule getImplementationClass();
}
