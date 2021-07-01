/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.ByteList;
import static org.jruby.runtime.Visibility.*;

/**
 * Defines the memory layout for a native structure.
 */
@JRubyClass(name=StructLayout.CLASS_NAME, parent="Object")
public final class StructLayout extends Type {
    static final Storage nullStorage = new NullStorage();
    
    /** The name to use to register this class in the JRuby runtime */
    static final String CLASS_NAME = "StructLayout";

    private final Member[] identityLookupTable;

    /** The name:offset map for this struct */
    private final Map<IRubyObject, Member> memberMap;
    
    /** The ordered list of field names (as symbols) */
    private final List<IRubyObject> fieldNames;

    /** The ordered list of fields */
    private final List<Field> fields;

    /** The ordered list of fields */
    private final Collection<Member> members;

    /** The number of cacheable fields in this struct */
    private final int cacheableFieldCount;

    /** The number of reference fields in this struct */
    private final int referenceFieldCount;

    private final boolean isUnion;

    /**
     * Registers the StructLayout class in the JRuby runtime.
     * @param runtime The JRuby runtime to register the new class in.
     * @return The new class
     */
    public static RubyClass createStructLayoutClass(Ruby runtime, RubyModule module) {
        RubyClass layoutClass = runtime.defineClassUnder(CLASS_NAME, module.getClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, module);
        layoutClass.defineAnnotatedMethods(StructLayout.class);
        layoutClass.defineAnnotatedConstants(StructLayout.class);
        layoutClass.setReifiedClass(StructLayout.class);

        RubyClass inlineArrayClass = module.getClass("Struct").defineClassUnder("InlineArray", 
                runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        RubyClass arrayClass = runtime.defineClassUnder("ArrayProxy", inlineArrayClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        arrayClass.includeModule(runtime.getEnumerable());
        arrayClass.defineAnnotatedMethods(ArrayProxy.class);

        RubyClass charArrayClass = runtime.defineClassUnder("CharArrayProxy", arrayClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        charArrayClass.defineAnnotatedMethods(CharArrayProxy.class);

        RubyClass fieldClass = runtime.defineClassUnder("Field", runtime.getObject(),
                Field::new, layoutClass);
        fieldClass.defineAnnotatedMethods(Field.class);

        RubyClass numberFieldClass = runtime.defineClassUnder("Number", fieldClass,
                NumberField::new, layoutClass);

        RubyClass enumFieldClass = runtime.defineClassUnder("Enum", fieldClass,
                EnumField::new, layoutClass);

        RubyClass stringFieldClass = runtime.defineClassUnder("String", fieldClass,
                StringField::new, layoutClass);

        RubyClass pointerFieldClass = runtime.defineClassUnder("Pointer", fieldClass,
                PointerField::new, layoutClass);

        RubyClass functionFieldClass = runtime.defineClassUnder("Function", fieldClass,
                FunctionField::new, layoutClass);
        functionFieldClass.defineAnnotatedMethods(FunctionField.class);

        RubyClass innerStructFieldClass = runtime.defineClassUnder("InnerStruct", fieldClass,
                InnerStructField::new, layoutClass);
        innerStructFieldClass.defineAnnotatedMethods(InnerStructField.class);

        RubyClass arrayFieldClass = runtime.defineClassUnder("Array", fieldClass,
                ArrayField::new, layoutClass);
        arrayFieldClass.defineAnnotatedMethods(ArrayField.class);

        return layoutClass;
    }
    
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>.
     * @param fields The fields map for this struct.
     * @param size the total size of the struct.
     * @param alignment The minimum alignment required when allocating memory.
     */
    private StructLayout(Ruby runtime, RubyClass klass, Collection<IRubyObject> fields, int size, int alignment) {
        super(runtime, klass, NativeType.STRUCT, size, alignment);

        int cfCount = 0, refCount = 0;
        List<Field> fieldList = new ArrayList<Field>(fields.size());
        List<IRubyObject> names = new ArrayList<IRubyObject>(fields.size());
        List<Member> memberList = new ArrayList<Member>(fields.size());
        Map<IRubyObject, Member> memberStringMap = new HashMap<IRubyObject, Member>(fields.size());
        Member[] memberSymbolLookupTable = new Member[Util.roundUpToPowerOfTwo(fields.size() * 8)];
        int offset = 0;
        
        int index = 0;
        for (IRubyObject obj : fields) {
            
            if (!(obj instanceof Field)) {
                throw runtime.newTypeError(obj, runtime.getModule("FFI").getClass("StructLayout").getClass("Field"));
            }

            Field f = (Field) obj;
            if (!(f.name instanceof RubySymbol)) {
                throw runtime.newTypeError("fields list contains field with invalid name");
            }
            if (f.type.getNativeSize() < 1 && index < (fields.size() - 1)) {
                throw runtime.newTypeError("sizeof field == 0");
            }

            names.add(f.name);
            fieldList.add(f);

            Member m = new Member(f, index, f.isCacheable() ? cfCount++ : -1, f.isValueReferenceNeeded() ? refCount++ : -1);
            for (int idx = symbolIndex(f.name, memberSymbolLookupTable.length); ; idx = nextIndex(idx, memberSymbolLookupTable.length)) {
                if (memberSymbolLookupTable[idx] == null) {
                    memberSymbolLookupTable[idx] = m;
                    break;
                }
            }

            // Allow fields to be accessed as ['name'] as well as [:name] for legacy code
            memberStringMap.put(f.name, m);
            memberStringMap.put(f.name.asString(), m);
            memberList.add(m);
            offset = Math.max(offset, f.offset);
            index++;
        }


        this.cacheableFieldCount = cfCount;
        this.referenceFieldCount = refCount;

        // Create the ordered list of field names from the map
        this.fieldNames = Collections.unmodifiableList(new ArrayList<IRubyObject>(names));
        this.fields = Collections.unmodifiableList(fieldList);
        this.memberMap = Collections.unmodifiableMap(memberStringMap);
        this.identityLookupTable = memberSymbolLookupTable;
        this.members = Collections.unmodifiableList(memberList);
        this.isUnion = offset == 0 && memberList.size() > 1;
    }
    
    @JRubyMethod(name = "new", meta = true, required = 3, optional = 1)
    public static final IRubyObject newStructLayout(ThreadContext context, IRubyObject klass, 
            IRubyObject[] args) {

        IRubyObject rbFields = args[0], size = args[1], alignment = args[2];

        if (!(rbFields instanceof RubyArray)) {
            throw context.runtime.newTypeError(rbFields, context.runtime.getArray());
        }

        List<IRubyObject> fields = Arrays.asList(((RubyArray) rbFields).toJavaArrayMaybeUnsafe());

        return new StructLayout(context.runtime, (RubyClass) klass, fields,
                RubyNumeric.num2int(size), RubyNumeric.num2int(alignment));
    }

    /**
     * Gets the value of the struct member corresponding to <tt>name</tt>.
     * 
     * @param ptr The address of the structure in memory.
     * @param name The name of the member.
     * @return A ruby value for the native value of the struct member.
     */
    @JRubyMethod(name = "get", required = 2)
    public IRubyObject get(ThreadContext context, IRubyObject ptr, IRubyObject name) {
        return getValue(context, name, nullStorage, ptr);
    }
    
    /**
     * Sets the native value of the struct member corresponding to <tt>name</tt>.
     * 
     * @param ptr The address of the structure in memory.
     * @param name The name of the member.
     * @return A ruby value for the native value of the struct member.
     */
    @JRubyMethod(name = "put", required = 3)
    public IRubyObject put(ThreadContext context, IRubyObject ptr, IRubyObject name, IRubyObject value) {
        putValue(context, name, nullStorage, ptr, value);

        return value;
    }
    
    /**
     * Gets a ruby array of the names of all members of this struct.
     * 
     * @return a <tt>RubyArray</tt> containing the names of all members.
     */
    @JRubyMethod(name = "members")
    public IRubyObject members(ThreadContext context) {
        RubyArray mbrs = RubyArray.newArray(context.runtime, fieldNames.size());
        for (IRubyObject name : fieldNames) {
            mbrs.append(name);
        }
        return mbrs;
    }

    /**
     * Gets a ruby array of the offsets of all members of this struct.
     *
     * @return a <tt>RubyArray</tt> containing the offsets of all members.
     */
    @JRubyMethod(name = "offsets")
    public IRubyObject offsets(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyArray offsets = RubyArray.newArray(runtime);

        for (IRubyObject name : fieldNames) {
            RubyArray offset = RubyArray.newArray(runtime);
            // Assemble a [ :name, offset ] array
            offset.append(name);
            offset.append(runtime.newFixnum(getMember(runtime, name).offset));
            offsets.append(offset);
        }

        return offsets;
    }

    @JRubyMethod(name = "offset_of")
    public IRubyObject offset_of(ThreadContext context, IRubyObject fieldName) {
        return getField(context.runtime, fieldName).offset(context);
    }
    
    @JRubyMethod(name = "[]")
    public IRubyObject aref(ThreadContext context, IRubyObject fieldName) {
        return getField(context.runtime, fieldName);
    }

    @JRubyMethod
    public IRubyObject fields(ThreadContext context) {
        return RubyArray.newArray(context.runtime, fields);
    }

    @JRubyMethod(name = "__union!")
    public IRubyObject union_bang(ThreadContext context) {
        Ruby runtime = context.runtime;

        NativeType[] alignmentTypes = {
                NativeType.CHAR, NativeType.SHORT, NativeType.INT, NativeType.LONG,
                NativeType.FLOAT, NativeType.DOUBLE, NativeType.LONGDOUBLE
        };

        NativeType t = null;
        int count, i;

        for (NativeType alignmentType : alignmentTypes) {
            if (Type.getNativeAlignment(alignmentType) == alignment) {
                t = alignmentType;
                break;
            }
        }

        if (t == null) {
            throw runtime.newRuntimeError("cannot create libffi union representation for alignment " + alignment);
        }

        // FIXME: wot
//        count = layout.size / Type.getNativeSize(t);
//        layout.ffiTypes = xcalloc(count + 1, sizeof(ffi_type *));
//        layout.base.ffiType->elements = layout->ffiTypes;
//
//        for (i = 0; i < count; ++i) {
//            layout->ffiTypes[i] = t;
//        }

        return this;
    }

    final IRubyObject getValue(ThreadContext context, IRubyObject name, Storage cache, IRubyObject ptr) {
        return getMember(context.runtime, name).get(context, cache, AbstractMemory.cast(context, ptr));
    }

    final void putValue(ThreadContext context, IRubyObject name, Storage cache, IRubyObject ptr, IRubyObject value) {
        getMember(context.runtime, name).put(context, cache, AbstractMemory.cast(context, ptr), value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StructLayout that = (StructLayout) o;

        if (fields != null ? !fields.equals(that.fields) : that.fields != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }

    private static int symbolIndex(IRubyObject name, int length) {
        return System.identityHashCode(name) & (length - 1);
    }

    private static int nextIndex(int idx, int length) {
        return (idx + 1) & (length - 1);
    }

    /**
     * Returns a {@link Member} descriptor for a struct field.
     * 
     * @param name The name of the struct field.
     * @return A <tt>Member</tt> descriptor.
     */
    final Member getMember(Ruby runtime, IRubyObject name) {
        Member m;
        int idx = symbolIndex(name, identityLookupTable.length);
        while ((m = identityLookupTable[idx]) != null) {
            if (m.name == name) {
                return m;
            }
            idx = nextIndex(idx, identityLookupTable.length);
        }

        Member f = memberMap.get(name);
        if (f != null) {
            return f;
        }

        throw runtime.newArgumentError("Unknown field: " + name);
    }

    /**
     * Returns a {@link Field} descriptor for a struct field.
     *
     * @param name The name of the struct field.
     * @return A <tt>Member</tt> descriptor.
     */
    final Field getField(Ruby runtime, IRubyObject name) {
        return getMember(runtime, name).field;
    }

    public final int getSize() {
        return getNativeSize();
    }

    final int getReferenceFieldCount() {
        return referenceFieldCount;
    }

    final int getReferenceFieldIndex(Member member) {
        return member.referenceIndex;
    }

    final int getCacheableFieldCount() {
        return cacheableFieldCount;
    }

    final int getCacheableFieldIndex(Member member) {
        return member.cacheIndex;
    }

    public final int getFieldCount() {
        return fields.size();
    }

    public final java.util.Collection<Field> getFields() {
        return fields;
    }

    public final java.util.Collection<Member> getMembers() {
        return members;
    }

    public final boolean isUnion() {
        return isUnion;
    }

    /**
     * A struct member.  This defines the offset within a chunk of memory to use
     * when reading/writing the member, as well as how to convert between the 
     * native representation of the member and the JRuby representation.
     */
    public static final class Member {
        final FieldIO io;

        final Field field;

        /** The {@link Type} of this member. */
        final Type type;

        /** The offset within the memory area of this member */
        final int offset;

        /** The index of this member within the struct field cache */
        final int cacheIndex;

        /** The index of this member within the struct field reference array*/
        final int referenceIndex;

        /** The index of this member within the struct */
        final int index;

        final IRubyObject name;

        /** Initializes a new Member instance */
        protected Member(Field f, int index, int cacheIndex, int referenceIndex) {
            this.field = f;
            this.io = f.io;
            this.type = f.type;
            this.offset = f.offset;
            this.index = index;
            this.cacheIndex = cacheIndex;
            this.referenceIndex = referenceIndex;
            this.name = f.name;
        }

        final long getOffset(IRubyObject ptr) {
            return offset;
        }

        final int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Member && ((Member) obj).offset == offset && type.equals(((Member) obj).type);
        }

        @Override
        public int hashCode() {
            return 53 * 5 + this.offset + 37 * type.hashCode();
        }
        
        /**
         * Writes a ruby value to the native struct member as the appropriate native value.
         *
         * @param context the current context
         * @param cache The value cache
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public final void put(ThreadContext context, Storage cache, AbstractMemory ptr, IRubyObject value) {
            io.put(context, cache, this, ptr, value);
        }

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param ptr The struct memory area.
         * @return A ruby object equivalent to the native member value.
         */
        public final IRubyObject get(ThreadContext context, Storage cache, AbstractMemory ptr) {
            return io.get(context, cache, this, ptr);
        }

        public final int offset() {
            return offset;
        }

        public final Type type() {
            return type;
        }
    }

    interface FieldIO {
        /**
         * Writes a ruby value to the native struct member as the appropriate native value.
         *
         * @param context the current context
         * @param cache The value cache
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public abstract void put(ThreadContext context, Storage cache, Member m, AbstractMemory ptr, IRubyObject value);

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param ptr The struct memory area.
         * @return A ruby object equivalent to the native member value.
         */
        public abstract IRubyObject get(ThreadContext context, Storage cache, Member m, AbstractMemory ptr);

        /**
         * Gets the cacheable status of this Struct member
         *
         * @return <tt>true</tt> if this member type is cacheable
         */
        public abstract boolean isCacheable();

        /**
         * Checks if a reference to the ruby object assigned to this field needs to be stored
         *
         * @return <tt>true</tt> if this member type requires the ruby value to be stored.
         */
        public abstract boolean isValueReferenceNeeded();
    }

    static final class DefaultFieldIO implements FieldIO {
        public static final FieldIO INSTANCE = new DefaultFieldIO();
        private final CachingCallSite getCallSite = new FunctionalCachingCallSite("get");
        private final CachingCallSite putCallSite = new FunctionalCachingCallSite("put");

        public IRubyObject get(ThreadContext context, Storage cache, Member m, AbstractMemory ptr) {
            return getCallSite.call(context, m.field, m.field, ptr);
        }

        public void put(ThreadContext context, Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            putCallSite.call(context, m.field, m.field, ptr, value);
        }

        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }

    @JRubyClass(name="FFI::StructLayout::Field", parent="Object")
    public static class Field extends RubyObject {

        /** The basic ops to read/write this field */
        private FieldIO io;

        /** The name of this field */
        private IRubyObject name;

        /** The {@link Type} of this field. */
        private Type type;

        /** The offset within the memory area of this member */
        private int offset;

        /** The memory operation for this field type */
        private MemoryOp memoryOp;


        Field(Ruby runtime, RubyClass klass) {
            this(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        Field(Ruby runtime, RubyClass klass, FieldIO io) {
            this(runtime, klass, (Type) runtime.getModule("FFI").getClass("Type").getConstant("VOID"),
                    -1, io);
            
        }
        
        Field(Ruby runtime, RubyClass klass, Type type, int offset, FieldIO io) {
            super(runtime, klass);
            this.name = runtime.getNil();
            this.type = type;
            this.offset = offset;
            this.io = io;
            this.memoryOp = MemoryOp.getMemoryOp(type);
        }

        void init(IRubyObject name, IRubyObject type, IRubyObject offset) {
            this.name = name;
            Type realType = checkType(type);
            this.type = realType;
            this.offset = RubyNumeric.num2int(offset);
            this.memoryOp = MemoryOp.getMemoryOp(realType);
        }

        void init(IRubyObject name, IRubyObject type, IRubyObject offset, FieldIO io) {
            init(name, type, offset);
            this.io = io;
        }

        void init(IRubyObject[] args, FieldIO io) {
            init(args[0], args[2], args[1], io);
        }

        @JRubyMethod(name="initialize", visibility = PRIVATE, required = 3, optional = 1)
        public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
            
            init(args[0], args[2], args[1]);

            return this;
        }

        final Type checkType(IRubyObject type) {
            if (!(type instanceof Type)) {
                throw getRuntime().newTypeError(type, getRuntime().getModule("FFI").getClass("Type"));
            }
            return (Type) type;
        }

        public final int offset() {
            return this.offset;
        }

        public final Type ffiType() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Field && ((Field) obj).offset == offset && ((Field) obj).type.equals(type);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + offset;
            return result;
        }

        /**
         * Gets the cacheable status of this Struct member
         *
         * @return <tt>true</tt> if this member type is cacheable
         */
        public final boolean isCacheable() {
            return io.isCacheable();
        }

        /**
         * Checks if a reference to the ruby object assigned to this field needs to be stored
         *
         * @return <tt>true</tt> if this member type requires the ruby value to be stored.
         */
        public final boolean isValueReferenceNeeded() {
            return io.isValueReferenceNeeded();
        }

        final FieldIO getFieldIO() {
            return io;
        }

        static ByteOrder getByteOrderOption(ThreadContext context, IRubyObject[] args) {

            ByteOrder order = ByteOrder.nativeOrder();

            if (args.length > 3 && args[3] instanceof RubyHash) {
                RubyHash options = (RubyHash) args[3];
                IRubyObject byte_order = options.fastARef(RubySymbol.newSymbol(context.runtime, "byte_order"));
                if (byte_order instanceof RubySymbol || byte_order instanceof RubyString) {
                    String orderName = byte_order.asJavaString();
                    if ("network".equals(orderName) || "big".equals(orderName)) {
                        order = ByteOrder.BIG_ENDIAN;
                    
                    } else if ("little".equals(orderName)) {
                        order = ByteOrder.LITTLE_ENDIAN;
                    }
                }
            }
            
            return order;
        }

        @JRubyMethod
        public final IRubyObject size(ThreadContext context) {
            return context.runtime.newFixnum(type.getNativeSize());
        }

        @JRubyMethod
        public final IRubyObject alignment(ThreadContext context) {
            return context.runtime.newFixnum(type.getNativeAlignment());
        }

        @JRubyMethod
        public final IRubyObject offset(ThreadContext context) {
            return context.runtime.newFixnum(offset);
        }

        @JRubyMethod(name = { "type", "ffi_type" })
        public final IRubyObject type(ThreadContext context) {
            return type;
        }

        @JRubyMethod
        public final IRubyObject name(ThreadContext context) {
            return name;
        }

        @JRubyMethod
        public final IRubyObject get(ThreadContext context, IRubyObject pointer) {
            MemoryOp memoryOp = this.memoryOp;
            if (memoryOp == null) {
                throw context.runtime.newArgumentError("get not supported for " + type.nativeType.name());
            }

            return memoryOp.get(context, AbstractMemory.cast(context, pointer), offset);
        }

        @JRubyMethod
        public final IRubyObject put(ThreadContext context, IRubyObject pointer, IRubyObject value) {
            MemoryOp memoryOp = this.memoryOp;

            if (memoryOp == null) {
                throw context.runtime.newArgumentError("put not supported for " + type.nativeType.name());
            }

            memoryOp.put(context, AbstractMemory.cast(context, pointer), offset, value);

            return this;
        }
    }

    @JRubyClass(name="FFI::StructLayout::Number", parent="FFI::StructLayout::Field")
    public static final class NumberField extends Field {

        public NumberField(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
        }

        @Override
        public final IRubyObject initialize(ThreadContext context, IRubyObject[] args) {

            init(args, new NumberFieldIO(checkType(args[2]), getByteOrderOption(context, args)));

            return this;
        }
    }

    @JRubyClass(name="FFI::StructLayout::Enum", parent="FFI::StructLayout::Field")
    public static final class EnumField extends Field {
        public EnumField(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
        }

        @Override
        public final IRubyObject initialize(ThreadContext context, IRubyObject[] args) {

            init(args, new EnumFieldIO(getByteOrderOption(context, args)));

            return this;
        }
    }

    @JRubyClass(name="FFI::StructLayout::String", parent="FFI::StructLayout::Field")
    static final class StringField extends Field {
        public StringField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, StringFieldIO.INSTANCE);
        }
    }

    @JRubyClass(name="FFI::StructLayout::Pointer", parent="FFI::StructLayout::Field")
    public static final class PointerField extends Field {
        public PointerField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, PointerFieldIO.INSTANCE);
        }
    }

    @JRubyClass(name="FFI::StructLayout::Function", parent="FFI::StructLayout::Field")
    public static final class FunctionField extends Field {

        public FunctionField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, FunctionFieldIO.INSTANCE);
        }

        @Override
        @JRubyMethod(name="initialize", visibility = PRIVATE, required = 3, optional = 1)
        public final IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
            
            IRubyObject type = args[2];

            if (!(type instanceof CallbackInfo)) {
                throw context.runtime.newTypeError(type, context.runtime.getModule("FFI").getClass("Type").getClass("Function"));
            }
            init(args, FunctionFieldIO.INSTANCE);

            return this;
        }
    }

    @JRubyClass(name="FFI::StructLayout::InnerStruct", parent="FFI::StructLayout::Field")
    public static final class InnerStructField extends Field {

        public InnerStructField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, DefaultFieldIO.INSTANCE);
        }
        
        @Override
        @JRubyMethod(name="initialize", visibility = PRIVATE, required = 3, optional = 1)
        public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {

            IRubyObject type = args[2];

            if (!(type instanceof StructByValue)) {
                throw context.runtime.newTypeError(type,
                        context.runtime.getModule("FFI").getClass("Type").getClass("Struct"));
            }
            init(args, new InnerStructFieldIO((StructByValue) type));

            return this;
        }
    }

    @JRubyClass(name="FFI::StructLayout::Array", parent="FFI::StructLayout::Field")
    public static final class ArrayField extends Field {

        public ArrayField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        @Override
        @JRubyMethod(name="initialize", visibility = PRIVATE, required = 3, optional = 1)
        public final IRubyObject initialize(ThreadContext context, IRubyObject[] args) {

            IRubyObject type = args[2];
            if (!(type instanceof Type.Array)) {
                throw context.runtime.newTypeError(type,
                        context.runtime.getModule("FFI").getClass("Type").getClass("Array"));
            }
            init(args, new ArrayFieldIO((Type.Array) type));

            return this;
        }
    }

    public static interface Storage {
        IRubyObject getCachedValue(Member member);
        void putCachedValue(Member member, IRubyObject value);
        void putReference(Member member, Object value);
    }

    static class NullStorage implements Storage {
        public IRubyObject getCachedValue(Member member) { return null; }
        public void putCachedValue(Member member, IRubyObject value) { }
        public void putReference(Member member, Object value) { }
    }
    
    @JRubyClass(name="FFI::StructLayout::ArrayProxy", parent="Object")
    public static class ArrayProxy extends RubyObject {
        protected final AbstractMemory ptr;
        final MemoryOp aio;
        protected final Type.Array arrayType;
        private final boolean cacheable;
        private IRubyObject[] valueCache;

        ArrayProxy(Ruby runtime, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            this(runtime, runtime.getModule("FFI").getClass(CLASS_NAME).getClass("ArrayProxy"),
                    ptr, offset, type, aio);
        }

        ArrayProxy(Ruby runtime, RubyClass klass, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            super(runtime, klass);
            this.ptr = type.length() > 0 
                    ? ((AbstractMemory) ptr).slice(runtime, offset, type.getNativeSize())
                    : ((AbstractMemory) ptr).slice(runtime, offset);
            this.arrayType = type;
            this.aio = aio;
            this.cacheable = type.length() > 0 && (type.getComponentType() instanceof Type.Array 
                    || type.getComponentType() instanceof StructByValue);
        }

        private long getOffset(int index) {
            if (index < 0 || (index >= arrayType.length() && arrayType.length() > 0)) {
                throw getRuntime().newIndexError("index " + index + " out of bounds");
            }

            return index * (long) arrayType.getComponentType().getNativeSize();
        }

        private IRubyObject get(ThreadContext context, int index) {
            IRubyObject obj;
            
            if (valueCache != null && (obj = valueCache[index]) != null) {
                return obj;
            }
            putCachedValue(index, obj = aio.get(context, ptr, getOffset(index)));
            
            return obj;
        }
        
        public final void putCachedValue(int idx, IRubyObject value) {
            if (cacheable) {
                if (valueCache == null) {
                    valueCache = new IRubyObject[arrayType.length()];
                }
                valueCache[idx] = value;
            }
        }

        @JRubyMethod(name = "[]")
        public IRubyObject get(ThreadContext context, IRubyObject index) {
            return get(context, Util.int32Value(index));
        }

        @JRubyMethod(name = "[]=")
        public IRubyObject put(ThreadContext context, IRubyObject index, IRubyObject value) {
            int idx = Util.int32Value(index);
            
            putCachedValue(idx, value);
            
            aio.put(context, ptr, getOffset(idx), value);
            
            return value;
        }

        @JRubyMethod(name = { "to_a", "to_ary" })
        public IRubyObject get(ThreadContext context) {
            
            IRubyObject[] elems = new IRubyObject[arrayType.length()];
            for (int i = 0; i < elems.length; ++i) {
                elems[i] = get(context, i);
            }

            return RubyArray.newArrayMayCopy(context.runtime, elems);
        }

        @JRubyMethod(name = { "to_ptr" })
        public IRubyObject to_ptr(ThreadContext context) {
            return ptr;
        }

        @JRubyMethod(name = { "size" })
        public IRubyObject size(ThreadContext context) {
            return arrayType.length(context);
        }

        /**
         * Needed for Enumerable implementation
         */
        @JRubyMethod(name = "each")
        public IRubyObject each(ThreadContext context, Block block) {
            if (!block.isGiven()) {
                throw context.runtime.newLocalJumpErrorNoBlock();
            }
            for (int i = 0; i < arrayType.length(); ++i) {
                block.yield(context, get(context, i));
            }
            return this;
        }

        
    }

    @JRubyClass(name="FFI::StructLayout::CharArrayProxy", parent="FFI::StructLayout::ArrayProxy")
    public static final class CharArrayProxy extends ArrayProxy {
        CharArrayProxy(Ruby runtime, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            super(runtime, runtime.getModule("FFI").getClass("StructLayout").getClass("CharArrayProxy"),
                    ptr, offset, type, aio);
        }

        @JRubyMethod(name = { "to_s" })
        public IRubyObject to_s(ThreadContext context) {
            return MemoryUtil.getTaintedString(context.runtime, ptr.getMemoryIO(), 0, arrayType.length());
        }
    }

    /**
     * Primitive (byte, short, int, long, float, double) types are all handled by
     * a PrimitiveMember type.
     */
    static final class NumberFieldIO implements FieldIO {
        private final MemoryOp op;

        NumberFieldIO(Type type, ByteOrder order) {
            this.op = MemoryOp.getMemoryOp(type, order);
        }
        
        NumberFieldIO(MemoryOp op) {
            this.op = op;
        }

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            op.put(context, ptr, m.offset, value);
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            return op.get(context, ptr, m.offset);
        }

        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }

    /**
     * Enum (maps :foo => 1, :bar => 2, etc)
     */
    static final class EnumFieldIO implements FieldIO {
        private final MemoryOp op;

        public EnumFieldIO(ByteOrder order) {
            this.op = MemoryOp.getMemoryOp(NativeType.INT, order);
        }


        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            // Upcall to ruby to convert :foo to an int, then write it out
            op.put(context, ptr, m.offset, m.type.callMethod(context, "find", value));
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            // Read an int from the native memory, then upcall to the ruby value
            // lookup code to convert it to the appropriate symbol
            return m.type.callMethod(context, "find", op.get(context, ptr, m.offset));
        }

        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }

    

    static final class PointerFieldIO implements FieldIO {
        public static final FieldIO INSTANCE = new PointerFieldIO();
        
        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            DynamicMethod conversionMethod;
            if (value instanceof Pointer) {
                ptr.getMemoryIO().putMemoryIO(m.offset, ((Pointer) value).getMemoryIO());
            } else if (value instanceof Struct) {
                MemoryIO mem = ((Struct) value).getMemoryIO();

                if (!mem.isDirect()) {
                    throw context.runtime.newArgumentError("Struct memory not backed by a native pointer");
                }
                ptr.getMemoryIO().putMemoryIO(m.offset, mem);

            } else if (value instanceof RubyInteger) {
                ptr.getMemoryIO().putAddress(m.offset, Util.int64Value(value));

            } else if (value.isNil()) {
                ptr.getMemoryIO().putAddress(m.offset, 0L);

            } else if (!(conversionMethod = value.getMetaClass().searchMethod("to_ptr")).isUndefined()) {
                IRubyObject addr = conversionMethod.call(context, value, value.getMetaClass(), "to_ptr");
                if (addr instanceof Pointer) {
                    ptr.getMemoryIO().putMemoryIO(m.offset, ((Pointer) addr).getMemoryIO());
                } else {
                    throw context.runtime.newArgumentError("Invalid pointer value");
                }
            } else {
                throw context.runtime.newArgumentError("Invalid pointer value");
            }
            cache.putReference(m, value);
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            MemoryIO memory = ((AbstractMemory) ptr).getMemoryIO().getMemoryIO(m.getOffset(ptr));
            IRubyObject old = cache.getCachedValue(m);
            if (old instanceof Pointer) {
                MemoryIO oldMemory = ((Pointer) old).getMemoryIO();
                if (memory.equals(oldMemory)) {
                    return old;
                }
            }
            Pointer retval = new Pointer(context.runtime, memory);
            cache.putCachedValue(m, retval);

            return retval;
        }

        public final boolean isCacheable() {
            return true;
        }

        public final boolean isValueReferenceNeeded() {
            return true;
        }
    }

    static final class StringFieldIO implements FieldIO {
        public static final FieldIO INSTANCE = new StringFieldIO();
        
        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            MemoryIO io = ptr.getMemoryIO().getMemoryIO(m.getOffset(ptr));
            if (io == null || io.isNull()) {
                return context.nil;
            }

            return RubyString.newStringNoCopy(context.runtime, io.getZeroTerminatedByteArray(0));
        }

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            ByteList bl = value.convertToString().getByteList();

            MemoryPointer mem = MemoryPointer.allocate(context.runtime, 1, bl.length() + 1, false);
            //
            // Keep a reference to the temporary memory in the cache so it does
            // not get freed by the GC until the struct is freed
            //
            cache.putReference(m, mem);

            MemoryIO io = mem.getMemoryIO();
            io.put(0, bl.getUnsafeBytes(), bl.begin(), bl.length());
            io.putByte(bl.length(), (byte) 0);

            ptr.getMemoryIO().putMemoryIO(m.getOffset(ptr), io);
        }

        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return true;
        }
    }


    static final class FunctionFieldIO implements FieldIO {
        public static final FieldIO INSTANCE = new FunctionFieldIO();

        public void put(ThreadContext context, Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            if (value.isNil()) {
                ptr.getMemoryIO().putAddress(m.getOffset(ptr), 0L);
                cache.putReference(m, value);
            } else {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(context.runtime, (CallbackInfo) m.type, value);
                ptr.getMemoryIO().putMemoryIO(m.getOffset(ptr), cb.getMemoryIO());
                cache.putReference(m, cb);
            }
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            final long address = ((Pointer) ptr).getMemoryIO().getAddress(m.getOffset(ptr));
            
            AbstractInvoker fptr = (AbstractInvoker) cache.getCachedValue(m);
            if (fptr != null && fptr.getAddress() == address) {
                return fptr;
            }

            fptr = Factory.getInstance().newFunction(context.runtime,
                    ((Pointer) ptr).getPointer(context.runtime, m.getOffset(ptr)), (CallbackInfo) m.type);
            cache.putCachedValue(m, fptr);

            return fptr;
        }

        public final boolean isCacheable() {
            return true;
        }

        public final boolean isValueReferenceNeeded() {
            return true;
        }
    }

    

    static final class InnerStructFieldIO implements FieldIO {
        private final StructByValue sbv;

        public InnerStructFieldIO(StructByValue sbv) {
            this.sbv = sbv;
        }

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            if (!(value instanceof Struct)) {
                throw context.runtime.newTypeError(value, context.runtime.getFFI().structClass);
            }

            Struct s = (Struct) value;
            if (!s.getLayout(context).equals(sbv.getStructLayout())) {
                throw context.runtime.newTypeError("incompatible struct layout");
            }

            ByteBuffer src = s.getMemoryIO().asByteBuffer();
            if (src.remaining() != sbv.size) {
                throw context.runtime.newRuntimeError("bad size in " + value.getMetaClass().toString());
            }

            ptr.getMemoryIO().slice(m.offset(), sbv.size).asByteBuffer().put(src);
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            IRubyObject s = cache.getCachedValue(m);
            if (s == null) {
                s = sbv.getStructClass().newInstance(context, ptr.slice(context.runtime, m.getOffset(ptr)), Block.NULL_BLOCK);
                cache.putCachedValue(m, s);
            }

            return s;
        }

        public final boolean isCacheable() {
            return true;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }    
    
    private static MemoryOp getArrayComponentMemoryOp(Type.Array arrayType) {
        Type componentType = arrayType.getComponentType();
        MemoryOp op = componentType instanceof Type.Array
                ? new MultiDimensionArrayOp((Type.Array) componentType)
                : MemoryOp.getMemoryOp(componentType);
        if (op == null) {
            throw arrayType.getRuntime().newNotImplementedError("unsupported array field type: " + arrayType.getComponentType());
        }

        return op;
    }

    
    static final class MultiDimensionArrayOp extends MemoryOp {
        private final Type.Array arrayType;
        private final MemoryOp op;
        
        public MultiDimensionArrayOp(Type.Array arrayType) {
            this.arrayType = arrayType;
            this.op = getArrayComponentMemoryOp(arrayType);
        }
        
        @Override
        IRubyObject get(ThreadContext context, MemoryIO io, long offset) {
            throw context.runtime.newNotImplementedError("cannot get multi deminesional array field");
        }

        @Override
        void put(ThreadContext context, MemoryIO io, long offset, IRubyObject value) {
            if (isCharArray() && value instanceof RubyString) {
                ByteList bl = value.convertToString().getByteList();
                io.putZeroTerminatedByteArray(offset, bl.getUnsafeBytes(), bl.begin(),
                    Math.min(bl.length(), arrayType.length() - 1));
            } else {
                throw context.runtime.newNotImplementedError("cannot set multi deminesional array field");
            }
        }

        @Override
        IRubyObject get(ThreadContext context, AbstractMemory ptr, long offset) {
            return isCharArray()
                    ? new StructLayout.CharArrayProxy(context.runtime, ptr, offset, arrayType, op)
                    : new StructLayout.ArrayProxy(context.runtime, ptr, offset, arrayType, op);
        }
        
        private boolean isCharArray() {
            return arrayType.getComponentType().nativeType == NativeType.CHAR
                    || arrayType.getComponentType().nativeType == NativeType.UCHAR;
        }
    }
    
    static final class ArrayFieldIO implements FieldIO {
        private final Type.Array arrayType;
        private final MemoryOp op;

        public ArrayFieldIO(Type.Array arrayType) {
            this.arrayType = arrayType;
            this.op = getArrayComponentMemoryOp(arrayType);
        }


        public void put(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            
            if (isCharArray() && value instanceof RubyString) {
                ByteList bl = value.convertToString().getByteList();
                int arrayLen = arrayType.length();
                int valueLen = bl.length();
                if(valueLen < arrayLen) {
                    ptr.getMemoryIO().putZeroTerminatedByteArray(m.offset, bl.getUnsafeBytes(), bl.begin(), bl.length());
                } else if (valueLen == arrayLen) {
                    ptr.getMemoryIO().put(m.offset, bl.getUnsafeBytes(), bl.begin(), valueLen);
                } else {
                    throw context.runtime.newIndexError("String is longer (" + valueLen +
                            " bytes) than the char array (" + arrayLen + " bytes)");
                }
            } else if (false) {
                RubyArray ary = value.convertToArray();
                int count = ary.size();
                if (count > arrayType.length()) {
                    throw context.runtime.newIndexError("array too big");
                }
                AbstractMemory memory = (AbstractMemory) ptr;

                // Clear any elements that will not be filled by the array
                if (count < arrayType.length()) {
                    memory.getMemoryIO().setMemory(m.offset + (count * arrayType.getComponentType().getNativeSize()),
                            (arrayType.length() - count) * arrayType.getComponentType().getNativeSize(), (byte) 0);
                }
                
                for (int i = 0; i < count; ++i) {
                    op.put(context, memory, 
                            m.offset + (i * arrayType.getComponentType().getNativeSize()),
                            ary.entry(i));
                }
            } else {
                throw context.runtime.newNotImplementedError("cannot set array field");
            }
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, AbstractMemory ptr) {
            IRubyObject s = cache.getCachedValue(m);
            if (s == null) {
                s = isCharArray()
                    ? new StructLayout.CharArrayProxy(context.runtime, ptr, m.offset, arrayType, op)
                    : new StructLayout.ArrayProxy(context.runtime, ptr, m.offset, arrayType, op);

                cache.putCachedValue(m, s);
            }

            return s;
        }

        private final boolean isCharArray() {
            return arrayType.getComponentType().nativeType == NativeType.CHAR
                    || arrayType.getComponentType().nativeType == NativeType.UCHAR;
        }

        private boolean isVariableLength() {
            return arrayType.length() < 1;
        }


        public final boolean isCacheable() {
            return true;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }

    static final class MappedFieldIO implements FieldIO {
        private final FieldIO nativeFieldIO;
        private final MappedType mappedType;

        public MappedFieldIO(MappedType mappedType, FieldIO nativeFieldIO) {
            this.nativeFieldIO = nativeFieldIO;
            this.mappedType = mappedType;
        }

        /* since we always need to call in to ruby to convert the native value to
         * a ruby value, we cannot cache it here.
         */
        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return nativeFieldIO.isValueReferenceNeeded() || mappedType.isReferenceRequired();
        }

        public final IRubyObject get(ThreadContext context, Storage cache, Member m, AbstractMemory ptr) {
            return mappedType.fromNative(context, nativeFieldIO.get(context, nullStorage, m, ptr));
        }

        public void put(ThreadContext context, Storage cache, Member m, AbstractMemory ptr, IRubyObject value) {
            final IRubyObject nativeValue = mappedType.toNative(context, value);
            nativeFieldIO.put(context, cache, m, ptr, nativeValue);

            if (isValueReferenceNeeded()) {
                // keep references to both the ruby and native values to preserve
                // reference chains
                cache.putReference(m, new Object[] { value, nativeValue });
            }
        }
    }
}
