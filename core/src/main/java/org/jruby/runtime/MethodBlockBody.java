package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a method wrapped in a block (proc), as in Method#to_proc.
 */
public class MethodBlockBody extends ContextAwareBlockBody {
    private final DynamicMethod method;
    private final ArgumentDescriptor[] argsDesc;
    private final IRubyObject receiver;
    private final RubyModule originModule;
    private final String originName;
    private final String file;
    private final int line;

    public MethodBlockBody(StaticScope staticScope, Signature signature, DynamicMethod method, ArgumentDescriptor[] argsDesc, IRubyObject receiver, RubyModule originModule, String originName, String file, int line) {
        super(staticScope, signature);
        this.method = method;
        this.argsDesc = argsDesc;
        this.receiver = receiver;
        this.originModule = originModule;
        this.originName = originName;
        this.file = file;
        this.line = line;
    }

    public static Block createMethodBlock(MethodBlockBody body) {
        RubyModule module = body.method.getImplementationClass();
        Frame frame = new Frame();

        frame.setKlazz(module);
        frame.setName(body.method.getName());
        frame.setSelf(body.receiver);
        frame.setVisibility(body.method.getVisibility());

        Binding binding = new Binding(
                frame,
                body.getStaticScope().getDummyScope(),
                body.method.getName(), body.getFile(), body.getLine());

        return new Block(body, binding);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        args = prepareArgumentsForCall(context, args, type);

        return method.call(context, receiver, originModule, originName, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding,
                            Block.Type type, Block block) {
        args = prepareArgumentsForCall(context, args, type);

        return method.call(context, receiver, originModule, originName, args, block);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        IRubyObject[] realArgs = Helpers.restructureBlockArgs19(value, getSignature(), type, false, false);
        return method.call(context, receiver, originModule, originName, realArgs, Block.NULL_BLOCK);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Block.Type type) {
        return method.call(context, receiver, originModule, originName, args, Block.NULL_BLOCK);
    }

    @Override
    public String getFile() {
        return file;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argsDesc;
    }
}
