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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * C memory pointer operations.
 * <p>
 * This implements the Rubinius FFI MemoryPointer class, but in java for speed.
 * </p>
 */
@JRubyClass(name=AbstractMemoryPointer.className, parent=AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public abstract class AbstractMemoryPointer extends AbstractMemory {
    /** The base class name to register in the Ruby runtime */
    public static final String className = "AbstractMemoryPointer";
    /**
     * Used to hold a permanent reference to a memory pointer so it does not get
     * garbage collected
     */
    private static final Map<AbstractMemoryPointer, Object> pointerSet
            = new ConcurrentHashMap();
    
    public static RubyClass createMemoryPointerClass(Ruby runtime) {
        RubyModule module = FFIProvider.getModule(runtime);
        RubyClass result = module.defineClassUnder(className, 
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        result.defineAnnotatedMethods(AbstractMemoryPointer.class);
        result.defineAnnotatedConstants(AbstractMemoryPointer.class);

        return result;
    }
    protected AbstractMemoryPointer(Ruby runtime, RubyClass klass) {
        this(runtime, klass, 0, Long.MAX_VALUE);
    }
    protected AbstractMemoryPointer(Ruby runtime, RubyClass klass, long offset, long size) {
        super(runtime, klass, offset, size);
    }
    
    /**
     * Tests if this <tt>MemoryPointer</tt> represents the C <tt>NULL</tt> value.
     *
     * @return true if the address is NULL.
     */
    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.getRuntime().newBoolean(getMemoryIO().isNull());
    }
    
    /**
     * Reads a pointer value from the memory address.
     *
     * @return A new <tt>MemoryPointer</tt>.
     */
    @JRubyMethod(name = "read_pointer")
    public IRubyObject read_pointer(ThreadContext context) {
        return getMemoryPointer(context.getRuntime(), 0);
    }
        
    @JRubyMethod(name = "get_string", required = 1, optional = 1)
    public IRubyObject get_string(ThreadContext context, IRubyObject[] args) {
        long off = getOffset(args[0]);
        int len = 0;
        if (args.length > 1) {
            int maxlen = Util.int32Value(args[1]);
            len = (int) getMemoryIO().indexOf(off, (byte) 0, maxlen);
            if (len < 0 || len > maxlen) {
                len = maxlen;
            }
        } else {
            len = (int) getMemoryIO().indexOf(off, (byte) 0);
        }
        ByteList bl = new ByteList(len);
        getMemoryIO().get(off, bl.unsafeBytes(), bl.begin(), len);
        bl.length(len);
        return context.getRuntime().newString(bl);
    }
    @JRubyMethod(name = "put_string", required = 2, optional = 1)
    public IRubyObject put_string(ThreadContext context, IRubyObject[] args) {
        long off = getOffset(args[0]);
        ByteList bl = args[1].convertToString().getByteList();
        int len = bl.length();
        if (args.length > 2) {
            len = Math.min(Util.int32Value(args[2]) - 1, len);
        }
        getMemoryIO().put(off, bl.unsafeBytes(), bl.begin(), len);
        getMemoryIO().putByte(off + bl.length(), (byte) 0);
        return context.getRuntime().newFixnum(len);
    }
    @JRubyMethod(name = "get_buffer", required = 2)
    public IRubyObject get_buffer(ThreadContext context, IRubyObject off, IRubyObject len_) {
        int len = Util.int32Value(len_);
        ByteList bl = new ByteList(len);
        getMemoryIO().get(getOffset(off), bl.unsafeBytes(), bl.begin(), len);
        bl.length(len);
        return context.getRuntime().newString(bl);
    }
    @JRubyMethod(name = "put_buffer", required = 3)
    public IRubyObject put_buffer(ThreadContext context, IRubyObject off, IRubyObject str, IRubyObject len_) {
        ByteList bl = str.convertToString().getByteList();
        int len = Math.min(bl.length(), Util.int32Value(len_));
        getMemoryIO().put(getOffset(off), bl.unsafeBytes(), bl.begin(), len);
        return context.getRuntime().newFixnum(len);
    }
    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        // Just let the GC collect and free the pointer
        pointerSet.remove(this);
        return context.getRuntime().getNil();
    }
    @JRubyMethod(name = "autorelease=", required = 1)
    public IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        if (release.isTrue()) {
            pointerSet.remove(this);
        } else {
            pointerSet.put(this, Boolean.TRUE);
        }
        return context.getRuntime().getNil();
    }
}
