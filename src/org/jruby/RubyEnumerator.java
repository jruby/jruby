/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Michael Studman <me@michaelstudman.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import static org.jruby.CompatVersion.*;
import static org.jruby.runtime.Visibility.*;

/**
 * Implementation of Ruby's Enumerator module.
 */
@JRubyModule(name="Enumerable::Enumerator", include="Enumerable")
public class RubyEnumerator extends RubyObject {
    /** target for each operation */
    private IRubyObject object;

    /** method to invoke for each operation */
    private String method;

    /** args to each method */
    private IRubyObject[] methodArgs;

    public static void defineEnumerator(Ruby runtime) {
        runtime.getKernel().defineAnnotatedMethods(RubyEnumeratorKernel.class);

        RubyModule enm = runtime.getClassFromPath("Enumerable");
        enm.defineAnnotatedMethods(RubyEnumeratorEnumerable.class);

        final RubyClass enmr;
        if (runtime.is1_9()) {
            enmr = runtime.defineClass("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);
        } else {
            enmr = enm.defineClassUnder("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);
        }

        enmr.includeModule(enm);
        enmr.defineAnnotatedMethods(RubyEnumerator.class);
        runtime.setEnumerator(enmr);

        RubyYielder.createYielderClass(runtime);
    }

    private static ObjectAllocator ENUMERATOR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyEnumerator(runtime, klass);
        }
    };

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
        object = runtime.getNil();
        initialize(runtime.getNil(), RubyString.newEmptyString(runtime), IRubyObject.NULL_ARRAY);
    }

    private RubyEnumerator(Ruby runtime, IRubyObject object, IRubyObject method, IRubyObject[]args) {
        super(runtime, runtime.getEnumerator());
        initialize(object, method, args);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method) {
        return new RubyEnumerator(runtime, object, runtime.fastNewSymbol(method), IRubyObject.NULL_ARRAY);
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject arg) {
        return new RubyEnumerator(runtime, object, runtime.fastNewSymbol(method), new IRubyObject[]{arg});
    }

    public static IRubyObject enumeratorize(Ruby runtime, IRubyObject object, String method, IRubyObject[]args) {
        return new RubyEnumerator(runtime, object, runtime.fastNewSymbol(method), args); // TODO: make sure it's really safe to not to copy it
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context) {
        throw context.runtime.newArgumentError(0, 1);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, Block block) {
        if(!block.isGiven()) {
            throw context.runtime.newArgumentError(0, 1);
        }

        // TODO: avoid double lookup
        IRubyObject obj = context.runtime.getModule("JRuby").getClass("Generator").callMethod(context, "new", new IRubyObject[0], block);
        return initialize19(context, obj, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object) {
        return initialize(object, context.runtime.fastNewSymbol("each"), NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, Block block) {
        return initialize(object, context.runtime.fastNewSymbol("each"), NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method) {
        return initialize(object, method, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, Block block) {
        return initialize(object, method, NULL_ARRAY);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg) {
        return initialize(object, method, new IRubyObject[] { methodArg });
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject object, IRubyObject method, IRubyObject methodArg, Block block) {
        return initialize(object, method, new IRubyObject[] { methodArg });
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0: return initialize(context);
            case 1: return initialize(context, args[0]);
            case 2: return initialize(context, args[0], args[1]);
        }

        IRubyObject[] methArgs = new IRubyObject[args.length - 2];
        System.arraycopy(args, 2, methArgs, 0, methArgs.length);
        return initialize(args[0], args[1], methArgs);
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return initialize19(context, block);
            case 1: return initialize19(context, args[0], block);
            case 2: return initialize19(context, args[0], args[1], block);
        }

        IRubyObject[] methArgs = new IRubyObject[args.length - 2];
        System.arraycopy(args, 2, methArgs, 0, methArgs.length);
        return initialize(args[0], args[1], methArgs);
    }

    private IRubyObject initialize(IRubyObject object, IRubyObject method, IRubyObject[] methodArgs) {
        this.object = object;
        this.method = method.asJavaString();
        this.methodArgs = methodArgs;
        setInstanceVariable("@__object__", object);
        setInstanceVariable("@__method__", method);
        setInstanceVariable("@__args__", RubyArray.newArrayNoCopyLight(getRuntime(), methodArgs));
        return this;
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        // JRUBY-5013: Enumerator needs to copy private fields in order to have a valid structure
        RubyEnumerator copy = (RubyEnumerator) super.dup();
        copy.object     = this.object;
        copy.method     = this.method;
        copy.methodArgs = this.methodArgs;
        return copy;
    }

    /**
     * Send current block and supplied args to method on target. According to MRI
     * Block may not be given and "each" should just ignore it and call on through to
     * underlying method.
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        return object.callMethod(context, method, methodArgs, block);
    }

    @JRubyMethod(name = "inspect", compat = RUBY1_9)
    public IRubyObject inspect19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (runtime.isInspecting(this)) return inspect(context, true);

        try {
            runtime.registerInspecting(this);
            return inspect(context, false);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    private IRubyObject inspect(ThreadContext context, boolean recurse) {
        Ruby runtime = context.runtime;
        ByteList bytes = new ByteList();
        bytes.append((byte)'#').append((byte)'<');
        bytes.append(getMetaClass().getName().getBytes());
        bytes.append((byte)':').append((byte)' ');

        if (recurse) {
            bytes.append("...>".getBytes());
            return RubyString.newStringNoCopy(runtime, bytes).taint(context);
        } else {
            boolean tainted = isTaint();
            bytes.append(RubyObject.inspect(context, object).getByteList());
            bytes.append((byte)':');
            bytes.append(method.getBytes());
            if (methodArgs.length > 0) {
                bytes.append((byte)'(');
                for (int i= 0; i < methodArgs.length; i++) {
                    bytes.append(RubyObject.inspect(context, methodArgs[i]).getByteList());
                    if (i < methodArgs.length - 1) {
                        bytes.append((byte)',').append((byte)' ');
                    } else {
                        bytes.append((byte)')');
                    }
                    if (methodArgs[i].isTaint()) tainted = true;
                }
            }
            bytes.append((byte)'>');
            RubyString result = RubyString.newStringNoCopy(runtime, bytes);
            if (tainted) result.setTaint(true);
            return result;
        }
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg) {
        return context.runtime.getEnumerator().callMethod(context, "new", arg);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.invoke(context, context.runtime.getEnumerator(), "new", arg1, arg2);
    }

    protected static IRubyObject newEnumerator(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return RuntimeHelpers.invoke(context, context.runtime.getEnumerator(), "new", arg1, arg2, arg3);
    }

    public static final class RubyEnumeratorKernel {
        @JRubyMethod(name = {"to_enum", "enum_for"})
        public static IRubyObject obj_to_enum(ThreadContext context, IRubyObject self) {
            return newEnumerator(context, self);
        }

        @JRubyMethod(name = {"to_enum", "enum_for"})
        public static IRubyObject obj_to_enum(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return newEnumerator(context, self, arg);
        }

        @JRubyMethod(name = {"to_enum", "enum_for"})
        public static IRubyObject obj_to_enum(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
            return newEnumerator(context, self, arg0, arg1);
        }

        @JRubyMethod(name = {"to_enum", "enum_for"}, optional = 1, rest = true)
        public static IRubyObject obj_to_enum(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            newArgs[0] = self;
            System.arraycopy(args, 0, newArgs, 1, args.length);

            return context.runtime.getEnumerator().callMethod(context, "new", newArgs);
        }
    }

    public static final class RubyEnumeratorEnumerable {
        public static IRubyObject each_slice(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            final int size = RubyNumeric.num2int(arg);
            final Ruby runtime = context.runtime;
            if (size <= 0) throw runtime.newArgumentError("invalid slice size");

            final RubyArray result[] = new RubyArray[]{runtime.newArray(size)};

            RubyEnumerable.callEach(runtime, context, self, Arity.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    result[0].append(largs[0]);
                    if (result[0].size() == size) {
                        block.yield(ctx, result[0]);
                        result[0] = runtime.newArray(size);
                    }
                    return runtime.getNil();
                }
            });

            if (result[0].size() > 0) block.yield(context, result[0]);
            return context.runtime.getNil();
        }

        @JRubyMethod(name = "each_slice")
        public static IRubyObject each_slice19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            return block.isGiven() ? each_slice(context, self, arg, block) : enumeratorize(context.runtime, self, "each_slice", arg);
        }

        @JRubyMethod(name = "enum_slice")
        public static IRubyObject enum_slice19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            return block.isGiven() ? each_slice(context, self, arg, block) : enumeratorize(context.runtime, self, "enum_slice", arg);
        }

        public static IRubyObject each_cons(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            final int size = (int)RubyNumeric.num2long(arg);
            final Ruby runtime = context.runtime;
            if (size <= 0) throw runtime.newArgumentError("invalid size");

            final RubyArray result = runtime.newArray(size);

            RubyEnumerable.callEach(runtime, context, self, Arity.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    if (result.size() == size) result.shift(ctx);
                    result.append(largs[0]);
                    if (result.size() == size) block.yield(ctx, result.aryDup());
                    return runtime.getNil();
                }
            });

            return runtime.getNil();        
        }

        @JRubyMethod(name = "each_cons")
        public static IRubyObject each_cons19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            return block.isGiven() ? each_cons(context, self, arg, block) : enumeratorize(context.runtime, self, "each_cons", arg);
        }

        @JRubyMethod(name = "enum_cons")
        public static IRubyObject enum_cons19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
            return block.isGiven() ? each_cons(context, self, arg, block) : enumeratorize(context.runtime, self, "enum_cons", arg);
        }

        @JRubyMethod(name = "each_with_object", compat = RUBY1_9)
        public static IRubyObject each_with_object(ThreadContext context, IRubyObject self, final IRubyObject arg, final Block block) {
            return with_object_common(context, self, arg, block, "each_with_object");
        }

        @JRubyMethod(name = "with_object", compat = RUBY1_9)
        public static IRubyObject with_object(ThreadContext context, IRubyObject self, final IRubyObject arg, final Block block) {
            return with_object_common(context, self, arg, block, "with_object");
        }

        private static IRubyObject with_object_common(ThreadContext context, IRubyObject self,
                final IRubyObject arg, final Block block, final String rubyMethodName) {
            final Ruby runtime = context.runtime;
            if (!block.isGiven()) return enumeratorize(runtime, self , rubyMethodName, arg);

            RubyEnumerable.callEach(runtime, context, self, Arity.ONE_ARGUMENT, new BlockCallback() {
                public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                    block.call(ctx, new IRubyObject[]{runtime.newArray(largs[0], arg)});
                    return runtime.getNil();
                }
            });
            return arg;
        }
    }
    private static class EachWithIndex implements BlockCallback {
        private int index = 0;
        private final Block block;
        private final Ruby runtime;

        public EachWithIndex(ThreadContext ctx, Block block, int index) {
            this.block = block;
            this.runtime = ctx.runtime;
            this.index = index;
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] iargs, Block block) {
            return this.block.call(context, new IRubyObject[] { runtime.newArray(RubyEnumerable.checkArgs(runtime, iargs), runtime.newFixnum(index++)) });
        }
    }

    private static IRubyObject with_index_common(ThreadContext context, IRubyObject self, 
            final Block block, final String rubyMethodName, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        int index = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if (!block.isGiven()) {
            return arg.isNil() ? enumeratorize(runtime, self , rubyMethodName) :
                enumeratorize(runtime, self , rubyMethodName, runtime.newFixnum(index));
        }

        return RubyEnumerable.callEach(runtime, context, self, new EachWithIndex(context, block, index));
    }

    @JRubyMethod
    public static IRubyObject each_with_index(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "each_with_index", context.runtime.getNil());
    }

    @JRubyMethod(compat = RUBY1_8)
    public static IRubyObject with_index(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "with_index", context.runtime.getNil());
    }

    @JRubyMethod(name = "with_index", compat = RUBY1_9)
    public static IRubyObject with_index19(ThreadContext context, IRubyObject self, final Block block) {
        return with_index_common(context, self, block, "with_index", context.runtime.getNil());
    }

    @JRubyMethod(name = "with_index", compat = RUBY1_9)
    public static IRubyObject with_index19(ThreadContext context, IRubyObject self, IRubyObject arg, final Block block) {
        return with_index_common(context, self, block, "with_index", arg);
    }
}
