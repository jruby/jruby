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
        RubyModule enumerableModule = runtime.getModule("Enumerable");
        RubyClass object = runtime.getObject();
        RubyClass enumeratorClass = enumerableModule.defineClassUnder("Enumerator", object, ENUMERATOR_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyEnumerator.class);

        enumeratorClass.includeModule(enumerableModule);
        enumeratorClass.getMetaClass().defineMethod("new", callbackFactory.getOptSingletonMethod("new_instance"));
        enumeratorClass.defineMethod("initialize", callbackFactory.getOptSingletonMethod("initialize"));
        enumeratorClass.defineMethod("each", callbackFactory.getOptSingletonMethod("each"));

        object.defineMethod("to_enum", callbackFactory.getOptSingletonMethod("o_to_enum"));
        object.defineMethod("enum_for", callbackFactory.getOptSingletonMethod("o_to_enum"));

        enumerableModule.defineMethod("enum_with_index", callbackFactory.getSingletonMethod("each_with_index"));
        enumerableModule.defineMethod("each_slice", callbackFactory.getSingletonMethod("each_slice",IRubyObject.class));
        enumerableModule.defineMethod("enum_slice", callbackFactory.getSingletonMethod("enum_slice",IRubyObject.class));
        enumerableModule.defineMethod("each_cons", callbackFactory.getSingletonMethod("each_cons",IRubyObject.class));
        enumerableModule.defineMethod("enum_cons", callbackFactory.getSingletonMethod("enum_cons",IRubyObject.class));
    }

    private RubyEnumerator(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static IRubyObject new_instance(IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass)self;
        RubyEnumerator result = (RubyEnumerator) klass.allocate();
        result.callInit(args, block);
        return result;
    }

    public static IRubyObject initialize(IRubyObject self, IRubyObject[] args, Block block) {
        return ((RubyEnumerator) self).initialize(self.getRuntime().getCurrentContext(), args, block);
    }

    public static IRubyObject each(IRubyObject self, IRubyObject[] args, Block block) {
        return ((RubyEnumerator) self).each(self.getRuntime().getCurrentContext(), args, block);
    }

    public static IRubyObject o_to_enum(IRubyObject self, IRubyObject[] args, Block block) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];

        newArgs[0] = self;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return self.getRuntime().getModule("Enumerable").getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", newArgs);
    }

    public static IRubyObject each_with_index(IRubyObject self, Block block) {
        return self.getRuntime().getModule("Enumerable").getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                               new IRubyObject[] { self, self.getRuntime().newSymbol("each_with_index") });
    }

    public static IRubyObject each_slice(IRubyObject self, IRubyObject arg, Block block) {
        long sliceSize = arg.convertToInteger().getLongValue();

        if (sliceSize <= 0L) {
            throw self.getRuntime().newArgumentError("invalid slice size");
        } 

        SlicedBlockCallback sliceBlock = new SlicedBlockCallback(self.getRuntime(), block, sliceSize);

        RubyEnumerable.callEach(self.getRuntime().getCurrentContext(), self, self.getMetaClass(), sliceBlock);
            
        if (sliceBlock.hasLeftovers()) {
            sliceBlock.yieldLeftovers(self.getRuntime().getCurrentContext());
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject each_cons(IRubyObject self, IRubyObject arg, Block block) {
        long consecutiveSize = arg.convertToInteger().getLongValue();

        if (consecutiveSize <= 0L) {
            throw self.getRuntime().newArgumentError("invalid size");
        }

        RubyEnumerable.callEach(self.getRuntime().getCurrentContext(), self, self.getMetaClass(), 
                                new ConsecutiveBlockCallback(self.getRuntime(), block, consecutiveSize));

        return self.getRuntime().getNil();
    }

    public static IRubyObject enum_slice(IRubyObject self, IRubyObject arg, Block block) {
        return self.getRuntime().getModule("Enumerable").getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                                     new IRubyObject[] { self, self.getRuntime().newSymbol("each_slice"), arg });
    }

    public static IRubyObject enum_cons(IRubyObject self, IRubyObject arg, Block block) {
        return self.getRuntime().getModule("Enumerable").getConstant("Enumerator").callMethod(self.getRuntime().getCurrentContext(), "new", 
                               new IRubyObject[] { self, self.getRuntime().newSymbol("each_cons"), arg });
    }

    /** Primes the instance. Little validation is done at this stage */
    private IRubyObject initialize(ThreadContext tc, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(tc.getRuntime(), args, 1, -1);
           
        object = args[0];
        methodArgs = new IRubyObject[Math.max(0, args.length - 2)];

        if (args.length >= 2) {
            method = args[1];
        } else {
            method = RubySymbol.newSymbol(tc.getRuntime(), "each");
        }

        if (args.length >= 3) {
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
    private IRubyObject each(ThreadContext tc, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(tc.getRuntime(), args, 0, 0);

        return object.callMethod(tc, method.asSymbol(), methodArgs, block);
    }

    /** Block callback for slicing the results of calling the client block */
    public static class SlicedBlockCallback implements BlockCallback {
        protected RubyArray slice;
        protected final long sliceSize;
        protected final Block clientBlock;
        protected final Ruby runtime;

        public SlicedBlockCallback(Ruby runtime, Block clientBlock, long sliceSize) {
            this.runtime = runtime;
            this.clientBlock = clientBlock;
            this.sliceSize = sliceSize;
            this.slice = RubyArray.newArray(runtime, sliceSize);
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            if (args.length > 1) {
                slice.append(RubyArray.newArray(runtime, args));
            } else {
                slice.append(args[0]);
            }

            if (slice.getLength() == sliceSize) {
                //no need to dup slice as we create a new one momentarily
                clientBlock.call(context, new IRubyObject[] { slice });

                slice = RubyArray.newArray(runtime, sliceSize);
            }

            return runtime.getNil();
        }


        /** Slice may be over but there weren't enough items to make the slice */
        public boolean hasLeftovers() {
            return (slice.getLength() > 0) && (slice.getLength() < sliceSize);
        }

        /** Pass slice dregs on to client blcok */
        public void yieldLeftovers(ThreadContext context) {
            clientBlock.call(context, new IRubyObject[] { slice });
        }
    }

    /** Block callback for viewing consecutive results from calling the client block */
    public static class ConsecutiveBlockCallback implements BlockCallback {
        protected final RubyArray cont;
        protected final long contSize;
        protected final Block clientBlock;
        protected final Ruby runtime;


        public ConsecutiveBlockCallback(Ruby runtime, Block clientBlock, long contSize) {
            this.runtime = runtime;
            this.clientBlock = clientBlock;
            this.contSize = contSize;
            this.cont = RubyArray.newArray(runtime, contSize);
        }

        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            if (cont.getLength() == contSize) {
                cont.shift();
            }

            if (args.length > 1) {
                cont.append(RubyArray.newArray(runtime, args));
            } else {
                cont.append(args[0]);
            }

            if (cont.getLength() == contSize) {
                //dup so we are in control of the array
                clientBlock.call(context, new IRubyObject[] { cont.dup() });
            }

            return runtime.getNil();
        }
    }
}
