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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
@JRubyClass(name=StructLayoutBuilder.CLASS_NAME, parent="Object")
public final class StructLayoutBuilder extends RubyObject {
    public static final String CLASS_NAME = "StructLayoutBuilder";
    private static final boolean isSparc() { 
        return false; // FIXME: fix sparc support
    }
    /*
     * Most arches align long/double on the same size as a native long (or a pointer)
     * Sparc (32bit) requires it to be aligned on an 8 byte boundary
     */
    static final int LONG_SIZE = Platform.getPlatform().longSize();
    static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();
    static final int REGISTER_SIZE = Platform.getPlatform().addressSize();
    static final long LONG_MASK = LONG_SIZE == 32 ? 0x7FFFFFFFL : 0x7FFFFFFFFFFFFFFFL;
    static final int LONG_ALIGN = isSparc() ? 64 : LONG_SIZE;
    static final int ADDRESS_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int DOUBLE_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int FLOAT_ALIGN = isSparc() ? 64 : Float.SIZE;
    private final Map<IRubyObject, StructLayout.Member> fields = new LinkedHashMap<IRubyObject, StructLayout.Member>();
    private int size = 0, maxAlign = 1;
    
    private static final class Allocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StructLayoutBuilder(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new Allocator();
    }
    public static RubyClass createStructLayoutBuilderClass(Ruby runtime, RubyModule module) {
        RubyClass result = runtime.defineClassUnder(CLASS_NAME, runtime.getObject(),
                Allocator.INSTANCE, module);
        result.defineAnnotatedMethods(StructLayoutBuilder.class);
        result.defineAnnotatedConstants(StructLayoutBuilder.class);

        return result;
    }
    StructLayoutBuilder(Ruby runtime) {
        this(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME));
    }
    StructLayoutBuilder(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
    @JRubyMethod(name = "new", meta = true)
    public static StructLayoutBuilder newInstance(ThreadContext context, IRubyObject recv) {
        return new StructLayoutBuilder(context.getRuntime());
    }
    @JRubyMethod(name = "build")
    public StructLayout build(ThreadContext context) {
        return new StructLayout(context.getRuntime(), fields, size, maxAlign);
    }
    private static int alignMember(int offset, int alignBits) {
        int alignBytes = alignBits >> 3;
        int mask = alignBytes - 1;
        int off = offset;
        if ((off & mask) != 0) {
            off = (off & ~mask) + alignBytes;
        }
        return off;
    }
    @JRubyMethod(name = "add_field", required = 2, optional = 1)
    public IRubyObject add(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        Object type;
        int offset = args.length > 2 && !args[2].isNil() ? Util.int32Value(args[2]) : -1;
        int align = 8, sizeBits = 8;
        if (args[1] instanceof CallbackInfo) {
            type = (CallbackInfo) args[1];
            align = ADDRESS_ALIGN; sizeBits = ADDRESS_SIZE;
        } else if (args[1] instanceof RubyClass && Struct.isStruct(runtime, (RubyClass) args[1])) {
            type = args[1];
            align = Struct.getStructLayout(runtime, args[1]).getMinimumAlignment();
            sizeBits = Struct.getStructSize(args[1]);
        } else {
            NativeType t = NativeType.valueOf(Util.int32Value(args[1]));
            type = t;
            switch (t) {
                case INT8:
                case UINT8:
                    align = 8; sizeBits = 8;
                    break;
                case INT16:
                case UINT16:
                    align = 16; sizeBits = 16;
                    break;
                case INT32:
                case UINT32:
                    align = 32; sizeBits = 32;
                    break;
                case INT64:
                case UINT64:
                    align = LONG_ALIGN;
                    sizeBits = 64;
                    break;
                case LONG:
                case ULONG:
                    align = LONG_ALIGN;
                    sizeBits = LONG_SIZE;
                    break;
                case FLOAT32:
                    align = FLOAT_ALIGN;
                    sizeBits = 32;
                    break;
                case FLOAT64:
                    align = DOUBLE_ALIGN;
                    sizeBits = 64;
                    break;
                case POINTER:
                    align = Platform.getPlatform().addressSize();
                    sizeBits = LONG_ALIGN;
                    break;
                case STRING:
                case RBXSTRING:
                    align = ADDRESS_ALIGN;
                    sizeBits = ADDRESS_SIZE;
                    break;
            }
        }
        if (offset < 0) {
            offset = alignMember(this.size, align);
        }
        StructLayout.Member field = createMember(context.getRuntime(), type, offset);
        if (field == null) {
            throw runtime.newArgumentError("Unknown field type: " + type);
        }
        
        fields.put(createKey(runtime, name), field);
        this.size = offset + (sizeBits / 8);
        this.maxAlign = Math.max(this.maxAlign, align);
        return this;
    }

    @JRubyMethod(name = "add_struct", required = 2, optional = 1)
    public IRubyObject add_struct(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        int offset = args.length > 2 && !args[2].isNil() ? Util.int32Value(args[2]) : -1;
        final int align = Struct.getStructLayout(runtime, args[1]).getMinimumAlignment();
        final int sizeBytes = Struct.getStructSize(args[1]);
        if (offset < 0) {
            offset = alignMember(this.size, align);
        }
        StructLayout.Member field = StructMember.create((RubyClass) args[1], offset);
        
        fields.put(createKey(runtime, name), field);
        this.size = offset + sizeBytes;
        this.maxAlign = Math.max(this.maxAlign, align);
        return this;
    }
    @JRubyMethod(name = "add_char_array", required = 2, optional = 1)
    public IRubyObject add_char_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        int strlen = Util.int32Value(args[1]);
        long offset = args.length > 2 ? Util.int64Value(args[2]) : -1;
        if (offset < 0) {
            offset = alignMember(this.size, 8);
        }
        StructLayout.Member field = CharArrayMember.create(offset, strlen);
        fields.put(createKey(runtime, name), field);
        this.size += strlen;
        return runtime.getNil();
    }
    private static IRubyObject createKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubySymbol) {
            return key;
        }
        return runtime.getSymbolTable().getSymbol(key.asJavaString());
    }
    StructLayout.Member createMember(Ruby runtime, Object type, long offset) {
        if (type instanceof NativeType) {
            return createMember((NativeType) type, offset);
        } else if (type instanceof CallbackInfo) {
            return CallbackMember.create((CallbackInfo) type, offset);
        } else if (type instanceof RubyClass && Struct.isStruct(runtime, (RubyClass) type)) {
            return StructMember.create((RubyClass) type, offset);
        } else {
            return null;
        }
    }
    StructLayout.Member createMember(NativeType type, long offset) {
        switch (type) {
            case INT8:
                return Signed8Member.create(offset);
            case UINT8:
                return Unsigned8Member.create(offset);                
            case INT16:
                return Signed16Member.create(offset);
            case UINT16:
                return Unsigned16Member.create(offset);
            case INT32:
                return Signed32Member.create(offset);
            case UINT32:
                return Unsigned32Member.create(offset);
            case INT64:
                return Signed64Member.create(offset);
            case UINT64:
                return Unsigned64Member.create(offset);
            case LONG:
                return LONG_SIZE == 32 
                        ? Signed32Member.create(offset)
                        : Signed64Member.create(offset);
            case ULONG:
                return LONG_SIZE == 32 
                        ? Unsigned32Member.create(offset)
                        : Unsigned64Member.create(offset);
            case FLOAT32:
                return Float32Member.create(offset);
            case FLOAT64:
                return Float64Member.create(offset);
            case POINTER:
                return PointerMember.create(offset);
            case STRING:
            case RBXSTRING:
                return StringMember.create(offset);
        }
        return null;
    }
    
    static final class Signed8Member extends StructLayout.Member {
        Signed8Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putByte(getOffset(ptr), Util.int8Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned8(runtime, getMemoryIO(ptr).getByte(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Signed8Member(offset); }
    }
    static final class Unsigned8Member extends StructLayout.Member {
        Unsigned8Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putByte(getOffset(ptr), (byte) Util.uint8Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned8(runtime, getMemoryIO(ptr).getByte(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Unsigned8Member(offset); }
    }
    static final class Signed16Member extends StructLayout.Member {
        Signed16Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putShort(getOffset(ptr), Util.int16Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned16(runtime, getMemoryIO(ptr).getShort(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Signed16Member(offset); }
    }
    static final class Unsigned16Member extends StructLayout.Member {
        Unsigned16Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putShort(getOffset(ptr), (short) Util.uint16Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned16(runtime, getMemoryIO(ptr).getShort(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Unsigned16Member(offset); }
    }
    static final class Signed32Member extends StructLayout.Member {
        Signed32Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putInt(getOffset(ptr), Util.int32Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned32(runtime, getMemoryIO(ptr).getInt(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Signed32Member(offset); }
    }
    static final class Unsigned32Member extends StructLayout.Member {
        Unsigned32Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putInt(getOffset(ptr), (int) Util.uint32Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned32(runtime, getMemoryIO(ptr).getInt(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Unsigned32Member(offset); }
    }
    static final class Signed64Member extends StructLayout.Member {
        Signed64Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putLong(getOffset(ptr), Util.int64Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned64(runtime, getMemoryIO(ptr).getLong(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Signed64Member(offset); }
    }
    static final class Unsigned64Member extends StructLayout.Member {
        Unsigned64Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putLong(getOffset(ptr), Util.uint64Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned64(runtime, getMemoryIO(ptr).getLong(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Unsigned64Member(offset); }
    }
    static final class PointerMember extends StructLayout.Member {
        PointerMember(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            if (value instanceof Pointer) {
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), ((Pointer) value).getMemoryIO());
            } else if (Platform.getPlatform().addressSize() == 32) {
                getMemoryIO(ptr).putInt(getOffset(ptr), Util.int32Value(value));
            } else if (Platform.getPlatform().addressSize() == 64) {
                getMemoryIO(ptr).putLong(getOffset(ptr), Util.int64Value(value));
            }
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return ((AbstractMemory) ptr).getPointer(runtime, getOffset(ptr));
        }
        static StructLayout.Member create(long offset) { return new PointerMember(offset); }
    }
    static final class Float32Member extends StructLayout.Member {
        Float32Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putFloat(getOffset(ptr), Util.floatValue(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return RubyFloat.newFloat(runtime, getMemoryIO(ptr).getFloat(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Float32Member(offset); }
    }
    static final class Float64Member extends StructLayout.Member {
        Float64Member(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putDouble(getOffset(ptr), Util.doubleValue(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return RubyFloat.newFloat(runtime, getMemoryIO(ptr).getDouble(getOffset(ptr)));
        }
        static StructLayout.Member create(long offset) { return new Float64Member(offset); }
    }
    static final class StringMember extends StructLayout.Member {
        StringMember(long offset) {
            super(offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            MemoryIO io = getMemoryIO(ptr).getMemoryIO(getOffset(ptr));
            if (!io.isNull()) {
                ByteList bl = value.convertToString().getByteList();
                io.put(0, bl.unsafeBytes(), bl.begin(), bl.length());
                io.putByte(bl.length(), (byte) 0);
            }
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            MemoryIO io = getMemoryIO(ptr).getMemoryIO(getOffset(ptr));
            if (io.isNull()) {
                return runtime.getNil();
            }
            int len = (int) io.indexOf(0, (byte) 0, Integer.MAX_VALUE);
            ByteList bl = new ByteList(len);
            bl.length(len);
            io.get(0, bl.unsafeBytes(), bl.begin(), len);
        
            return runtime.newString(bl);
        }
        static StructLayout.Member create(long offset) { return new StringMember(offset); }
    }
    static final class CharArrayMember extends StructLayout.Member {
        private final int size;
        CharArrayMember(long offset, int size) {
            super(offset);
            this.size = size;
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            MemoryIO io = getMemoryIO(ptr);
            ByteList bl = value.convertToString().getByteList();
            // Clamp to no longer than 
            int len = Math.min(bl.length(), size - 1);
            io.put(getOffset(ptr), bl.unsafeBytes(), bl.begin(), len);
            io.putByte(getOffset(ptr) + len, (byte) 0);
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            MemoryIO io = getMemoryIO(ptr);
            int len = (int) io.indexOf(getOffset(ptr), (byte) 0, size);
            if (len < 0) {
                len = size;
            }
            ByteList bl = new ByteList(len);
            bl.length(len);
            io.get(0, bl.unsafeBytes(), bl.begin(), len);
        
            return runtime.newString(bl);
        }
        static StructLayout.Member create(long offset, int size) { 
            return new CharArrayMember(offset, size); 
        }
    }
    static final class CallbackMember extends StructLayout.Member {
        private final CallbackInfo cbInfo;
        CallbackMember(CallbackInfo cbInfo, long offset) {
            super(offset);
            this.cbInfo = cbInfo;
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            if (value.isNil()) {
                getMemoryIO(ptr).putAddress(getOffset(ptr), 0L);
            } else {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(runtime, cbInfo, value);
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), cb.getMemoryIO());
            }
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            throw runtime.newNotImplementedError("Cannot get callback struct fields");
        }
        static StructLayout.Member create(CallbackInfo cbInfo, long offset) { return new CallbackMember(cbInfo, offset); }
    }
    static final class StructMember extends StructLayout.Member {
        private final RubyClass klass;
        StructMember(RubyClass klass, long offset) {
            super(offset);
            this.klass = klass;
            System.out.println("Creating struct field of type " + klass + " offset=" + offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Struct fields");
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Struct.newStruct(runtime, klass, ((AbstractMemory) ptr).slice(runtime, getOffset(ptr)));
        }

        @Override
        public IRubyObject get(Map<Object, IRubyObject> cache, Ruby runtime, IRubyObject ptr) {
            IRubyObject s = cache.get(this);
            if (s == null) {
                cache.put(this, s = Struct.newStruct(runtime, klass, ((AbstractMemory) ptr).slice(runtime, getOffset(ptr))));
            }
            return s;
        }

        static StructLayout.Member create(RubyClass klass, long offset) { return new StructMember(klass, offset); }
    }
}
