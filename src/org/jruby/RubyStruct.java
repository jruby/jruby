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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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

import static org.jruby.RubyEnumerator.enumeratorize;


import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ClassIndex;

import java.util.concurrent.Callable;

import static org.jruby.runtime.Visibility.*;

import static org.jruby.javasupport.util.RuntimeHelpers.invokedynamic;
import static org.jruby.runtime.MethodIndex.HASH;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Struct")
public class RubyStruct extends RubyObject {
    private IRubyObject[] values;

    /**
     * Constructor for RubyStruct.
     * @param runtime
     * @param rubyClass
     */
    private RubyStruct(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
        
        int size = RubyNumeric.fix2int(getInternalVariable((RubyClass)rubyClass, "__size__"));

        values = new IRubyObject[size];

        RuntimeHelpers.fillNil(values, runtime);
    }

    public static RubyClass createStructClass(Ruby runtime) {
        RubyClass structClass = runtime.defineClass("Struct", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setStructClass(structClass);
        structClass.index = ClassIndex.STRUCT;
        structClass.includeModule(runtime.getEnumerable());
        structClass.defineAnnotatedMethods(RubyStruct.class);

        return structClass;
    }
    
    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.STRUCT;
    }
    
    private static IRubyObject getInternalVariable(RubyClass type, String internedName) {
        RubyClass structClass = type.getRuntime().getStructClass();
        IRubyObject variable;

        while (type != null && type != structClass) {
            if ((variable = (IRubyObject)type.getInternalVariable(internedName)) != null) {
                return variable;
            }

            type = type.getSuperClass();
        }

        return type.getRuntime().getNil();
    }

    private RubyClass classOf() {
        return getMetaClass() instanceof MetaClass ? getMetaClass().getSuperClass() : getMetaClass();
    }

    private void modify() {
        testFrozen();

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify struct");
        }
    }
    
    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        Ruby runtime = getRuntime();
        int h = getMetaClass().getRealClass().hashCode();

        for (int i = 0; i < values.length; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            h ^= RubyNumeric.num2long(invokedynamic(context, values[i], HASH));
        }
        
        return runtime.newFixnum(h);
    }

    private IRubyObject setByName(String name, IRubyObject value) {
        RubyArray member = (RubyArray) getInternalVariable(classOf(), "__member__");

        assert !member.isNil() : "uninitialized struct";

        modify();

        for (int i = 0,k=member.getLength(); i < k; i++) {
            if (member.eltInternal(i).asJavaString().equals(name)) {
                return values[i] = value;
            }
        }

        throw notStructMemberError(name);
    }

    private IRubyObject getByName(String name) {
        RubyArray member = (RubyArray) getInternalVariable(classOf(), "__member__");

        assert !member.isNil() : "uninitialized struct";

        for (int i = 0,k=member.getLength(); i < k; i++) {
            if (member.eltInternal(i).asJavaString().equals(name)) {
                return values[i];
            }
        }

        throw notStructMemberError(name);
    }

    // Struct methods

    /** Create new Struct class.
     *
     * MRI: rb_struct_s_def / make_struct
     *
     */
    @JRubyMethod(name = "new", required = 1, rest = true, meta = true)
    public static RubyClass newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        String name = null;
        boolean nilName = false;
        Ruby runtime = recv.getRuntime();

        if (args.length > 0) {
            IRubyObject firstArgAsString = args[0].checkStringType();
            if (!firstArgAsString.isNil()) {
                name = ((RubyString)firstArgAsString).getByteList().toString();
            } else if (args[0].isNil()) {
                nilName = true;
            }
        }

        RubyArray member = runtime.newArray();

        for (int i = (name == null && !nilName) ? 0 : 1; i < args.length; i++) {
            member.append(runtime.newSymbol(args[i].asJavaString()));
        }

        RubyClass newStruct;
        RubyClass superClass = (RubyClass)recv;

        if (name == null || nilName) {
            newStruct = RubyClass.newClass(runtime, superClass);
            newStruct.setAllocator(STRUCT_INSTANCE_ALLOCATOR);
            newStruct.makeMetaClass(superClass.getMetaClass());
            newStruct.inherit(superClass);
        } else {
            if (!IdUtil.isConstant(name)) {
                throw runtime.newNameError("identifier " + name + " needs to be constant", name);
            }

            IRubyObject type = superClass.getConstantAt(name);
            if (type != null) {
                ThreadContext context = runtime.getCurrentContext();
                runtime.getWarnings().warn(ID.STRUCT_CONSTANT_REDEFINED, context.getFile(), context.getLine(), "redefining constant Struct::" + name);
                superClass.remove_const(context, runtime.newString(name));
            }
            newStruct = superClass.defineClassUnder(name, superClass, STRUCT_INSTANCE_ALLOCATOR);
        }

        // set reified class to RubyStruct, for Java subclasses to use
        newStruct.setReifiedClass(RubyStruct.class);
        newStruct.index = ClassIndex.STRUCT;
        
        newStruct.setInternalVariable("__size__", member.length());
        newStruct.setInternalVariable("__member__", member);

        newStruct.getSingletonClass().defineAnnotatedMethods(StructMethods.class);

        // define access methods.
        for (int i = (name == null && !nilName) ? 0 : 1; i < args.length; i++) {
            final String memberName = args[i].asJavaString();
            // if we are storing a name as well, index is one too high for values
            final int index = (name == null && !nilName) ? i : i - 1;
            newStruct.addMethod(memberName, new DynamicMethod(newStruct, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    Arity.checkArgumentCount(context.runtime, name, args, 0, 0);
                    return ((RubyStruct)self).get(index);
                }

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                    return ((RubyStruct)self).get(index);
                }

                @Override
                public DynamicMethod dup() {
                    return this;
                }
            });
            newStruct.addMethod(memberName + "=", new DynamicMethod(newStruct, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    Arity.checkArgumentCount(context.runtime, name, args, 1, 1);
                    return ((RubyStruct)self).set(args[0], index);
                }

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
                    return ((RubyStruct)self).set(arg, index);
                }

                @Override
                public DynamicMethod dup() {
                    return this;
                }
            });
        }
        
        if (block.isGiven()) {
            // Struct bodies should be public by default, so set block visibility to public. JRUBY-1185.
            block.getBinding().setVisibility(Visibility.PUBLIC);
            block.yieldNonArray(runtime.getCurrentContext(), null, newStruct, newStruct);
        }

        return newStruct;
    }
    
    // For binding purposes on the newly created struct types
    public static class StructMethods {
        @JRubyMethod(name = {"new", "[]"}, rest = true)
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject[] args, Block block) {
            return RubyStruct.newStruct(recv, args, block);
        }

        @JRubyMethod(name = {"new", "[]"})
        public static IRubyObject newStruct(IRubyObject recv, Block block) {
            return RubyStruct.newStruct(recv, block);
        }

        @JRubyMethod(name = {"new", "[]"})
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, Block block) {
            return RubyStruct.newStruct(recv, arg0, block);
        }

        @JRubyMethod(name = {"new", "[]"})
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
            return RubyStruct.newStruct(recv, arg0, arg1, block);
        }

        @JRubyMethod(name = {"new", "[]"})
        public static IRubyObject newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return RubyStruct.newStruct(recv, arg0, arg1, arg2, block);
        }

        @JRubyMethod(name = "members", compat = CompatVersion.RUBY1_8)
        public static IRubyObject members(IRubyObject recv, Block block) {
            return RubyStruct.members(recv, block);
        }

        @JRubyMethod(name = "members", compat = CompatVersion.RUBY1_9)
        public static IRubyObject members19(IRubyObject recv, Block block) {
            return RubyStruct.members19(recv, block);
        }
    }

    /** Create new Structure.
     *
     * MRI: struct_alloc
     *
     */
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        struct.callInit(args, block);

        return struct;
    }

    public static RubyStruct newStruct(IRubyObject recv, Block block) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        struct.callInit(block);

        return struct;
    }

    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, Block block) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        struct.callInit(arg0, block);

        return struct;
    }

    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        struct.callInit(arg0, arg1, block);

        return struct;
    }

    public static RubyStruct newStruct(IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        struct.callInit(arg0, arg1, arg2, block);

        return struct;
    }

    private void checkSize(int length) {
        if (length > values.length) {
            throw getRuntime().newArgumentError("struct size differs (" + length +" for " + values.length + ")");
        }
    }

    @JRubyMethod(rest = true, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        modify();
        checkSize(args.length);

        System.arraycopy(args, 0, values, 0, args.length);
        RuntimeHelpers.fillNil(values, args.length, values.length, context.runtime);

        return context.nil;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        IRubyObject nil = context.nil;
        return initializeInternal(context, 0, nil, nil, nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        IRubyObject nil = context.nil;
        return initializeInternal(context, 1, arg0, nil, nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return initializeInternal(context, 2, arg0, arg1, context.nil);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return initializeInternal(context, 3, arg0, arg1, arg2);
    }
    
    public IRubyObject initializeInternal(ThreadContext context, int provided, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        modify();
        checkSize(provided);

        switch (provided) {
        case 3:
            values[2] = arg2;
        case 2:
            values[1] = arg1;
        case 1:
            values[0] = arg0;
        }
        if (provided < values.length) {
            RuntimeHelpers.fillNil(values, provided, values.length, context.runtime);
        }

        return getRuntime().getNil();
    }
    
    public static RubyArray members(IRubyObject recv, Block block) {
        RubyArray member = (RubyArray) getInternalVariable((RubyClass) recv, "__member__");

        assert !member.isNil() : "uninitialized struct";

        RubyArray result = recv.getRuntime().newArray(member.getLength());
        for (int i = 0,k=member.getLength(); i < k; i++) {
            // this looks weird, but it's because they're RubySymbol and that's java.lang.String internally
            result.append(recv.getRuntime().newString(member.eltInternal(i).asJavaString()));
        }

        return result;
    }

    public static RubyArray members19(IRubyObject recv, Block block) {
        RubyArray member = (RubyArray) getInternalVariable((RubyClass) recv, "__member__");

        assert !member.isNil() : "uninitialized struct";

        RubyArray result = recv.getRuntime().newArray(member.getLength());
        for (int i = 0,k=member.getLength(); i < k; i++) {
            result.append(member.eltInternal(i));
        }

        return result;
    }

    @JRubyMethod(name = "members", compat = CompatVersion.RUBY1_8)
    public RubyArray members() {
        return members(classOf(), Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "members", compat = CompatVersion.RUBY1_9)
    public RubyArray members19() {
        return members19(classOf(), Block.NULL_BLOCK);
    }
    
    @JRubyMethod
    public RubyArray select(ThreadContext context, Block block) {
        RubyArray array = RubyArray.newArray(context.getRuntime());
        
        for (int i = 0; i < values.length; i++) {
            if (block.yield(context, values[i]).isTrue()) {
                array.append(values[i]);
            }
        }
        
        return array;
    }

    public IRubyObject set(IRubyObject value, int index) {
        modify();

        return values[index] = value;
    }

    private RaiseException notStructMemberError(String name) {
        return getRuntime().newNameError(name + " is not struct member", name);
    }

    public IRubyObject get(int index) {
        return values[index];
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyStruct struct = (RubyStruct)clone;
        struct.values = new IRubyObject[values.length];
        System.arraycopy(values, 0, struct.values, 0, values.length);
    }

    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(final ThreadContext context, IRubyObject other) {
        if (this == other) return getRuntime().getTrue();
        if (!(other instanceof RubyStruct)) return getRuntime().getFalse();
        if (getMetaClass().getRealClass() != other.getMetaClass().getRealClass()) return getRuntime().getFalse();
        
        final Ruby runtime = getRuntime();
        final RubyStruct otherStruct = (RubyStruct)other;

        // identical
        if (other == this) return runtime.getTrue();

        // recursion guard
        return runtime.execRecursiveOuter(new Ruby.RecursiveFunction() {
            public IRubyObject call(IRubyObject obj, boolean recur) {
                if (recur) {
                    return runtime.getTrue();
                }
                for (int i = 0; i < values.length; i++) {
                    if (!equalInternal(context, values[i], otherStruct.values[i])) return runtime.getFalse();
                }
                return runtime.getTrue();
            }
        }, this);
    }
    
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(final ThreadContext context, IRubyObject other) {
        if (this == other) return getRuntime().getTrue();
        if (!(other instanceof RubyStruct)) return getRuntime().getFalse();
        if (getMetaClass() != other.getMetaClass()) return getRuntime().getFalse();
        
        final Ruby runtime = getRuntime();
        final RubyStruct otherStruct = (RubyStruct)other;

        // identical
        if (other == this) return runtime.getTrue();

        // recursion guard
        return runtime.execRecursiveOuter(new Ruby.RecursiveFunction() {
            public IRubyObject call(IRubyObject obj, boolean recur) {
                if (recur) {
                    return runtime.getTrue();
                }
                for (int i = 0; i < values.length; i++) {
                    if (!eqlInternal(context, values[i], otherStruct.values[i])) return runtime.getFalse();
                }
                return runtime.getTrue();
            }
        }, this);
    }

    /** inspect_struct
    *
    */
    private IRubyObject inspectStruct(final ThreadContext context) {    
        RubyArray member = (RubyArray) getInternalVariable(classOf(), "__member__");

        assert !member.isNil() : "uninitialized struct";

        ByteList buffer = new ByteList("#<struct ".getBytes());
        buffer.append(getMetaClass().getRealClass().getRealClass().getName().getBytes());
        buffer.append(' ');

        for (int i = 0,k=member.getLength(); i < k; i++) {
            if (i > 0) buffer.append(',').append(' ');
            // FIXME: MRI has special case for constants here 
            buffer.append(RubyString.objAsString(context, member.eltInternal(i)).getByteList());
            buffer.append('=');
            buffer.append(inspect(context, values[i]).getByteList());
        }

        buffer.append('>');
        return getRuntime().newString(buffer); // OBJ_INFECT        
    }

    @JRubyMethod(name = {"inspect", "to_s"})
    public IRubyObject inspect(ThreadContext context) {
        if (getRuntime().isInspecting(this)) return getRuntime().newString("#<struct " + getMetaClass().getRealClass().getName() + ":...>");

        try {
            getRuntime().registerInspecting(this);
            return inspectStruct(context);
        } finally {
            getRuntime().unregisterInspecting(this);
        }
    }

    @JRubyMethod(name = {"to_a", "values"})
    public RubyArray to_a() {
        return getRuntime().newArray(values);
    }

    @JRubyMethod(name = {"size", "length"} )
    public RubyFixnum size() {
        return getRuntime().newFixnum(values.length);
    }

    public IRubyObject eachInternal(ThreadContext context, Block block) {
        for (int i = 0; i < values.length; i++) {
            block.yield(context, values[i]);
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each(final ThreadContext context, final Block block) {
        return block.isGiven() ? eachInternal(context, block) : enumeratorize(context.getRuntime(), this, "each");
    }

    public IRubyObject each_pairInternal(ThreadContext context, Block block) {
        RubyArray member = (RubyArray) getInternalVariable(classOf(), "__member__");

        assert !member.isNil() : "uninitialized struct";

        for (int i = 0; i < values.length; i++) {
            block.yield(context, getRuntime().newArrayNoCopy(new IRubyObject[]{member.eltInternal(i), values[i]}));
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each_pair(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairInternal(context, block) : enumeratorize(context.getRuntime(), this, "each_pair");
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject aref(IRubyObject key) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return getByName(key.asJavaString());
        }

        int idx = RubyNumeric.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        return values[idx];
    }

    @JRubyMethod(name = "[]=", required = 2)
    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return setByName(key.asJavaString(), value);
        }

        int idx = RubyNumeric.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        modify();
        return values[idx] = value;
    }
    
    // FIXME: This is copied code from RubyArray.  Both RE, Struct, and Array should share one impl
    // This is also hacky since I construct ruby objects to access ruby arrays through aref instead
    // of something lower.
    @JRubyMethod(rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        int olen = values.length;
        RubyArray result = getRuntime().newArray(args.length);

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RubyFixnum) {
                result.append(aref(args[i]));
                continue;
            }

            int beglen[];
            if (!(args[i] instanceof RubyRange)) {
            } else if ((beglen = ((RubyRange) args[i]).begLenInt(olen, 0)) == null) {
                continue;
            } else {
                int beg = beglen[0];
                int len = beglen[1];
                int end = len;
                for (int j = 0; j < end; j++) {
                    result.append(aref(getRuntime().newFixnum(j + beg)));
                }
                continue;
            }
            result.append(aref(getRuntime().newFixnum(RubyNumeric.num2long(args[i]))));
        }

        return result;
    }

    public static void marshalTo(RubyStruct struct, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(struct);
        output.dumpDefaultObjectHeader('S', struct.getMetaClass());

        RubyArray array = (RubyArray)getInternalVariable(struct.classOf(), "__member__");
        output.writeInt(array.size());

        for (int i = 0; i < array.size(); i++) {
            RubySymbol name = (RubySymbol) array.eltInternal(i);
            output.dumpObject(name);
            output.dumpObject(struct.values[i]);
        }
    }

    public static RubyStruct unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        Ruby runtime = input.getRuntime();

        RubySymbol className = (RubySymbol) input.unmarshalObject(false);
        RubyClass rbClass = pathToClass(runtime, className.asJavaString());
        if (rbClass == null) {
            throw runtime.newNameError("uninitialized constant " + className, className.asJavaString());
        }

        RubyArray mem = members(rbClass, Block.NULL_BLOCK);

        int len = input.unmarshalInt();
        IRubyObject[] values;
        if (len == 0) {
            values = IRubyObject.NULL_ARRAY;
        } else {
            values = new IRubyObject[len];
            RuntimeHelpers.fillNil(values, runtime);
        }

        // FIXME: This could all be more efficient, but it's how struct works
        RubyStruct result;
        if (runtime.is1_9()) {
            // 1.9 does not appear to call initialize (JRUBY-5875)
            result = new RubyStruct(runtime, rbClass);
        } else {
            result = newStruct(rbClass, values, Block.NULL_BLOCK);
        }
        input.registerLinkTarget(result);
        
        for(int i = 0; i < len; i++) {
            IRubyObject slot = input.unmarshalObject(false);
            if(!mem.eltInternal(i).toString().equals(slot.toString())) {
                throw runtime.newTypeError("struct " + rbClass.getName() + " not compatible (:" + slot + " for :" + mem.eltInternal(i) + ")");
            }
            result.aset(runtime.newFixnum(i), input.unmarshalObject());
        }
        return result;
    }

    private static RubyClass pathToClass(Ruby runtime, String path) {
        // FIXME: Throw the right ArgumentError's if the class is missing
        // or if it's a module.
        return (RubyClass) runtime.getClassFromPath(path);
    }
    
    private static ObjectAllocator STRUCT_INSTANCE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyStruct instance = new RubyStruct(runtime, klass);
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };
    
    @Override
    @JRubyMethod(required = 1)
    public IRubyObject initialize_copy(IRubyObject arg) {
        if (this == arg) return this;
        RubyStruct original = (RubyStruct) arg;
        
        values = new IRubyObject[original.values.length];
        System.arraycopy(original.values, 0, values, 0, original.values.length);

        return this;
    }
    
}
