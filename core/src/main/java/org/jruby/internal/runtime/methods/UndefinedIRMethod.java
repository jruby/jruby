package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 11/6/15.
 */
public class UndefinedIRMethod extends DynamicMethod implements Compilable<InterpreterContext> {
    public static final UndefinedIRMethod INSTANCE = new UndefinedIRMethod();

    /**
     * Constructor for the one UndefinedMethod instance.
     */
    private UndefinedIRMethod() {
    }

    /**
     * The one implementation of call, which throws an exception because
     * UndefinedMethod can't be invoked.
     *
     * @see DynamicMethod
     */
    public IRubyObject call(ThreadContext context, CallSite callsite, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        throw new UnsupportedOperationException("BUG: invoking UndefinedMethod.call; report at http://bugs.jruby.org");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        throw new UnsupportedOperationException("BUG: invoking UndefinedMethod.call; report at http://bugs.jruby.org");
    }

    /**
     * A dummy implementation of dup that just returns the singleton instance.
     *
     * @return The singleton instance
     */
    public DynamicMethod dup() {
        return INSTANCE;
    }

    /**
     * Retrieve the singleton instance.
     *
     * @return The singleton instance
     */
    public static UndefinedIRMethod getInstance() {
        return INSTANCE;
    }

    /**
     * Dummy override of setImplementationClass that does nothing.
     *
     * @param implClass Ignored
     */
    @Override
    public void setImplementationClass(RubyModule implClass) {
        // UndefinedMethod should be immutable
    }

    /**
     * Dummy implementation of setVisibility that does nothing.
     *
     * @param visibility Ignored
     */
    @Override
    public void setVisibility(Visibility visibility) {
        // UndefinedMethod should be immutable
    }

    /**
     * Dummy implementation of setCallConfig that does nothing.
     *
     * @param callConfig Ignored
     */
    @Override
    public void setCallConfig(CallConfiguration callConfig) {
        // UndefinedMethod should be immutable
    }

    /**
     * UndefinedMethod is always visible because it's only used as a marker for
     * missing or undef'ed methods.
     *
     * @param caller The calling object
     * @param callType The type of call
     * @return true always
     */
    @Override
    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        return true;
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
    public String getFile() {
        return null;
    }

    @Override
    public int getLine() {
        return 0;
    }

    @Override
    public void setInterpreterContext(InterpreterContext interpreterContext) {

    }
}
