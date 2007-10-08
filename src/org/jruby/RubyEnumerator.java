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
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of Ruby's Enumerator module.
 */
public class RubyEnumerator extends RubyObject {
    /** target for each operation */
    private IRubyObject object;
    
    /** method to invoke for each operation */
    private IRubyObject method;
    
    /** args to each method */
    private IRubyObject[] methodArgs;
    
    private static ObjectAllocator ENUMERATOR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyEnumerator(runtime, klass);
        }
    };

    public static void defineEnumerator(Ruby runtime) {
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyEnumerator.class);

        RubyModule kernel = runtime.getKernel();
        kernel.defineAnnotatedMethod(RubyEnumerator.class, "obj_to_enum", callbackFactory);

        RubyModule enm = runtime.getClassFromPath("Enumerable");
        enm.defineAnnotatedMethod(RubyEnumerator.class, "each_with_index", callbackFactory);
        enm.defineAnnotatedMethod(RubyEnumerator.class, "each_slice", callbackFactory);
        enm.defineAnnotatedMethod(RubyEnumerator.class, "enum_slice", callbackFactory);
        enm.defineAnnotatedMethod(RubyEnumerator.class, "each_cons", callbackFactory);
        enm.defineAnnotatedMethod(RubyEnumerator.class, "enum_cons", callbackFactory);

        RubyClass enmr = enm.defineClassUnder("Enumerator", runtime.getObject(), ENUMERATOR_ALLOCATOR);

        enmr.includeModule(enm);

        enmr.defineAnnotatedMethod(RubyEnumerator.class, "initialize", callbackFactory);
        enmr.defineAnnotatedMethod(RubyEnumerator.class, "each", callbackFactory);
    }

    @JRubyMethod(name = "to_enum", name2 = "enum_for", optional = 1, rest = true, frame = true)
    public static IRubyObject obj_to_enum(IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        newArgs[0] = self;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return self.getRuntime().getEnumerable().getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", newArgs);
    }

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
        object = method = runtime.getNil();
    }

    @JRubyMethod(name = "initialize", required = 1, rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, -1);
        object = args[0];
        method = args.length > 1 ? args[1] : getRuntime().newSymbol("each");
        if (args.length > 2) {
            methodArgs = new IRubyObject[Math.max(0, args.length - 2)];
            System.arraycopy(args, 2, methodArgs, 0, args.length - 2);
        } else {
            methodArgs = new IRubyObject[0];
        }
        return this;
    }

    /**
     * Send current block and supplied args to method on target. According to MRI
     * Block may not be given and "each" should just ignore it and call on through to
     * underlying method.
     */
    @JRubyMethod(name = "each", frame = true)
    public IRubyObject each(Block block) {
        return object.callMethod(getRuntime().getCurrentContext(), method.asSymbol(), methodArgs, block);
    }

    @JRubyMethod(name = "enum_with_index")
    public static IRubyObject each_with_index(IRubyObject self) {
        return self.getRuntime().getEnumerable().getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                               new IRubyObject[] { self, self.getRuntime().newSymbol("each_with_index") });
    }

    @JRubyMethod(name = "each_slice", required = 1, frame = true)
    public static IRubyObject each_slice(IRubyObject self, IRubyObject arg, final Block block) {
        final int size = (int)RubyNumeric.num2long(arg);

        if (size <= 0) throw self.getRuntime().newArgumentError("invalid slice size");

        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result[] = new RubyArray[]{runtime.newArray(size)};

        RubyEnumerable.callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                result[0].append(largs[0]);
                if (result[0].size() == size) {
                    block.yield(context, result[0]);
                    result[0] = runtime.newArray(size);
                }
                return runtime.getNil();
            }
        });

        if (result[0].size() > 0) block.yield(context, result[0]);
        return self.getRuntime().getNil();
    }

    @JRubyMethod(name = "each_cons", required = 1, frame = true)
    public static IRubyObject each_cons(IRubyObject self, IRubyObject arg, final Block block) {
        final int size = (int)RubyNumeric.num2long(arg);

        if (size <= 0) throw self.getRuntime().newArgumentError("invalid size");

        final Ruby runtime = self.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();
        final RubyArray result = runtime.newArray(size);

        RubyEnumerable.callEach(runtime, context, self, new BlockCallback() {
            public IRubyObject call(ThreadContext ctx, IRubyObject[] largs, Block blk) {
                if (result.size() == size) result.shift();
                result.append(largs[0]);
                if (result.size() == size) block.yield(context, result.aryDup());
                return runtime.getNil();
            }
        });

        return runtime.getNil();        
    }

    @JRubyMethod(name = "enum_slice", required = 1)
    public static IRubyObject enum_slice(IRubyObject self, IRubyObject arg) {
        return self.getRuntime().getEnumerable().getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                                     new IRubyObject[] { self, self.getRuntime().newSymbol("each_slice"), arg });
    }

    @JRubyMethod(name = "enum_cons", required = 1)
    public static IRubyObject enum_cons(IRubyObject self, IRubyObject arg) {
        return self.getRuntime().getEnumerable().getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                               new IRubyObject[] { self, self.getRuntime().newSymbol("each_cons"), arg });
    }
}
