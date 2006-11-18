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

import org.jruby.internal.runtime.methods.MultiStubMethod;
import org.jruby.internal.runtime.methods.NoopMultiStub;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
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

    public static void defineEnumerator(IRuby runtime) {
        RubyModule enumerableModule = runtime.getModule("Enumerable");
        RubyClass object = runtime.getObject();
        RubyClass enumeratorClass = enumerableModule.defineClassUnder("Enumerator", object);
        RubyEnumeratorStub0 enumeratorStub = RubyEnumeratorStub0.createStub(enumeratorClass, object, enumerableModule);

        enumeratorClass.includeModule(enumerableModule);
        enumeratorClass.addSingletonMethod("new", enumeratorStub.enumerator__new);
        enumeratorClass.addMethod("initialize", enumeratorStub.enumerator__initialize);
        enumeratorClass.addMethod("each", enumeratorStub.enumerator__each);

        object.addMethod("to_enum", enumeratorStub.object__to_enum);
        object.addMethod("enum_for", enumeratorStub.object__to_enum);

        enumerableModule.addMethod("enum_with_index", enumeratorStub.enumerable__enum_with_index);
        enumerableModule.addMethod("each_slice", enumeratorStub.enumerable__each_slice);
        enumerableModule.addMethod("enum_slice", enumeratorStub.enumerable__enum_slice);
        enumerableModule.addMethod("each_cons", enumeratorStub.enumerable__each_cons);
        enumerableModule.addMethod("enum_cons", enumeratorStub.enumerable__enum_cons);
    }

    private RubyEnumerator(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }

    /** Primes the instance. Little validation is done at this stage */
    private IRubyObject initialize(ThreadContext tc, IRubyObject[] args) {
        checkArgumentCount(args, 1, -1);
           
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
    private IRubyObject each(ThreadContext tc, IRubyObject[] args) {
        checkArgumentCount(args, 0, 0);

        boolean blockGiven = tc.isBlockGiven();

        if (blockGiven) {
            tc.setBlockAvailable();
        }

        try {
            return object.callMethod(tc, method.asSymbol(), methodArgs);
        } finally {
            if (blockGiven) {
                tc.clearInBlock();
            }
        }
    }

    /** Block callback for slicing the results of calling the client block */
    public static class SlicedBlockCallback implements BlockCallback {
        protected RubyArray slice;
        protected final long sliceSize;
        protected final Block clientBlock;
        protected final IRuby runtime;

        public SlicedBlockCallback(IRuby runtime, Block clientBlock, long sliceSize) {
            this.runtime = runtime;
            this.clientBlock = clientBlock;
            this.sliceSize = sliceSize;
            this.slice = RubyArray.newArray(runtime, sliceSize);
        }

        public IRubyObject call(IRubyObject[] args, IRubyObject replacementSelf) {
            if (args.length > 1) {
                slice.append(RubyArray.newArray(runtime, args));
            } else {
                slice.append(args[0]);
            }

            if (slice.getLength() == sliceSize) {
                //no need to dup slice as we create a new one momentarily
                clientBlock.call(new IRubyObject[] { slice }, null);

                slice = RubyArray.newArray(runtime, sliceSize);
            }

            return runtime.getNil();
        }


        /** Slice may be over but there weren't enough items to make the slice */
        public boolean hasLeftovers() {
            return (slice.getLength() > 0) && (slice.getLength() < sliceSize);
        }

        /** Pass slice dregs on to client blcok */
        public void yieldLeftovers() {
            clientBlock.call(new IRubyObject[] { slice }, null);
        }
    }

    /** Block callback for viewing consecutive results from calling the client block */
    public static class ConsecutiveBlockCallback implements BlockCallback {
        protected final RubyArray cont;
        protected final long contSize;
        protected final Block clientBlock;
        protected final IRuby runtime;


        public ConsecutiveBlockCallback(IRuby runtime, Block clientBlock, long contSize) {
            this.runtime = runtime;
            this.clientBlock = clientBlock;
            this.contSize = contSize;
            this.cont = RubyArray.newArray(runtime, contSize);
        }

        public IRubyObject call(IRubyObject[] args, IRubyObject replacementSelf) {
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
                clientBlock.call(new IRubyObject[] { cont.dup() }, null);
            }

            return runtime.getNil();
        }
    }

    /** Multi-stub for all enumerator methods */
    public static class RubyEnumeratorStub0 extends NoopMultiStub {
        private final RubyModule enumerator;
        private final IRuby runtime;
        
        public static RubyEnumeratorStub0 createStub(RubyClass enumeratorClass,
                RubyClass objectClass, RubyModule enumerableModule) {
            return new RubyEnumeratorStub0(enumeratorClass, objectClass, enumerableModule);
        }

        public final MultiStubMethod enumerator__new;
        public final MultiStubMethod enumerator__initialize;
        public final MultiStubMethod enumerator__each;
        public final MultiStubMethod object__to_enum;
        public final MultiStubMethod enumerable__each_slice;
        public final MultiStubMethod enumerable__each_cons;
        public final MultiStubMethod enumerable__enum_with_index;
        public final MultiStubMethod enumerable__enum_slice;
        public final MultiStubMethod enumerable__enum_cons;

        private RubyEnumeratorStub0(RubyClass enumeratorClass,
                RubyClass objectClass, RubyModule enumerableModule) {
            enumerator = enumeratorClass;
            runtime = enumeratorClass.getRuntime();
            enumerator__new = new MultiStubMethod(RubyEnumeratorStub0.this, 0, 
                    enumeratorClass, Arity.required(1), Visibility.PUBLIC);
            enumerator__initialize = new MultiStubMethod(RubyEnumeratorStub0.this, 1, 
                    enumeratorClass, Arity.required(1), Visibility.PRIVATE);          
            enumerator__each = new MultiStubMethod(RubyEnumeratorStub0.this, 2, 
                    enumeratorClass, Arity.optional(), Visibility.PUBLIC);
            object__to_enum = new MultiStubMethod(RubyEnumeratorStub0.this, 3, 
                    objectClass, Arity.optional(), Visibility.PUBLIC);
            enumerable__each_slice = new MultiStubMethod(RubyEnumeratorStub0.this, 4, 
                    enumerableModule, Arity.singleArgument(), Visibility.PUBLIC);
            enumerable__each_cons = new MultiStubMethod(RubyEnumeratorStub0.this, 5, 
                    enumerableModule, Arity.singleArgument(), Visibility.PUBLIC);
            enumerable__enum_with_index = new MultiStubMethod(RubyEnumeratorStub0.this, 6, 
                    enumerableModule, Arity.noArguments(), Visibility.PUBLIC);
            enumerable__enum_slice = new MultiStubMethod(RubyEnumeratorStub0.this, 7, 
                    enumerableModule, Arity.singleArgument(), Visibility.PUBLIC);
            enumerable__enum_cons = new MultiStubMethod(RubyEnumeratorStub0.this, 8, 
                    enumerableModule, Arity.singleArgument(), Visibility.PUBLIC);
        }

        /** Enumerable::Enumerator#new */
        public IRubyObject method0(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            RubyEnumerator result = new RubyEnumerator(runtime, (RubyClass)self);
            
            result.callMethod(tc, "initialize", args);
            
            return result;
        }

        /** Enumerable::Enumerator#initialize */
        public IRubyObject method1(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            return ((RubyEnumerator) self).initialize(tc, args);
        }

        /** Enumerable::Enumerator#each */
        public IRubyObject method2(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            return ((RubyEnumerator) self).each(tc, args);
        }

        /** Object#to_enum and Object#enum_for. Just like Enumerable::Enumerator.new(self, arg_0) */
        public IRubyObject method3(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            newArgs[0] = self;
            System.arraycopy(args, 0, newArgs, 1, args.length);

            return enumerator.callMethod(tc, "new", newArgs);
        }

        /** Enumerable:#each_slice */
        public IRubyObject method4(final ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            self.checkArgumentCount(args, 1, 1);

            long sliceSize = args[0].convertToInteger().getLongValue();

            if (sliceSize <= 0L) {
                throw runtime.newArgumentError("invalid slice size");
            } 

            SlicedBlockCallback sliceBlock = new SlicedBlockCallback(runtime, tc.getCurrentBlock(), sliceSize);

            RubyEnumerable.callEach(tc, self, self.getMetaClass(), sliceBlock);

            if (sliceBlock.hasLeftovers()) {
                sliceBlock.yieldLeftovers();
            }

            return runtime.getNil();
        }

        /** Enumerable:#each_cons */
        public IRubyObject method5(final ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            self.checkArgumentCount(args, 1, 1);

            long consecutiveSize = args[0].convertToInteger().getLongValue();

            if (consecutiveSize <= 0L) {
                throw runtime.newArgumentError("invalid size");
            }

            RubyEnumerable.callEach(tc, self, self.getMetaClass(), 
                    new ConsecutiveBlockCallback(runtime, tc.getCurrentBlock(), consecutiveSize));

            return runtime.getNil();
        }

        /** Enumerable#enum_with_index */
        public IRubyObject method6(final ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            self.checkArgumentCount(args, 0, 0);

            return enumerator.callMethod(tc, "new", 
                    new IRubyObject[] { self, runtime.newSymbol("each_with_index") });
        }

        /** Enumerable#enum_slice */
        public IRubyObject method7(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            self.checkArgumentCount(args, 1, 1);

            return enumerator.callMethod(tc, "new", 
                    new IRubyObject[] { self, runtime.newSymbol("each_slice"), args[0] });
        }

        /** Enumerable#enum_cons */
        public IRubyObject method8(ThreadContext tc, IRubyObject self, IRubyObject[] args) {
            self.checkArgumentCount(args, 1, 1);

            return enumerator.callMethod(tc, "new", 
                    new IRubyObject[] { self, runtime.newSymbol("each_cons"), args[0] });
        }
    }
}
