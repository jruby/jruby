package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

/**
 * Represents a method wrapped in a block (proc), as in Method#to_proc.
 */
public class MethodBlockBody extends ContextAwareBlockBody {
    private final CacheEntry entry;
    private final ArgumentDescriptor[] argsDesc;
    private final IRubyObject receiver;
    private final String originName;
    private final String file;
    private final int line;

    public MethodBlockBody(StaticScope staticScope, Signature signature, CacheEntry entry, ArgumentDescriptor[] argsDesc, IRubyObject receiver, RubyModule originModule, String originName, String file, int line) {
        super(staticScope, signature);

        // TODO: It's possible this should be dynamically dispatching every time...
        this.entry = entry;
        this.argsDesc = argsDesc;
        this.receiver = receiver;
        this.originName = originName;
        this.file = file;
        this.line = line;
    }

    public static Block createMethodBlock(ThreadContext context, MethodBlockBody body) {
        DynamicMethod method = body.entry.method;
        RubyModule module = method.getImplementationClass();
        Frame frame = new Frame(context.threadID);

        frame.setKlazz(module);
        frame.setName(method.getName());
        frame.setSelf(body.receiver);
        frame.setVisibility(method.getVisibility());

        Binding binding = new Binding(
                frame,
                body.getStaticScope().getDummyScope(),
                method.getName(), body.getFile(), body.getLine());

        return new Block(body, binding);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        args = prepareArgumentsForCall(context, args, block.type);

        return entry.method.call(context, receiver, entry.sourceModule, originName, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        args = prepareArgumentsForCall(context, args, block.type);

        return entry.method.call(context, receiver, entry.sourceModule, originName, args, blockArg);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        IRubyObject[] realArgs = Helpers.restructureBlockArgs(context, value, getSignature(), block.type);
        return entry.method.call(context, receiver, entry.sourceModule, originName, realArgs, Block.NULL_BLOCK);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        return entry.method.call(context, receiver, entry.sourceModule, originName, args, Block.NULL_BLOCK);
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
