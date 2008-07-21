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

import java.util.Collections;
import java.util.LinkedHashMap;
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
    
    /** The total size of this struct */
    private final int size;
    
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
    public static RubyClass createStructLayoutClass(Ruby runtime) {
        RubyModule parent = runtime.getModule(FFIProvider.MODULE_NAME);
        RubyClass result = runtime.defineClassUnder(CLASS_NAME, runtime.getObject(),
                Allocator.INSTANCE, parent);
        result.defineAnnotatedMethods(StructLayout.class);
        result.defineAnnotatedConstants(StructLayout.class);

        return result;
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance using defaults.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>
     */
    StructLayout(Ruby runtime) {
        this(runtime, runtime.fastGetModule(FFIProvider.MODULE_NAME).fastGetClass(CLASS_NAME));
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
        this.fields = Collections.emptyMap();
    }
    
    /**
     * Creates a new <tt>StructLayout</tt> instance.
     * 
     * @param runtime The runtime for the <tt>StructLayout</tt>.
     * @param fields The fields map for this struct.
     * @param size the total size of the struct.
     */
    StructLayout(Ruby runtime, Map<IRubyObject, Member> fields, int size) {
        super(runtime, runtime.fastGetModule(FFIProvider.MODULE_NAME).fastGetClass(CLASS_NAME));
        //
        // fields should really be an immutable map as it is never modified after construction
        //
        this.fields = immutableMap(fields);
        this.size = size;
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
        Map<IRubyObject, Member> tmp = new LinkedHashMap<IRubyObject, Member>(fields.size());
        for (Map.Entry<IRubyObject, Member> e : fields.entrySet()) {
            tmp.put(convertKey(e.getKey().getRuntime(), e.getKey()), e.getValue());
        }
        return Collections.unmodifiableMap(tmp);
    }
    
    /**
     * Converts a non-symbol key into a <tt>RubySymbol</tt>.
     * 
     * @param key The key to convert to a <tt>RubySymbol</tt>.
     * @return A <tt>RubySymbol</tt> equivalent to the key.
     */
    private static IRubyObject convertKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubySymbol) {
            return key;
        }
        return runtime.getSymbolTable().getSymbol(key.asJavaString());
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
     * Gets a ruby array of the names of all members of this struct.
     * 
     * @return a <tt>RubyArray</tt> containing the names of all members.
     */
    @JRubyMethod(name = "members")
    public IRubyObject members(ThreadContext context) {
        return RubyArray.newArray(context.getRuntime(), fields.keySet());
    }
    
    /**
     * Gets the total size of the struct.
     * 
     * @return The size of the struct.
     */
    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return RubyFixnum.newFixnum(context.getRuntime(), size);
    }
    /**
     * Returns a {@link Member} descriptor for a struct field.
     * 
     * @param name The name of the struct field.
     * @return A <tt>Member</tt> descriptor.
     */
    private Member getMember(Ruby runtime, IRubyObject name) {
        Member f = fields.get(convertKey(runtime, name));
        if (f != null) {
            return f;
        }
        throw runtime.newArgumentError("Unknown field: " + name);
    }
    
    /**
     * A struct member.  This defines the offset within a chunk of memory to use
     * when reading/writing the member, as well as how to convert between the 
     * native representation of the member and the JRuby representation.
     */
    static abstract class Member {
        /** The offset within the memory area of this member */
        protected final long offset;
        
        /** Initializes a new Member instance */
        protected Member(long offset) {
            this.offset = offset;
        }
        /**
         * Gets the memory I/O accessor for this member.
         * @param ptr The memory area of the struct.
         * @return A memory I/O accessor that can be used to read/write this member.
         */
        static final MemoryIO getMemoryIO(IRubyObject ptr) {
            return ((AbstractMemoryPointer) ptr).getMemoryIO();
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
    }
    
}
