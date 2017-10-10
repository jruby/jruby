/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.RubyEnumerator.SizeFn;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Struct")
public class RubyStruct extends RubyObject {
    public static final String NO_MEMBER_IN_STRUCT = "no member '%1$s' in struct";
    public static final String IDENTIFIER_NEEDS_TO_BE_CONSTANT = "identifier %1$s needs to be constant";
    public static final String UNINITIALIZED_CONSTANT = "uninitialized constant %1$s";
    private final IRubyObject[] values;

    /**
     * Constructor for RubyStruct.
     * @param runtime
     * @param rubyClass
     */
    private RubyStruct(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);

        int size = RubyNumeric.fix2int(getInternalVariable(rubyClass, "__size__"));

        values = new IRubyObject[size];

        Helpers.fillNil(values, runtime);
    }

    public static RubyClass createStructClass(Ruby runtime) {
        RubyClass structClass = runtime.defineClass("Struct", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setStructClass(structClass);
        structClass.setClassIndex(ClassIndex.STRUCT);
        structClass.includeModule(runtime.getEnumerable());
        structClass.setReifiedClass(RubyStruct.class);
        structClass.defineAnnotatedMethods(RubyStruct.class);

        return structClass;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
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
        final RubyClass metaClass = getMetaClass();
        return metaClass instanceof MetaClass ? metaClass.getSuperClass() : metaClass;
    }

    private void modify() {
        testFrozen();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        Ruby runtime = context.runtime;

        int h = getType().hashCode();

        IRubyObject[] values = this.values;
        for (int i = 0; i < values.length; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            IRubyObject hash = context.safeRecurse(HashRecursive.INSTANCE, runtime, values[i], "hash", true);
            h ^= RubyNumeric.num2long(hash);
        }

        return runtime.newFixnum(h);
    }

    private IRubyObject setByName(String name, IRubyObject value) {
        final RubyArray member = __member__();

        modify();

        for ( int i = 0; i < member.getLength(); i++ ) {
            if ( member.eltInternal(i).asJavaString().equals(name) ) {
                return values[i] = value;
            }
        }

        return null;
    }

    private IRubyObject getByName(String name) {
        final RubyArray member = __member__();

        for ( int i = 0; i < member.getLength(); i++ ) {
            if ( member.eltInternal(i).asJavaString().equals(name) ) {
                return values[i];
            }
        }

        return null;
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
                throw runtime.newNameError(IDENTIFIER_NEEDS_TO_BE_CONSTANT, recv, name);
            }

            IRubyObject type = superClass.getConstantAt(name);
            if (type != null) {
                ThreadContext context = runtime.getCurrentContext();
                runtime.getWarnings().warn(ID.STRUCT_CONSTANT_REDEFINED, context.getFile(), context.getLine(), "redefining constant " + type);
                superClass.deleteConstant(name);
            }
            newStruct = superClass.defineClassUnder(name, superClass, STRUCT_INSTANCE_ALLOCATOR);
        }

        // set reified class to RubyStruct, for Java subclasses to use
        newStruct.setReifiedClass(RubyStruct.class);
        newStruct.setClassIndex(ClassIndex.STRUCT);

        newStruct.setInternalVariable("__size__", member.length());
        newStruct.setInternalVariable("__member__", member);

        newStruct.getSingletonClass().defineAnnotatedMethods(StructMethods.class);

        // define access methods.
        for (int i = (name == null && !nilName) ? 0 : 1; i < args.length; i++) {
            final String memberName = args[i].asJavaString();
            // if we are storing a name as well, index is one too high for values
            final int index = (name == null && !nilName) ? i : i - 1;
            newStruct.addMethod(memberName, new Accessor(newStruct, index));
            newStruct.addMethod(memberName + '=', new Mutator(newStruct, index));
        }

        if (block.isGiven()) {
            // Since this defines a new class, run the block as a module-eval.
            block.setEvalType(EvalType.MODULE_EVAL);
            // Struct bodies should be public by default, so set block visibility to public. JRUBY-1185.
            block.getBinding().setVisibility(Visibility.PUBLIC);
            block.yieldNonArray(runtime.getCurrentContext(), newStruct, newStruct);
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

        @JRubyMethod
        public static IRubyObject members(IRubyObject recv, Block block) {
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
        Helpers.fillNil(values, args.length, values.length, context.runtime);

        return context.nil;
    }

    @JRubyMethod(visibility = PRIVATE)
    @Override
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
            Helpers.fillNil(values, provided, values.length, context.runtime);
        }

        return context.nil;
    }

    static RubyArray members(RubyClass type) {
        final RubyArray member = __member__(type);
        final int len = member.getLength();
        IRubyObject[] result = new IRubyObject[len];

        for ( int i = 0; i < len; i++ ) {
            result[i] = member.eltInternal(i);
        }

        return RubyArray.newArrayNoCopy(type.getClassRuntime(), result);
    }

    @Deprecated // NOTE: no longer used ... should it get deleted?
    public static RubyArray members(IRubyObject recv, Block block) {
        return members((RubyClass) recv);
    }

    private static RubyArray __member__(RubyClass clazz) {
        RubyArray member = (RubyArray) getInternalVariable(clazz, "__member__");

        assert !member.isNil() : "uninitialized struct";

        return member;
    }

    private RubyArray __member__() {
        return __member__(classOf());
    }

    @JRubyMethod(name = "members")
    public RubyArray members() {
        return members(classOf());
    }

    @Deprecated
    public final RubyArray members19() {
        return members();
    }

    @JRubyMethod
    public IRubyObject select(ThreadContext context, Block block) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, this, "select", enumSizeFn());
        }

        RubyArray array = RubyArray.newArray(context.runtime);

        for (int i = 0; i < values.length; i++) {
            if (block.yield(context, values[i]).isTrue()) {
                array.append(values[i]);
            }
        }

        return array;
    }

    private SizeFn enumSizeFn() {
        final RubyStruct self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.size();
            }
        };
    }

    public IRubyObject set(IRubyObject value, int index) {
        modify();

        return values[index] = value;
    }

    private RaiseException notStructMemberError(IRubyObject name) {
        return getRuntime().newNameError(NO_MEMBER_IN_STRUCT, this, name);
    }

    public final IRubyObject get(int index) {
        return values[index];
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyStruct struct = (RubyStruct) clone;
        System.arraycopy(values, 0, struct.values, 0, values.length);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (!(other instanceof RubyStruct)) return context.fals;
        if (getType() != other.getType()) {
            return context.fals;
        }

        if (other == this) return context.tru;

        // recursion guard
        return context.safeRecurse(EqualRecursive.INSTANCE, other, this, "==", true);
    }

    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (!(other instanceof RubyStruct)) return context.fals;
        if (getMetaClass() != other.getMetaClass()) {
            return context.fals;
        }

        if (other == this) return context.tru;

        // recursion guard
        return context.safeRecurse(EqlRecursive.INSTANCE, other, this, "eql?", true);
    }

    private static final byte[] STRUCT_BEG = { '#','<','s','t','r','u','c','t',' ' };
    private static final byte[] STRUCT_END = { ':','.','.','.','>' };

    /** inspect_struct
    *
    */
    private RubyString inspectStruct(final ThreadContext context, final boolean recur) {
        final Ruby runtime = context.runtime;
        RubyString buffer = RubyString.newString(runtime, new ByteList(32));
        buffer.cat(STRUCT_BEG);

        String cname = getMetaClass().getRealClass().getName();
        final char first = cname.charAt(0);

        if (recur || first != '#') {
            buffer.cat(cname.getBytes());
        }
        if (recur) {
            return buffer.cat(STRUCT_END);
        }

        final RubyArray member = __member__();
        for ( int i = 0; i < member.getLength(); i++ ) {
            if (i > 0) {
                buffer.cat(',').cat(' ');
            }
            else if (first != '#') {
                buffer.cat(' ');
            }
            RubySymbol slot = (RubySymbol) member.eltInternal(i);
            String name = slot.toString();
            if (IdUtil.isLocal(name) || IdUtil.isConstant(name)) {
                buffer.cat19(RubyString.objAsString(context, slot));
            } else {
                buffer.cat19(((RubyString) slot.inspect(context)));
            }
            buffer.cat('=');
            buffer.cat19(inspect(context, values[i]));
        }

        buffer.cat('>');
        return (RubyString) buffer.infectBy(this);
    }

    @JRubyMethod(name = {"inspect", "to_s"})
    public RubyString inspect(final ThreadContext context) {
        // recursion guard
        return (RubyString) context.safeRecurse(InspectRecursive.INSTANCE, this, this, "inspect", false);
    }

    @JRubyMethod(name = {"to_a", "values"})
    @Override
    public RubyArray to_a() {
        return RubyArray.newArrayMayCopy(getRuntime(), values);
    }

    @JRubyMethod
    public RubyHash to_h(ThreadContext context) {
        RubyHash hash = RubyHash.newHash(context.runtime);
        RubyArray members = __member__();

        for (int i = 0; i < values.length; i++) {
            hash.op_aset(context, members.eltOk(i), values[i]);
        }

        return hash;
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
        return block.isGiven() ? eachInternal(context, block) : enumeratorizeWithSize(context, this, "each", enumSizeFn());
    }

    public IRubyObject each_pairInternal(ThreadContext context, Block block) {
        RubyArray member = __member__();

        for (int i = 0; i < values.length; i++) {
            block.yield(context, RubyArray.newArray(context.runtime, member.eltInternal(i), values[i]));
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each_pair(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_pairInternal(context, block) : enumeratorizeWithSize(context, this, "each_pair", enumSizeFn());
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject aref(IRubyObject key) {
        return arefImpl( key, false );
    }

    private IRubyObject arefImpl(IRubyObject key, final boolean nilOnNoMember) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            final String name = key.asJavaString();
            final IRubyObject value = getByName(name);
            if ( value == null ) {
                if ( nilOnNoMember ) return getRuntime().getNil();
                throw notStructMemberError(key);
            }
            return value;
        }
        return aref( RubyNumeric.fix2int(key) );
    }

    final IRubyObject aref(int idx) {
        int newIdx = idx < 0 ? values.length + idx : idx;

        if (newIdx < 0) {
            throw getRuntime().newIndexError("offset " + idx + " too small for struct(size:" + values.length + ")");
        }
        if (newIdx >= values.length) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct(size:" + values.length + ")");
        }

        return values[newIdx];
    }

    @JRubyMethod(name = "[]=", required = 2)
    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            final String name = key.asJavaString();
            final IRubyObject val = setByName(name, value);
            if ( val == null ) throw notStructMemberError(key);
            return value;
        }

        return aset(RubyNumeric.fix2int(key), value);
    }

    private IRubyObject aset(int idx, IRubyObject value) {
        int newIdx = idx < 0 ? values.length + idx : idx;

        if (newIdx < 0) {
            throw getRuntime().newIndexError("offset " + idx + " too small for struct(size:" + values.length + ")");
        } else if (newIdx >= values.length) {
            throw getRuntime().newIndexError("offset " + idx + " too large for struct(size:" + values.length + ")");
        }

        modify();
        return values[newIdx] = value;
    }

    // NOTE: copied from RubyArray, both RE, Struct, and Array should share one impl
    @JRubyMethod(rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        final int olen = values.length;
        RubyArray result = getRuntime().newArray(args.length);

        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if ( arg instanceof RubyFixnum ) {
                result.append( aref(arg) );
                continue;
            }

            final int[] begLen;
            if ( ! ( arg instanceof RubyRange ) ) {
                // do result.append
            }
            else if ( ( begLen = ((RubyRange) args[i]).begLenInt(olen, 0) ) == null ) {
                continue;
            }
            else {
                final int beg = begLen[0];
                final int len = begLen[1];
                for (int j = 0; j < len; j++) {
                    result.append( aref(j + beg) );
                }
                continue;
            }
            result.append( aref(RubyNumeric.num2int(arg)) );
        }

        return result;
    }

    @JRubyMethod(name = "dig", required = 1, rest = true)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        return dig(context, args, 0);
    }

    final IRubyObject dig(ThreadContext context, IRubyObject[] args, int idx) {
        final IRubyObject val = arefImpl( args[idx++], true );
        return idx == args.length ? val : RubyObject.dig(context, val, args, idx);
    }

    public static void marshalTo(RubyStruct struct, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(struct);
        output.dumpDefaultObjectHeader('S', struct.getMetaClass());

        RubyArray member = __member__(struct.classOf());
        output.writeInt(member.size());

        for (int i = 0; i < member.size(); i++) {
            RubySymbol name = (RubySymbol) member.eltInternal(i);
            output.dumpObject(name);
            output.dumpObject(struct.values[i]);
        }
    }

    public static RubyStruct unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        final Ruby runtime = input.getRuntime();

        RubySymbol className = (RubySymbol) input.unmarshalObject(false);
        RubyClass rbClass = pathToClass(runtime, className.asJavaString());
        if (rbClass == null) {
            throw runtime.newNameError(UNINITIALIZED_CONSTANT, runtime.getStructClass(), className);
        }

        final RubyArray member = __member__(rbClass);

        final int len = input.unmarshalInt();

        // FIXME: This could all be more efficient, but it's how struct works
        final RubyStruct result;
        // 1.9 does not appear to call initialize (JRUBY-5875)
        result = new RubyStruct(runtime, rbClass);
        input.registerLinkTarget(result);

        for (int i = 0; i < len; i++) {
            IRubyObject slot = input.unmarshalObject(false);
            final IRubyObject elem = member.eltInternal(i); // RubySymbol
            if ( ! elem.toString().equals( slot.toString() ) ) {
                throw runtime.newTypeError("struct " + rbClass.getName() + " not compatible (:" + slot + " for :" + elem + ")");
            }
            result.aset(i, input.unmarshalObject());
        }
        return result;
    }

    private static RubyClass pathToClass(Ruby runtime, String path) {
        // FIXME: Throw the right ArgumentError's if the class is missing
        // or if it's a module.
        return (RubyClass) runtime.getClassFromPath(path);
    }

    private static final ObjectAllocator STRUCT_INSTANCE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public RubyStruct allocate(Ruby runtime, RubyClass klass) {
            RubyStruct instance = new RubyStruct(runtime, klass);
            instance.setMetaClass(klass);
            return instance;
        }
    };

    @Override
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject arg) {
        if (this == arg) return this;
        RubyStruct original = (RubyStruct) arg;

        checkFrozen();

        System.arraycopy(original.values, 0, values, 0, original.values.length);

        return this;
    }

    private static class Accessor extends DynamicMethod {
        private final int index;

        public Accessor(RubyClass newStruct, int index) {
            super(newStruct, Visibility.PUBLIC);
            this.index = index;
        }

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
            return new Accessor((RubyClass) getImplementationClass(), index);
        }
    }

    private static class Mutator extends DynamicMethod {
        private final int index;

        public Mutator(RubyClass newStruct, int index) {
            super(newStruct, Visibility.PUBLIC);
            this.index = index;
        }

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
            return new Accessor((RubyClass) getImplementationClass(), index);
        }
    }

    @Deprecated
    public static RubyArray members19(IRubyObject recv, Block block) {
        return members(recv, block);
    }

    private static class EqlRecursive implements ThreadContext.RecursiveFunctionEx<IRubyObject> {

        static final EqlRecursive INSTANCE = new EqlRecursive();

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject other, IRubyObject self, boolean recur) {
            if (recur) return context.tru;

            IRubyObject[] values = ((RubyStruct) self).values;
            IRubyObject[] otherValues = ((RubyStruct) other).values;
            for (int i = 0; i < values.length; i++) {
                if (!eqlInternal(context, values[i], otherValues[i])) return context.fals;
            }
            return context.tru;
        }
    }

    private static class HashRecursive implements ThreadContext.RecursiveFunctionEx<Ruby> {

        static final HashRecursive INSTANCE = new HashRecursive();

        @Override
        public IRubyObject call(ThreadContext context, Ruby runtime, IRubyObject obj, boolean recur) {
            if (recur) return RubyFixnum.zero(runtime);
            return invokedynamic(context, obj, HASH);
        }
    }

    private static class EqualRecursive implements ThreadContext.RecursiveFunctionEx<IRubyObject> {

        private static final EqualRecursive INSTANCE = new EqualRecursive();

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject other, IRubyObject self, boolean recur) {
            if (recur) return context.tru;

            IRubyObject[] values = ((RubyStruct) self).values;
            IRubyObject[] otherValues = ((RubyStruct) other).values;
            for (int i = 0; i < values.length; i++) {
                if (!equalInternal(context, values[i], otherValues[i])) return context.fals;
            }
            return context.tru;
        }
    }

    private static class InspectRecursive implements ThreadContext.RecursiveFunctionEx<RubyStruct> {

        private static final ThreadContext.RecursiveFunctionEx INSTANCE = new InspectRecursive();

        public IRubyObject call(ThreadContext context, RubyStruct self, IRubyObject obj, boolean recur) {
            return self.inspectStruct(context, recur);
        }
    }

}
