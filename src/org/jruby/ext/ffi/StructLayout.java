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
 *Inner
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Defines the memory layout for a native structure.
 */
@JRubyClass(name=StructLayout.CLASS_NAME, parent="Object")
public final class StructLayout extends Type {
    static final Storage nullStorage = new NullStorage();
    
    /** The name to use to register this class in the JRuby runtime */
    static final String CLASS_NAME = "StructLayout";

    /** The name:offset map for this struct */
    private final Map<IRubyObject, Member> fieldMap;
    
    /** The ordered list of field names (as symbols) */
    private final List<RubySymbol> fieldNames;

    /** The ordered list of fields */
    private final List<Field> fields;

    /** The ordered list of fields */
    private final List<Member> members;

    /** The number of cacheable fields in this struct */
    private final int cacheableFieldCount;

    /** The number of reference fields in this struct */
    private final int referenceFieldCount;

    /**
     * Registers the StructLayout class in the JRuby runtime.
     * @param runtime The JRuby runtime to register the new class in.
     * @return The new class
     */
    public static RubyClass createStructLayoutClass(Ruby runtime, RubyModule module) {
        RubyClass layoutClass = runtime.defineClassUnder(CLASS_NAME, module.fastGetClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, module);
        layoutClass.defineAnnotatedMethods(StructLayout.class);
        layoutClass.defineAnnotatedConstants(StructLayout.class);

        RubyClass arrayClass = runtime.defineClassUnder("ArrayProxy", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        arrayClass.includeModule(runtime.getEnumerable());
        arrayClass.defineAnnotatedMethods(ArrayProxy.class);

        RubyClass charArrayClass = runtime.defineClassUnder("CharArrayProxy", arrayClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        charArrayClass.defineAnnotatedMethods(CharArrayProxy.class);

        RubyClass fieldClass = runtime.defineClassUnder("Field", runtime.getObject(),
                FieldAllocator.INSTANCE, layoutClass);
        fieldClass.defineAnnotatedMethods(Field.class);

        RubyClass scalarFieldClass = runtime.defineClassUnder("Scalar", fieldClass,
                ScalarFieldAllocator.INSTANCE, layoutClass);
        scalarFieldClass.defineAnnotatedMethods(ScalarField.class);

        RubyClass enumFieldClass = runtime.defineClassUnder("Enum", fieldClass,
                EnumFieldAllocator.INSTANCE, layoutClass);
        enumFieldClass.defineAnnotatedMethods(EnumField.class);

        RubyClass stringFieldClass = runtime.defineClassUnder("String", fieldClass,
                StringFieldAllocator.INSTANCE, layoutClass);
        stringFieldClass.defineAnnotatedMethods(StringField.class);

        RubyClass pointerFieldClass = runtime.defineClassUnder("Pointer", fieldClass,
                PointerFieldAllocator.INSTANCE, layoutClass);
        pointerFieldClass.defineAnnotatedMethods(PointerField.class);

        RubyClass functionFieldClass = runtime.defineClassUnder("Function", fieldClass,
                FunctionFieldAllocator.INSTANCE, layoutClass);
        functionFieldClass.defineAnnotatedMethods(FunctionField.class);

        RubyClass innerStructFieldClass = runtime.defineClassUnder("InnerStruct", fieldClass,
                InnerStructFieldAllocator.INSTANCE, layoutClass);
        innerStructFieldClass.defineAnnotatedMethods(InnerStructField.class);

        RubyClass arrayFieldClass = runtime.defineClassUnder("Array", fieldClass,
                ArrayFieldAllocator.INSTANCE, layoutClass);
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
    StructLayout(Ruby runtime, Collection<RubySymbol> fieldNames, Map<IRubyObject, Field> fields, int size, int alignment) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout"), fieldNames, fields, size, alignment);
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>.
     * @param fields The fields map for this struct.
     * @param size the total size of the struct.
     * @param alignment The minimum alignment required when allocating memory.
     */
    StructLayout(Ruby runtime, RubyClass klass, Collection<RubySymbol> fieldNames, Map<IRubyObject, Field> fields, int size, int alignment) {
        super(runtime, klass, NativeType.STRUCT, size, alignment);
        
        int cfCount = 0, refCount = 0;
        List<Field> fieldList = new ArrayList<Field>(fieldNames.size());
        Map<IRubyObject, Member> memberMap = new LinkedHashMap<IRubyObject, Member>(fieldNames.size());

        int index = 0;
        for (RubySymbol fieldName : fieldNames) {
            Field f = fields.get(fieldName);
            fieldList.add(f);
            int cfIndex = f.isCacheable() ? cfCount++ : -1;
            int refIndex = f.isValueReferenceNeeded() ? refCount++ : -1;
            
            memberMap.put(fieldName, new Member(f, index, cfIndex, refIndex));
            fieldList.add(f);
        }

        
        this.cacheableFieldCount = cfCount;
        this.referenceFieldCount = refCount;

        // Create the ordered list of field names from the map
        this.fieldNames = Collections.unmodifiableList(new ArrayList<RubySymbol>(fieldNames));
        this.fields = Collections.unmodifiableList(fieldList);
        this.fieldMap = Collections.unmodifiableMap(memberMap);
        this.members = Collections.unmodifiableList(new ArrayList(memberMap.values()));
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
        RubyArray mbrs = RubyArray.newArray(context.getRuntime());
        for (RubySymbol name : fieldNames) {
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
        Ruby runtime = context.getRuntime();
        RubyArray offsets = RubyArray.newArray(runtime);

        for (RubySymbol name : fieldNames) {
            RubyArray offset = RubyArray.newArray(runtime);
            // Assemble a [ :name, offset ] array
            offset.append(name);
            offset.append(runtime.newFixnum(fieldMap.get(name).offset));
            offsets.append(offset);
        }

        return offsets;
    }
    
    /**
     * Gets the total size of the struct.
     * 
     * @return The size of the struct in bytes.
     */
    @JRubyMethod(name = "size")
    @Override
    public IRubyObject size(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), getNativeSize());
    }

    /**
     * Gets the minimum alignment of the struct.
     *
     * @return The minimum alignment of the struct in bytes.
     */
    @JRubyMethod(name = "alignment")
    @Override
    public IRubyObject alignment(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), getNativeAlignment());
    }

    /**
     * Gets the offset of a member of the struct.
     *
     * @return The offset of the member within the struct memory, in bytes.
     */
    @JRubyMethod(name = "offset_of")
    public IRubyObject offset_of(ThreadContext context, IRubyObject fieldName) {
        final Member member = getMember(context.getRuntime(), fieldName);
        return RubyFixnum.newFixnum(context.getRuntime(), member.offset);
    }
    
    @JRubyMethod(name = "[]")
    public IRubyObject aref(ThreadContext context, IRubyObject fieldName) {
        return getField(context.getRuntime(), fieldName);
    }

    @JRubyMethod
    public IRubyObject fields(ThreadContext context) {
        return RubyArray.newArray(context.getRuntime(), fields);
    }

    final IRubyObject getValue(ThreadContext context, IRubyObject name, Storage cache, IRubyObject ptr) {
        return getMember(context.getRuntime(), name).get(context, cache, ptr);
    }

    final void putValue(ThreadContext context, IRubyObject name, Storage cache, IRubyObject ptr, IRubyObject value) {
        getMember(context.getRuntime(), name).put(context, cache, ptr, value);
    }

    /**
     * Returns a {@link Member} descriptor for a struct field.
     * 
     * @param name The name of the struct field.
     * @return A <tt>Member</tt> descriptor.
     */
    final Member getMember(Ruby runtime, IRubyObject name) {
        Member f = fieldMap.get(name);
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

    public final int getMinimumAlignment() {
        return getNativeAlignment();
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

        /** Initializes a new Member instance */
        protected Member(Field f, int index, int cacheIndex, int referenceIndex) {
            this.field = f;
            this.io = f.io;
            this.type = f.type;
            this.offset = f.offset;
            this.index = index;
            this.cacheIndex = cacheIndex;
            this.referenceIndex = referenceIndex;
        }

        /**
         * Gets the memory I/O accessor for this member.
         * @param ptr The memory area of the struct.
         * @return A memory I/O accessor that can be used to read/write this member.
         */
        final MemoryIO getMemoryIO(IRubyObject ptr) {
            return ((AbstractMemory) ptr).getMemoryIO();
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
            return 53 * 5 + (int) (this.offset ^ (this.offset >>> 32)) + 37 * type.hashCode();
        }
        
        /**
         * Writes a ruby value to the native struct member as the appropriate native value.
         *
         * @param runtime The ruby runtime
         * @param cache The value cache
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public final void put(ThreadContext context, Storage cache, IRubyObject ptr, IRubyObject value) {
            io.put(context, cache, this, ptr, value);
        }

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param ptr The struct memory area.
         * @return A ruby object equivalent to the native member value.
         */
        public final IRubyObject get(ThreadContext context, Storage cache, IRubyObject ptr) {
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
         * @param runtime The ruby runtime
         * @param cache The value cache
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public abstract void put(ThreadContext context, Storage cache, Member m, IRubyObject ptr, IRubyObject value);

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param ptr The struct memory area.
         * @return A ruby object equivalent to the native member value.
         */
        public abstract IRubyObject get(ThreadContext context, Storage cache, Member m, IRubyObject ptr);

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

        public IRubyObject get(ThreadContext context, Storage cache, Member m, IRubyObject ptr) {
            return m.field.callMethod(context, "get", ptr);
        }

        public void put(ThreadContext context, Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            m.field.callMethod(context, "put", new IRubyObject[] { ptr, value });
        }

        public final boolean isCacheable() {
            return false;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }

    private static final class FieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Field(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new FieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::Field", parent="Object")
    public static class Field extends RubyObject {

        /** The basic ops to read/write this field */
        FieldIO io;

        /** The {@link Type} of this member. */
        private Type type;

        /** The offset within the memory area of this member */
        private int offset;


        Field(Ruby runtime, RubyClass klass) {
            this(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        Field(Ruby runtime, RubyClass klass, FieldIO io) {
            this(runtime, klass, (Type) runtime.fastGetModule("FFI").fastGetClass("Type").fastGetConstant("VOID"),
                    -1, io);
            
        }
        
        Field(Ruby runtime, RubyClass klass, Type type, int offset, FieldIO io) {
            super(runtime, klass);
            this.type = type;
            this.offset = offset;
            this.io = io;
        }

        void init(IRubyObject type, IRubyObject offset) {
            this.type = checkType(type);
            this.offset = RubyNumeric.num2int(offset);
        }

        void init(IRubyObject type, IRubyObject offset, FieldIO io) {
            init(type, offset);
            this.io = io;
        }

        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject offset) {
            init(type, offset);

            return this;
        }

        final Type checkType(IRubyObject type) {
            if (!(type instanceof Type)) {
                throw getRuntime().newTypeError(type, getRuntime().fastGetModule("FFI").fastGetClass("Type"));
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
            return obj instanceof Field && ((Field) obj).offset == offset;
        }

        @Override
        public int hashCode() {
            return 53 * 5 + (int) (this.offset ^ (this.offset >>> 32));
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

        @JRubyMethod
        public final IRubyObject size(ThreadContext context) {
            return context.getRuntime().newFixnum(type.getNativeSize());
        }

        @JRubyMethod
        public final IRubyObject alignment(ThreadContext context) {
            return context.getRuntime().newFixnum(type.getNativeAlignment());
        }

        @JRubyMethod
        public final IRubyObject offset(ThreadContext context) {
            return context.getRuntime().newFixnum(offset);
        }

        @JRubyMethod(name = { "type", "ffi_type" })
        public final IRubyObject type(ThreadContext context) {
            return type;
        }
    }

    private static final class ScalarFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new ScalarField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new ScalarFieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::Scalar", parent="FFI::StructLayout::Field")
    public static final class ScalarField extends Field {

        public ScalarField(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
        }

        public ScalarField(Ruby runtime, Type.Builtin type, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Scalar"),
                    type, offset, new ScalarFieldIO(type));
        }

        @Override
        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject offset) {

            init(type, offset, new ScalarFieldIO(checkType(type)));

            return this;
        }
    }

    private static final class EnumFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new EnumField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new EnumFieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::Enum", parent="FFI::StructLayout::Field")
    public static final class EnumField extends Field {

        public EnumField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, EnumFieldIO.INSTANCE);
        }

        public EnumField(Ruby runtime, Enum type, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Enum"),
                    type, offset, EnumFieldIO.INSTANCE);
        }
    }

    private static final class StringFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StringField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new StringFieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::String", parent="FFI::StructLayout::Field")
    static final class StringField extends Field {

        public StringField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, StringFieldIO.INSTANCE);
        }

        public StringField(Ruby runtime, Type.Builtin type, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("String"), 
                    type, offset, StringFieldIO.INSTANCE);
        }
    }

    private static final class PointerFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new PointerField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new PointerFieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::Pointer", parent="FFI::StructLayout::Field")
    public static final class PointerField extends Field {

        public PointerField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, PointerFieldIO.INSTANCE);
        }
        
        public PointerField(Ruby runtime, Type.Builtin type, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Pointer"), 
                    type, offset, PointerFieldIO.INSTANCE);
        }
    }

    private static final class FunctionFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new FunctionField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new FunctionFieldAllocator();
    }

    @JRubyClass(name="FFI::StructLayout::Function", parent="FFI::StructLayout::Field")
    public static final class FunctionField extends Field {

        public FunctionField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        public FunctionField(Ruby runtime, CallbackInfo sbv, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Function"),
                    sbv, offset, FunctionFieldIO.INSTANCE);
        }

        @Override
        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject offset) {
            if (!(type instanceof CallbackInfo)) {
                throw context.getRuntime().newTypeError(type, context.getRuntime().fastGetModule("FFI").fastGetClass("Type").fastGetClass("Function"));
            }
            init(type, offset, new FunctionFieldIO());

            return this;
        }
    }

    private static final class InnerStructFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new InnerStructField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new InnerStructFieldAllocator();
    }
    
    @JRubyClass(name="FFI::StructLayout::InnerStruct", parent="FFI::StructLayout::Field")
    public static final class InnerStructField extends Field {

        public InnerStructField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        public InnerStructField(Ruby runtime, StructByValue sbv, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("InnerStruct"),
                    sbv, offset, new InnerStructFieldIO(sbv));
        }

        @Override
        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject offset) {
            if (!(type instanceof StructByValue)) {
                throw context.getRuntime().newTypeError(type,
                        context.getRuntime().fastGetModule("FFI").fastGetClass("Type").fastGetClass("Struct"));
            }
            init(type, offset, new InnerStructFieldIO((StructByValue) type));

            return this;
        }
    }

    private static final class ArrayFieldAllocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new ArrayField(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new ArrayFieldAllocator();
    }
    
    @JRubyClass(name="FFI::StructLayout::Array", parent="FFI::StructLayout::Field")
    public static final class ArrayField extends Field {

        public ArrayField(Ruby runtime, RubyClass klass) {
            super(runtime, klass, DefaultFieldIO.INSTANCE);
        }

        public ArrayField(Ruby runtime, Type.Array arrayType, int offset) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Array"),
                    arrayType, offset, new ArrayFieldIO(arrayType));
        }

        @Override
        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject offset) {
            if (!(type instanceof Type.Array)) {
                throw context.getRuntime().newTypeError(type,
                        context.getRuntime().fastGetModule("FFI").fastGetClass("Type").fastGetClass("Array"));
            }
            init(type, offset, new ArrayFieldIO((Type.Array) type));

            return this;
        }
    }


    public static interface Storage {
        IRubyObject getCachedValue(Member member);
        void putCachedValue(Member member, IRubyObject value);
        void putReference(Member member, IRubyObject value);
    }

    static class NullStorage implements Storage {
        public IRubyObject getCachedValue(Member member) { return null; }
        public void putCachedValue(Member member, IRubyObject value) { }
        public void putReference(Member member, IRubyObject value) { }
    }
    
    @JRubyClass(name="FFI::StructLayout::ArrayProxy", parent="Object")
    public static class ArrayProxy extends RubyObject {
        protected final AbstractMemory ptr;
        final MemoryOp aio;
        protected final Type.Array arrayType;

        ArrayProxy(Ruby runtime, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            this(runtime, runtime.fastGetModule("FFI").fastGetClass(CLASS_NAME).fastGetClass("ArrayProxy"),
                    ptr, offset, type, aio);
        }

        ArrayProxy(Ruby runtime, RubyClass klass, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            super(runtime, klass);
            this.ptr = ((AbstractMemory) ptr).slice(runtime, offset, type.getNativeSize());
            this.arrayType = type;
            this.aio = aio;
        }

        private final long getOffset(IRubyObject index) {
            return getOffset(Util.int32Value(index));
        }

        private final long getOffset(int index) {
            if (index < 0 || index >= arrayType.length()) {
                throw getRuntime().newIndexError("index " + index + " out of bounds");
            }

            return (long) (index * arrayType.getComponentType().getNativeSize());
        }

        private IRubyObject get(Ruby runtime, int index) {
            return aio.get(runtime, ptr, getOffset(index));
        }

        @JRubyMethod(name = "[]")
        public IRubyObject get(ThreadContext context, IRubyObject index) {
            return aio.get(context.getRuntime(), ptr, getOffset(index));
        }

        @JRubyMethod(name = "[]=")
        public IRubyObject put(ThreadContext context, IRubyObject index, IRubyObject value) {
            aio.put(context.getRuntime(), ptr, getOffset(index), value);
            return value;
        }

        @JRubyMethod(name = { "to_a", "to_ary" })
        public IRubyObject get(ThreadContext context) {
            Ruby runtime = context.getRuntime();

            IRubyObject[] elems = new IRubyObject[arrayType.length()];
            for (int i = 0; i < elems.length; ++i) {
                elems[i] = get(runtime, i);
            }

            return RubyArray.newArrayNoCopy(runtime, elems);
        }

        @JRubyMethod(name = { "to_ptr" })
        public IRubyObject to_ptr(ThreadContext context) {
            return ptr;
        }

        @JRubyMethod(name = { "size" })
        public IRubyObject size(ThreadContext context) {
            return context.getRuntime().newFixnum(arrayType.getNativeSize());
        }
        /**
         * Needed for Enumerable implementation
         */
        @JRubyMethod(name = "each", frame = true)
        public IRubyObject each(ThreadContext context, Block block) {
            if (!block.isGiven()) {
                throw context.getRuntime().newLocalJumpErrorNoBlock();
            }
            for (int i = 0; i < arrayType.length(); ++i) {
                block.yield(context, get(context.getRuntime(), i));
            }
            return this;
        }

        
    }

    @JRubyClass(name="FFI::StructLayout::CharArrayProxy", parent="FFI::StructLayout::ArrayProxy")
    public static final class CharArrayProxy extends ArrayProxy {
        CharArrayProxy(Ruby runtime, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("CharArrayProxy"),
                    ptr, offset, type, aio);
        }

        @JRubyMethod(name = { "to_s" })
        public IRubyObject to_s(ThreadContext context) {
            return MemoryUtil.getTaintedString(context.getRuntime(), ptr.getMemoryIO(), 0, arrayType.length());
        }
    }

    /**
     * Primitive (byte, short, int, long, float, double) types are all handled by
     * a PrimitiveMember type.
     */
    static final class ScalarFieldIO implements FieldIO {
        private final MemoryOp op;

        ScalarFieldIO(Type type) {
            this.op = MemoryOp.getMemoryOp(type);
        }
        
        ScalarFieldIO(MemoryOp op) {
            this.op = op;
        }

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            op.put(context.getRuntime(), m.getMemoryIO(ptr), m.offset, value);
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            return op.get(context.getRuntime(), m.getMemoryIO(ptr), m.offset);
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
        public static final FieldIO INSTANCE = new EnumFieldIO();

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            // Upcall to ruby to convert :foo to an int, then write it out
            m.getMemoryIO(ptr).putInt(m.offset,
                    RubyNumeric.num2int(m.type.callMethod(context, "find", value)));
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            // Read an int from the native memory, then upcall to the ruby value
            // lookup code to convert it to the appropriate symbol
            return m.type.callMethod(context, "find",
                    context.getRuntime().newFixnum(m.getMemoryIO(ptr).getInt(m.offset)));
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
        
        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            if (value instanceof Pointer) {
                m.getMemoryIO(ptr).putMemoryIO(m.offset, ((Pointer) value).getMemoryIO());
            } else if (value instanceof Struct) {
                MemoryIO mem = ((Struct) value).getMemoryIO();

                if (!(mem instanceof DirectMemoryIO)) {
                    throw context.getRuntime().newArgumentError("Struct memory not backed by a native pointer");
                }
                m.getMemoryIO(ptr).putMemoryIO(m.offset, mem);

            } else if (value instanceof RubyInteger) {
                m.getMemoryIO(ptr).putAddress(m.offset, Util.int64Value(ptr));
            } else if (value.respondsTo("to_ptr")) {
                IRubyObject addr = value.callMethod(context, "to_ptr");
                if (addr instanceof Pointer) {
                    m.getMemoryIO(ptr).putMemoryIO(m.offset, ((Pointer) addr).getMemoryIO());
                } else {
                    throw context.getRuntime().newArgumentError("Invalid pointer value");
                }
            } else if (value.isNil()) {
                m.getMemoryIO(ptr).putAddress(m.offset, 0L);
            } else {
                throw context.getRuntime().newArgumentError("Invalid pointer value");
            }
            cache.putReference(m, value);
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            DirectMemoryIO memory = ((AbstractMemory) ptr).getMemoryIO().getMemoryIO(m.getOffset(ptr));
            IRubyObject old = cache.getCachedValue(m);
            if (old instanceof Pointer) {
                MemoryIO oldMemory = ((Pointer) old).getMemoryIO();
                if (memory.equals(oldMemory)) {
                    return old;
                }
            }
            Pointer retval = new Pointer(context.getRuntime(), memory);
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
        
        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            MemoryIO io = m.getMemoryIO(ptr).getMemoryIO(m.getOffset(ptr));
            if (io == null || io.isNull()) {
                return context.getRuntime().getNil();
            }

            return RubyString.newStringNoCopy(context.getRuntime(), io.getZeroTerminatedByteArray(0));
        }

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            ByteList bl = value.convertToString().getByteList();

            MemoryPointer mem = MemoryPointer.allocate(context.getRuntime(), 1, bl.length() + 1, false);
            //
            // Keep a reference to the temporary memory in the cache so it does
            // not get freed by the GC until the struct is freed
            //
            cache.putReference(m, mem);

            MemoryIO io = mem.getMemoryIO();
            io.put(0, bl.getUnsafeBytes(), bl.begin(), bl.length());
            io.putByte(bl.length(), (byte) 0);

            m.getMemoryIO(ptr).putMemoryIO(m.getOffset(ptr), io);
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

        public void put(ThreadContext context, Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            if (value.isNil()) {
                m.getMemoryIO(ptr).putAddress(m.getOffset(ptr), 0L);
            } else {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(context.getRuntime(), (CallbackInfo) m.type, value);
                m.getMemoryIO(ptr).putMemoryIO(m.getOffset(ptr), cb.getMemoryIO());
                cache.putCachedValue(m, cb);
                cache.putReference(m, cb);
            }
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            return Factory.getInstance().newFunction(context.getRuntime(), ((Pointer) ptr).getPointer(context.getRuntime(), m.getOffset(ptr)), (CallbackInfo) m.type);
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

        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            throw context.getRuntime().newNotImplementedError("Cannot set Struct fields");
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            IRubyObject s = cache.getCachedValue(m);
            if (s == null) {
                s = sbv.getStructClass().newInstance(context,
                        new IRubyObject[] { ((AbstractMemory) ptr).slice(context.getRuntime(), m.getOffset(ptr)) },
                        Block.NULL_BLOCK);
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

    static final class ArrayFieldIO implements FieldIO {
        private final Type.Array arrayType;
        private final MemoryOp op;

        public ArrayFieldIO(Type.Array arrayType) {
            this.arrayType = arrayType;
            this.op = MemoryOp.getMemoryOp(arrayType.getComponentType());

            if (op == null) {
                throw arrayType.getRuntime().newNotImplementedError("unsupported array field type: " + arrayType);
            }
        }


        public void put(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr, IRubyObject value) {
            
            if (isCharArray() && value instanceof RubyString) {
                ByteList bl = value.convertToString().getByteList();
                m.getMemoryIO(ptr).putZeroTerminatedByteArray(m.offset, bl.getUnsafeBytes(), bl.begin(),
                    Math.min(bl.length(), arrayType.length() - 1));

            } else if (false) {
                RubyArray ary = value.convertToArray();
                int count = ary.size();
                if (count > arrayType.length()) {
                    throw context.getRuntime().newIndexError("array too big");
                }
                AbstractMemory memory = (AbstractMemory) ptr;

                // Clear any elements that will not be filled by the array
                if (count < arrayType.length()) {
                    memory.getMemoryIO().setMemory(m.offset + (count * arrayType.getComponentType().getNativeSize()),
                            (arrayType.length() - count) * arrayType.getComponentType().getNativeSize(), (byte) 0);
                }
                
                for (int i = 0; i < count; ++i) {
                    op.put(context.getRuntime(), memory, m.offset + (i * arrayType.getComponentType().getNativeSize()),
                            ary.entry(i));
                }
            } else {
                throw context.getRuntime().newNotImplementedError("cannot set array field");
            }
        }

        public IRubyObject get(ThreadContext context, StructLayout.Storage cache, Member m, IRubyObject ptr) {
            IRubyObject s = cache.getCachedValue(m);
            if (s == null) {
                s = isCharArray()
                        ? new StructLayout.CharArrayProxy(context.getRuntime(), ptr, m.offset, arrayType, op)
                        : new StructLayout.ArrayProxy(context.getRuntime(), ptr, m.offset, arrayType, op);
                cache.putCachedValue(m, s);
            }

            return s;
        }

        private final boolean isCharArray() {
            return arrayType.getComponentType().nativeType == NativeType.CHAR
                    || arrayType.getComponentType().nativeType == NativeType.UCHAR;
        }


        public final boolean isCacheable() {
            return true;
        }

        public final boolean isValueReferenceNeeded() {
            return false;
        }
    }
}
