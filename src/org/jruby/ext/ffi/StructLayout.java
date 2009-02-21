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
public final class StructLayout extends RubyObject {
    /** The name to use to register this class in the JRuby runtime */
    static final String CLASS_NAME = "StructLayout";

    /** The name:offset map for this struct */
    private final Map<IRubyObject, Member> fields;
    
    /** The ordered list of field names (as symbols) */
    private final List<RubySymbol> fieldNames;

    /** The total size of this struct */
    private final int size;
    
    /** The minimum alignment of memory allocated for structs of this type */
    private final int align;

    private final int cacheableFieldCount;
    private final int[] cacheIndexMap;
    
    private static final class Allocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StructLayout(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new Allocator();
    }
    
    /**
     * Registers the StructLayout class in the JRuby runtime.
     * @param runtime The JRuby runtime to register the new class in.
     * @return The new class
     */
    public static RubyClass createStructLayoutClass(Ruby runtime, RubyModule module) {
        RubyClass result = runtime.defineClassUnder(CLASS_NAME, runtime.getObject(),
                Allocator.INSTANCE, module);
        result.defineAnnotatedMethods(StructLayout.class);
        result.defineAnnotatedConstants(StructLayout.class);
        RubyClass array = runtime.defineClassUnder("Array", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR, result);
        array.includeModule(runtime.getEnumerable());
        array.defineAnnotatedMethods(Array.class);
        return result;
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance using defaults.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>
     */
    StructLayout(Ruby runtime) {
        this(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME));
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>
     * @param klass the ruby class to use for the <tt>StructLayout</tt>
     */
    StructLayout(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        this.size = 0;
        this.align = 1;
        this.fields = Collections.emptyMap();
        this.fieldNames = Collections.emptyList();
        this.cacheableFieldCount = 0;
        this.cacheIndexMap = new int[0];
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>.
     * @param fields The fields map for this struct.
     * @param size the total size of the struct.
     * @param minAlign The minimum alignment required when allocating memory.
     */
    StructLayout(Ruby runtime, Map<IRubyObject, Member> fields, int size, int minAlign) {
        super(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME));
        //
        // fields should really be an immutable map as it is never modified after construction
        //
        this.fields = immutableMap(fields);
        this.size = size;
        this.align = minAlign;
        this.cacheIndexMap = new int[fields.size()];

        int i = 0, cfCount = 0;
        for (Member m : fields.values()) {
            if (m.isCacheable()) {
                cacheIndexMap[m.index] = i++;
                ++cfCount;
            } else {
                cacheIndexMap[m.index] = -1;
            }
        }
        this.cacheableFieldCount = cfCount;

        // Create the ordered list of field names from the map
        List<RubySymbol> names = new ArrayList<RubySymbol>(fields.size());
        for (Map.Entry<IRubyObject, Member> e : fields.entrySet()) {
            if (e.getKey() instanceof RubySymbol) {
                names.add((RubySymbol) e.getKey());
            }
        }
        this.fieldNames = Collections.unmodifiableList(names);
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
        return getMember(context.getRuntime(), name).get(context.getRuntime(), ptr);
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
        getMember(context.getRuntime(), name).put(context.getRuntime(), ptr, value);
        return value;
    }

    /**
     * Gets the value of the struct member corresponding to <tt>name</tt>.
     *
     * @param runtime The ruby runtime.
     * @param struct The struct to read the field for
     * @param name The name of the member.
     * @return A ruby value for the native value of the struct member.
     */
    IRubyObject get(Ruby runtime, Struct struct, IRubyObject name) {
        return getMember(runtime, name).get(runtime, struct);
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
        for (Map.Entry<IRubyObject, Member> e : fields.entrySet()) {
            if (e.getKey() instanceof RubySymbol) {
                RubyArray offset = RubyArray.newArray(runtime);
                // Assemble a [ :name, offset ] array
                offset.append(e.getKey());
                offset.append(runtime.newFixnum(e.getValue().offset));
                offsets.append(offset);
            }
        }
        return offsets;
    }
    
    /**
     * Gets the total size of the struct.
     * 
     * @return The size of the struct in bytes.
     */
    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), size);
    }

    /**
     * Gets the minimum alignment of the struct.
     *
     * @return The minimum alignment of the struct in bytes.
     */
    @JRubyMethod(name = "alignment")
    public IRubyObject aligment(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), getMinimumAlignment());
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

    /**
     * Returns a {@link Member} descriptor for a struct field.
     * 
     * @param name The name of the struct field.
     * @return A <tt>Member</tt> descriptor.
     */
    private Member getMember(Ruby runtime, IRubyObject name) {
        Member f = fields.get(name);
        if (f != null) {
            return f;
        }
        throw runtime.newArgumentError("Unknown field: " + name);
    }

    public final int getMinimumAlignment() {
        return align;
    }

    public final int getSize() {
        return size;
    }

    public final int getFieldCount() {
        return fields.size();
    }

    /**
     * A struct member.  This defines the offset within a chunk of memory to use
     * when reading/writing the member, as well as how to convert between the 
     * native representation of the member and the JRuby representation.
     */
    static abstract class Member {
        /** The offset within the memory area of this member */
        protected final long offset;
        
        /** The index of this member within the struct */
        protected final int index;
        /** Initializes a new Member instance */
        protected Member(int index, long offset) {
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
         * @param ptr The struct memory area.
         * @param value The ruby value to write to the native struct member.
         */
        public abstract void put(Ruby runtime, IRubyObject ptr, IRubyObject value);
        
        /**
         * Reads a ruby value from the struct member.
         * @param ptr The memory area of the struct.
         * @return A ruby object equivalent to the native member value.
         */
        public abstract IRubyObject get(Ruby runtime, IRubyObject ptr);

        /**
         * Reads a ruby value from the struct member.
         *
         * @param cache The cache used to store
         * @param struct The struct to fetch the field for
         * @return A ruby object equivalent to the native member value.
         */
        public IRubyObject get(Ruby runtime, Struct struct) {
            return get(runtime, struct.getMemory());
        }

        /**
         * Gets the cacheable status of this Struct member
         *
         * @return <tt>true</tt> if this member type is cacheable
         */
        protected boolean isCacheable() {
            return false;
        }
    }

    static final class Cache {
        private final int[] cacheIndexMap;
        private final IRubyObject[] array;

        Cache(StructLayout layout) {
            this.cacheIndexMap = layout.cacheIndexMap;
            this.array = new IRubyObject[layout.cacheableFieldCount];
        }
        public IRubyObject get(Member member) {
            return cacheIndexMap[member.index] != -1 ? array[cacheIndexMap[member.index]] : null;
        }
        public void put(Member member, IRubyObject value) {
            if (cacheIndexMap[member.index] != -1) {
                array[cacheIndexMap[member.index]] = value;
            }
        }
    }

    static abstract class ArrayMemberIO {
        static final MemoryIO getMemoryIO(IRubyObject ptr) {
            return ((AbstractMemory) ptr).getMemoryIO();
        }
        static final long getOffset(IRubyObject ptr, long offset) {
            return offset;
        }
        abstract IRubyObject get(Ruby runtime, MemoryIO io, long offset);
        abstract void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value);
    }
    @JRubyClass(name="FFI::StructLayout::Array", parent="Object")
    static final class Array extends RubyObject {
        private final AbstractMemory ptr;
        private final ArrayMemberIO aio;
        private final long offset;
        private final int length, typeSize;
        /**
         * Creates a new <tt>StructLayout</tt> instance.
         *
         * @param runtime The runtime for the <tt>StructLayout</tt>
         * @param klass the ruby class to use for the <tt>StructLayout</tt>
         */
        Array(Ruby runtime, RubyClass klass) {
            this(runtime, null, 0, 0, 0, null);
        }
        Array(Ruby runtime, IRubyObject ptr, long offset, int length, int sizeBits, ArrayMemberIO aio) {
            super(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME).fastGetClass("Array"));
            this.ptr = (AbstractMemory) ptr;
            this.offset = offset;
            this.length = length;
            this.aio = aio;
            this.typeSize = sizeBits / 8;
        }
        private final long getOffset(IRubyObject index) {
            return offset + (Util.uint32Value(index) * typeSize);
        }
        private final long getOffset(int index) {
            return offset + (index * typeSize);
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
            IRubyObject[] elems = new IRubyObject[length];
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
            return context.getRuntime().newFixnum(length * typeSize);
        }
        /**
         * Needed for Enumerable implementation
         */
        @JRubyMethod(name = "each", frame = true)
        public IRubyObject each(ThreadContext context, Block block) {
            if (!block.isGiven()) {
                throw context.getRuntime().newLocalJumpErrorNoBlock();
            }
            for (int i = 0; i < length; ++i) {
                block.yield(context, get(context.getRuntime(), i));
            }
            return this;
        }
    }
}
