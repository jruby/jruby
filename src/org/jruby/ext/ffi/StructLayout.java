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
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    private final List<Member> fields;
    
    private final int cacheableFieldCount;
    private final int[] cacheIndexMap;

    private final int referenceFieldCount;
    private final int[] referenceIndexMap;

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

        RubyClass arrayClass = runtime.defineClassUnder("Array", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        arrayClass.includeModule(runtime.getEnumerable());
        arrayClass.defineAnnotatedMethods(Array.class);

        RubyClass fieldClass = runtime.defineClassUnder("Field", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, layoutClass);
        fieldClass.defineAnnotatedMethods(Member.class);

        return layoutClass;
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>.
     * @param fields The fields map for this struct.
     * @param size the total size of the struct.
     * @param minAlign The minimum alignment required when allocating memory.
     */
    StructLayout(Ruby runtime, Collection<RubySymbol> fieldNames, Map<IRubyObject, Member> fields, int size, int minAlign) {
        super(runtime, runtime.fastGetModule("FFI").fastGetClass(CLASS_NAME), NativeType.STRUCT, size, minAlign);
        //
        // fields should really be an immutable map as it is never modified after construction
        //
        this.fieldMap = immutableMap(fields);
        this.cacheIndexMap = new int[fieldNames.size()];
        this.referenceIndexMap = new int[fieldNames.size()];

        int cfCount = 0, refCount = 0;
        List<Member> fieldList = new ArrayList<Member>(fieldNames.size());

        for (RubySymbol fieldName : fieldNames) {
            Member m = fields.get(fieldName);
            fieldList.add(m);
            if (m.isCacheable()) {
                cacheIndexMap[m.index] = cfCount++;
            } else {
                cacheIndexMap[m.index] = -1;
            }
            if (m.isValueReferenceNeeded()) {
                referenceIndexMap[m.index] = refCount++;
            } else {
                referenceIndexMap[m.index] = -1;
            }
        }
        this.cacheableFieldCount = cfCount;
        this.referenceFieldCount = refCount;

        // Create the ordered list of field names from the map
        this.fieldNames = Collections.unmodifiableList(new ArrayList<RubySymbol>(fieldNames));
        this.fields = Collections.unmodifiableList(fieldList);
    }
    
    /**
     * Creates an immutable copy of the map.
     * <p>
     * Copies each entry in <tt>fields</tt>, ensuring that each key is immutable,
     * and returns an immutable map.
     * </p>
     * 
     * @param fields the map of fields to copy
     * @return an immutable copy of <tt>fields</tt>
     */
    private static Map<IRubyObject, Member> immutableMap(Map<IRubyObject, Member> fields) {
        return Collections.unmodifiableMap(new LinkedHashMap<IRubyObject, Member>(fields));
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
        return getMember(context.getRuntime(), name).get(context.getRuntime(), nullStorage, ptr);
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
        getMember(context.getRuntime(), name).put(context.getRuntime(), nullStorage, ptr, value);
        return value;
    }
    
    /**
     * Gets a ruby array of the names of all members of this struct.
     * 
     * @return a <tt>RubyArray</tt> containing the names of all members.
     */
    @JRubyMethod(name = "members")
    public IRubyObject members(ThreadContext context) {
        RubyArray members = RubyArray.newArray(context.getRuntime());
        for (RubySymbol name : fieldNames) {
            members.append(name);
        }
        return members;
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
        return getMember(context.getRuntime(), fieldName);
    }

    @JRubyMethod
    public IRubyObject fields(ThreadContext context) {
        return RubyArray.newArray(context.getRuntime(), fields);
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
        return referenceIndexMap[member.index];
    }

    final int getCacheableFieldCount() {
        return cacheableFieldCount;
    }

    final int getCacheableFieldIndex(Member member) {
        return cacheIndexMap[member.index];
    }

    public final int getFieldCount() {
        return fields.size();
    }

    public final java.util.Collection<Member> getFields() {
        return fields;
    }

    /**
     * A struct member.  This defines the offset within a chunk of memory to use
     * when reading/writing the member, as well as how to convert between the 
     * native representation of the member and the JRuby representation.
     */
    @JRubyClass(name="FFI::StructLayout::Field", parent="Object")
    public static abstract class Member extends RubyObject {
        /** The name of this member. */
        protected final IRubyObject name;

        /** The {@link Type} of this member. */
        protected final Type type;

        /** The offset within the memory area of this member */
        protected final long offset;
        
        /** The index of this member within the struct */
        protected final int index;

        /** Initializes a new Member instance */
        protected Member(IRubyObject name, Type type, int index, long offset) {
            super(type.getRuntime(), type.getRuntime().fastGetModule("FFI").fastGetClass("StructLayout").fastGetClass("Field"));
            this.name = name;
            this.type = type;
            this.index = index;
            this.offset = offset;
        }

        /**
         * Gets the memory I/O accessor for this member.
         * @param ptr The memory area of the struct.
         * @return A memory I/O accessor that can be used to read/write this member.
         */
        static final MemoryIO getMemoryIO(IRubyObject ptr) {
            return ((AbstractMemory) ptr).getMemoryIO();
        }

        final long getOffset(IRubyObject ptr) {
            return offset;
        }

        final int getIndex() {
            return index;
        }

        public final NativeType getNativeType() {
            return type.getNativeType();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Member && ((Member) obj).offset == offset;
        }

        @Override
        public int hashCode() {
            return 53 * 5 + (int) (this.offset ^ (this.offset >>> 32));
        }
        
        /**
         * Writes a ruby value to the native struct member as the appropriate native value.
         *
         * @param runtime The ruby runtime
         * @param cache The value cache
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public abstract void put(Ruby runtime, Storage cache, IRubyObject ptr, IRubyObject value);

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param ptr The struct memory area.
         * @return A ruby object equivalent to the native member value.
         */
        public abstract IRubyObject get(Ruby runtime, Storage cache, IRubyObject ptr);

        /**
         * Gets the cacheable status of this Struct member
         *
         * @return <tt>true</tt> if this member type is cacheable
         */
        protected boolean isCacheable() {
            return false;
        }

        /**
         * Checks if a reference to the ruby object assigned to this field needs to be stored
         *
         * @return <tt>true</tt> if this member type requires the ruby value to be stored.
         */
        protected boolean isValueReferenceNeeded() {
            return false;
        }

        @JRubyMethod
        public IRubyObject size(ThreadContext context) {
            return context.getRuntime().newFixnum(type.getNativeSize());
        }

        @JRubyMethod
        public IRubyObject alignment(ThreadContext context) {
            return context.getRuntime().newFixnum(type.getNativeAlignment());
        }

        @JRubyMethod
        public IRubyObject offset(ThreadContext context) {
            return context.getRuntime().newFixnum(offset);
        }

        @JRubyMethod(name = { "type", "ffi_type" })
        public IRubyObject ffi_type(ThreadContext context) {
            return type;
        }
    }

    public static interface Aggregate {
        public abstract Collection<Member> getMembers();
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
    
    @JRubyClass(name="FFI::StructLayout::Array", parent="Object")
    public static final class Array extends RubyObject {
        private final AbstractMemory ptr;
        private final MemoryOp aio;
        private final long offset;
        private final Type.Array arrayType;

        Array(Ruby runtime, IRubyObject ptr, long offset, Type.Array type, MemoryOp aio) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass(CLASS_NAME).fastGetClass("Array"));
            this.ptr = (AbstractMemory) ptr;
            this.offset = offset;
            this.arrayType = type;
            this.aio = aio;
        }

        private final long getOffset(IRubyObject index) {
            return offset + (Util.uint32Value(index) * arrayType.getComponentType().getNativeSize());
        }
        private final long getOffset(int index) {
            return offset + (index * arrayType.getComponentType().getNativeSize());
        }
        private IRubyObject get(Ruby runtime, int index) {
            return aio.get(runtime, ptr.getMemoryIO(), getOffset(index));
        }
        @JRubyMethod(name = "[]")
        public IRubyObject get(ThreadContext context, IRubyObject index) {
            return aio.get(context.getRuntime(), ptr.getMemoryIO(), getOffset(index));
        }
        @JRubyMethod(name = "[]=")
        public IRubyObject put(ThreadContext context, IRubyObject index, IRubyObject value) {
            aio.put(context.getRuntime(), ptr.getMemoryIO(), getOffset(index), value);
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
            return ptr.slice(context.getRuntime(), offset);
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
}
