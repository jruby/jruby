package org.jruby.compiler;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;

/**
 * Created by enebo on 7/18/16.
 */
public class Uncompilable implements Compilable<InterpreterContext> {
    private final RubyModule implementationClass;

    public Uncompilable(RubyModule implementationClass) {
        this.implementationClass = implementationClass;
    }

    @Override
    public void setCallCount(int count) {

    }

    @Override
    public void completeBuild(InterpreterContext buildResult) {

    }

    @Override
    public IRScope getIRScope() {
        return null;
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        return null;
    }

    @Override
    public String getClassName(ThreadContext context) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getFile() {
        return null;
    }

    @Override
    public int getLine() {
        return 0;
    }

    @Override
    public RubyModule getImplementationClass() {
        return implementationClass;
    }

    @Override
    public void setInterpreterContext(InterpreterContext interpreterContext) {

    }
}
