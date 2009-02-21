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
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
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
        final Platform.CPU cpu = Platform.getPlatform().getCPU();
        return cpu == Platform.CPU.SPARC || cpu == Platform.CPU.SPARCV9;
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
    /** The current size of the layout in bytes */
    private int size = 0;
    /** The current minimum alignment of the layout in bytes */
    private int minAlign = 1;
    
    /** The number of fields in the struct */
    private int fieldCount = 0;
    
    /** Whether the StructLayout is for a structure or union */
    private boolean isUnion = false;

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
        return new StructLayout(context.getRuntime(), fields, minAlign + ((size - 1) & ~(minAlign - 1)), minAlign);
    }
    @JRubyMethod(name = "size")
    public IRubyObject get_size(ThreadContext context) {
        return context.getRuntime().newFixnum(size);
    }
    @JRubyMethod(name = "size=")
    public IRubyObject set_size(ThreadContext context, IRubyObject sizeArg) {
        int newSize = RubyNumeric.num2int(sizeArg);
        if (newSize > size) {
            size = newSize;
        }
        return context.getRuntime().newFixnum(size);
    }
    
    private static int alignMemberBits(int offset, int alignBits) {
        final int align = alignBits >> 3;
        return align + ((offset - 1) & ~(align - 1));
    }
    private static int getAlignmentBits(NativeType type) {
        switch (type) {
            case INT8:
            case UINT8:
                return 8;
            case INT16:
            case UINT16:
                return 16;
            case INT32:
            case UINT32:
                return 32;
            case INT64:
            case UINT64:
                return LONG_ALIGN;
            case LONG:
            case ULONG:
                return LONG_ALIGN;
            case FLOAT32:
                return FLOAT_ALIGN;
            case FLOAT64:
                return DOUBLE_ALIGN;
            case POINTER:
            case STRING:
            case RBXSTRING:
                return ADDRESS_ALIGN;
            default:
                throw new UnsupportedOperationException("Cannot determine alignment of " + type);
        }
    }
    private static int getSizeBits(NativeType type) {
        switch (type) {
            case INT8:
            case UINT8:
                return 8;
            case INT16:
            case UINT16:
                return 16;
            case INT32:
            case UINT32:
                return 32;
            case INT64:
            case UINT64:
                return 64;
            case LONG:
            case ULONG:
                return LONG_SIZE;
            case FLOAT32:
                return Float.SIZE;
            case FLOAT64:
                return Double.SIZE;
            case POINTER:
            case STRING:
            case RBXSTRING:
                return ADDRESS_SIZE;
            default:
                throw new UnsupportedOperationException("Cannot determine size of " + type);
        }
    }
    private static IRubyObject createSymbolKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubySymbol) {
            return key;
        }
        return runtime.getSymbolTable().getSymbol(key.asJavaString());
    }
    private static IRubyObject createStringKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubyString) {
            return key;
        }
        return RubyString.newString(runtime, key.asJavaString());
    }
    private final IRubyObject storeField(Ruby runtime, IRubyObject name, StructLayout.Member field, int align, int size) {
        fields.put(createStringKey(runtime, name), field);
        fields.put(createSymbolKey(runtime, name), field);
        this.size = Math.max(this.size, (int) field.offset + size);
        this.minAlign = Math.max(this.minAlign, align);
        return this;
    }

    @JRubyMethod(name = "union=")
    public IRubyObject set_union(ThreadContext context, IRubyObject isUnion) {
        this.isUnion = isUnion.isTrue();
        return this;
    }

    @JRubyMethod(name = "add_field", required = 2, optional = 1)
    public IRubyObject add(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        Object type;
        int offset = args.length > 2 && !args[2].isNil() ? Util.int32Value(args[2]) : -1;
        int alignBits = 8, sizeBits = 8;
        if (args[1] instanceof CallbackInfo) {
            type = (CallbackInfo) args[1];
            alignBits = ADDRESS_ALIGN; sizeBits = ADDRESS_SIZE;
        } else {
            NativeType t = NativeType.valueOf(Util.int32Value(args[1]));
            type = t;
            alignBits = getAlignmentBits(t);
            sizeBits = getSizeBits(t);
        }
        if (offset < 0) {
            offset = isUnion ? 0 : alignMemberBits(this.size, alignBits);
        }
        StructLayout.Member field = createMember(context.getRuntime(), type, fieldCount++, offset);
        if (field == null) {
            throw runtime.newArgumentError("Unknown field type: " + type);
        }
        return storeField(runtime, name, field, alignBits / 8, sizeBits / 8);
    }

    @JRubyMethod(name = "add_struct", required = 2, optional = 1)
    public IRubyObject add_struct(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        int offset = args.length > 2 && !args[2].isNil() ? Util.int32Value(args[2]) : -1;
        final StructLayout layout = Struct.getStructLayout(runtime, args[1]);
        final int alignBits = layout.getMinimumAlignment() * 8;
        if (offset < 0) {
            offset = isUnion ? 0 : alignMemberBits(this.size, alignBits);
        }
        StructLayout.Member field = StructMember.create((RubyClass) args[1], fieldCount++, offset);
        return storeField(runtime, name, field, alignBits / 8, layout.getSize());
    }
    @JRubyMethod(name = "add_array", required = 3, optional = 1)
    public IRubyObject add_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        NativeType type = NativeType.valueOf(Util.int32Value(args[1]));
        int length = Util.int32Value(args[2]);
        int offset = args.length > 3 && !args[3].isNil() ? Util.int32Value(args[3]) : -1;
        final int alignBits = getAlignmentBits(type);
        final int sizeBits = getSizeBits(type);
        
        if (offset < 0) {
            offset = isUnion ? 0 : alignMemberBits(this.size, alignBits);
        }
        StructLayout.ArrayMemberIO io = getArrayMemberIO(type);
        if (io == null) {
            throw context.getRuntime().newNotImplementedError("Unsupported array field type: " + type);
        }
        StructLayout.Member field = new ArrayMember(fieldCount++, offset, io, sizeBits, length);

        return storeField(runtime, name, field, alignBits / 8, ((sizeBits / 8) * length));
    }
    @JRubyMethod(name = "add_char_array", required = 2, optional = 1)
    public IRubyObject add_char_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        IRubyObject name = args[0];
        int strlen = Util.int32Value(args[1]);
        long offset = args.length > 2 ? Util.int64Value(args[2]) : -1;
        int alignBits = 8;
        if (offset < 0) {
            offset = isUnion ? 0 : alignMemberBits(this.size, alignBits);
        }
        return storeField(runtime, name, CharArrayMember.create(fieldCount++, offset, strlen), alignBits / 8, strlen);
    }
    
    StructLayout.Member createMember(Ruby runtime, Object type, final int index, long offset) {
        if (type instanceof NativeType) {
            return createMember((NativeType) type, index, offset);
        } else if (type instanceof CallbackInfo) {
            return CallbackMember.create((CallbackInfo) type, index, offset);
        } else if (type instanceof RubyClass && Struct.isStruct(runtime, (RubyClass) type)) {
            return StructMember.create((RubyClass) type, index, offset);
        } else {
            return null;
        }
    }
    static StructLayout.Member createMember(NativeType type, final int index, final long offset) {
        switch (type) {
            case INT8:
                return Signed8Member.create(index, offset);
            case UINT8:
                return Unsigned8Member.create(index, offset);
            case INT16:
                return Signed16Member.create(index, offset);
            case UINT16:
                return Unsigned16Member.create(index, offset);
            case INT32:
                return Signed32Member.create(index, offset);
            case UINT32:
                return Unsigned32Member.create(index, offset);
            case INT64:
                return Signed64Member.create(index, offset);
            case UINT64:
                return Unsigned64Member.create(index, offset);
            case LONG:
                return LONG_SIZE == 32 
                        ? Signed32Member.create(index, offset)
                        : Signed64Member.create(index, offset);
            case ULONG:
                return LONG_SIZE == 32 
                        ? Unsigned32Member.create(index, offset)
                        : Unsigned64Member.create(index, offset);
            case FLOAT32:
                return Float32Member.create(index, offset);
            case FLOAT64:
                return Float64Member.create(index, offset);
            case POINTER:
                return PointerMember.create(index, offset);
            case STRING:
            case RBXSTRING:
                return StringMember.create(index, offset);
        }
        return null;
    }
    
    static final class Signed8Member extends StructLayout.Member {
        Signed8Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putByte(getOffset(ptr), Util.int8Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned8(runtime, getMemoryIO(ptr).getByte(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Signed8Member(index, offset); }
    }
    static final class Unsigned8Member extends StructLayout.Member {
        Unsigned8Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putByte(getOffset(ptr), (byte) Util.uint8Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned8(runtime, getMemoryIO(ptr).getByte(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Unsigned8Member(index, offset); }
    }
    static final class Signed16Member extends StructLayout.Member {
        Signed16Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putShort(getOffset(ptr), Util.int16Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned16(runtime, getMemoryIO(ptr).getShort(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Signed16Member(index, offset); }
    }
    static final class Unsigned16Member extends StructLayout.Member {
        Unsigned16Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putShort(getOffset(ptr), (short) Util.uint16Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned16(runtime, getMemoryIO(ptr).getShort(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Unsigned16Member(index, offset); }
    }
    static final class Signed32Member extends StructLayout.Member {
        Signed32Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putInt(getOffset(ptr), Util.int32Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned32(runtime, getMemoryIO(ptr).getInt(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Signed32Member(index, offset); }
    }
    static final class Unsigned32Member extends StructLayout.Member {
        Unsigned32Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putInt(getOffset(ptr), (int) Util.uint32Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned32(runtime, getMemoryIO(ptr).getInt(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Unsigned32Member(index, offset); }
    }
    static final class Signed64Member extends StructLayout.Member {
        Signed64Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putLong(getOffset(ptr), Util.int64Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newSigned64(runtime, getMemoryIO(ptr).getLong(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Signed64Member(index, offset); }
    }
    static final class Unsigned64Member extends StructLayout.Member {
        Unsigned64Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putLong(getOffset(ptr), Util.uint64Value(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Util.newUnsigned64(runtime, getMemoryIO(ptr).getLong(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Unsigned64Member(index, offset); }
    }
    static final class PointerMember extends StructLayout.Member {
        PointerMember(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            if (value instanceof Pointer) {
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), ((Pointer) value).getMemoryIO());
            } else if (value instanceof Struct) {
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), ((Struct) value).getMemoryIO());
            } else if (value instanceof RubyInteger) {
                getMemoryIO(ptr).putAddress(offset, Util.int64Value(ptr));
            } else if (value.respondsTo("to_ptr")) {
                IRubyObject addr = value.callMethod(runtime.getCurrentContext(), "to_ptr");
                if (addr instanceof Pointer) {
                    getMemoryIO(ptr).putMemoryIO(offset, ((Pointer) addr).getMemoryIO());
                } else {
                    throw runtime.newArgumentError("Invalid pointer value");
                }
            } else if (value.isNil()) {
                getMemoryIO(ptr).putAddress(offset, 0L);
            } else {
                throw runtime.newArgumentError("Invalid pointer value");
            }
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return ((AbstractMemory) ptr).getPointer(runtime, getOffset(ptr));
        }

        @Override
        public IRubyObject get(Ruby runtime, Struct struct) {
            IRubyObject ptr = struct.getMemory();
            MemoryIO memory = ((AbstractMemory) ptr).getMemoryIO().getMemoryIO(getOffset(ptr));
            IRubyObject old = struct.getCachedValue(this);
            if (old != null) {
                MemoryIO oldMemory = ((AbstractMemory) old).getMemoryIO();
                if ((memory != null && memory.equals(oldMemory)) || (memory == null && oldMemory.isNull())) {
                    return old;
                }
            }
            Pointer retval = new BasePointer(runtime,
                    memory != null ? (DirectMemoryIO) memory : new NullMemoryIO(runtime));
            struct.putCachedValue(this, retval);
            return retval;
        }
        @Override
        protected boolean isCacheable() {
            return true;
        }
        static StructLayout.Member create(int index, long offset) { return new PointerMember(index, offset); }
    }
    static final class Float32Member extends StructLayout.Member {
        Float32Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putFloat(getOffset(ptr), Util.floatValue(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return RubyFloat.newFloat(runtime, getMemoryIO(ptr).getFloat(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Float32Member(index, offset); }
    }
    static final class Float64Member extends StructLayout.Member {
        Float64Member(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            getMemoryIO(ptr).putDouble(getOffset(ptr), Util.doubleValue(value));
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return RubyFloat.newFloat(runtime, getMemoryIO(ptr).getDouble(getOffset(ptr)));
        }
        static StructLayout.Member create(int index, long offset) { return new Float64Member(index, offset); }
    }
    static final class StringMember extends StructLayout.Member {
        StringMember(int index, long offset) {
            super(index, offset);
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            MemoryIO io = getMemoryIO(ptr).getMemoryIO(getOffset(ptr));
            if (io == null || io.isNull()) {
                throw runtime.newRuntimeError("Invalid memory access");
            }
            ByteList bl = value.convertToString().getByteList();
            io.put(0, bl.unsafeBytes(), bl.begin(), bl.length());
            io.putByte(bl.length(), (byte) 0);
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            MemoryIO io = getMemoryIO(ptr).getMemoryIO(getOffset(ptr));
            if (io == null || io.isNull()) {
                return runtime.getNil();
            }
            int len = (int) io.indexOf(0, (byte) 0, Integer.MAX_VALUE);
            ByteList bl = new ByteList(len);
            bl.length(len);
            io.get(0, bl.unsafeBytes(), bl.begin(), len);
        
            return runtime.newString(bl);
        }
        static StructLayout.Member create(int index, long offset) { return new StringMember(index, offset); }
    }
    static final class CharArrayMember extends StructLayout.Member {
        private final int size;
        CharArrayMember(int index, long offset, int size) {
            super(index, offset);
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
        static StructLayout.Member create(int index, long offset, int size) {
            return new CharArrayMember(index, offset, size);
        }
    }
    static final class CallbackMember extends StructLayout.Member {
        private final CallbackInfo cbInfo;
        CallbackMember(CallbackInfo cbInfo, int index, long offset) {
            super(index, offset);
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
        static StructLayout.Member create(CallbackInfo cbInfo, int index, long offset) { return new CallbackMember(cbInfo, index, offset); }
    }
    static final class StructMember extends StructLayout.Member {
        private final RubyClass klass;
        StructMember(RubyClass klass, int index, long offset) {
            super(index, offset);
            this.klass = klass;
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Struct fields");
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return Struct.newStruct(runtime, klass, ((AbstractMemory) ptr).slice(runtime, getOffset(ptr)));
        }

        @Override
        public IRubyObject get(Ruby runtime, Struct struct) {
            IRubyObject s = struct.getCachedValue(this);
            if (s == null) {
                IRubyObject ptr = struct.getMemory();
                s = Struct.newStruct(runtime, klass, ((AbstractMemory) ptr).slice(runtime, getOffset(ptr)));
                struct.putCachedValue(this, s);
            }
            return s;
        }
        @Override
        protected boolean isCacheable() {
            return true;
        }
        static StructLayout.Member create(RubyClass klass, int index, long offset) { return new StructMember(klass, index, offset); }
    }
    static final class ArrayMember extends StructLayout.Member {
        private final StructLayout.ArrayMemberIO io;
        private final int length, typeSize;
        ArrayMember(int index, long offset, StructLayout.ArrayMemberIO io, int typeSize, int length) {
            super(index, offset);
            this.io = io;
            this.typeSize = typeSize;
            this.length = length;
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Array fields");
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return new StructLayout.Array(runtime, ptr, offset, length, typeSize, io);
        }

        @Override
        public IRubyObject get(Ruby runtime, Struct struct) {
            IRubyObject s = struct.getCachedValue(this);
            if (s == null) {
                s = new StructLayout.Array(runtime, struct.getMemory(), offset, length, typeSize, io);
                struct.putCachedValue(this, s);
            }
            return s;
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }

    }
    private static final StructLayout.ArrayMemberIO getArrayMemberIO(NativeType type) {
        switch (type) {
            case INT8:
                return Signed8ArrayIO.INSTANCE;
            case UINT8:
                return Unsigned8ArrayIO.INSTANCE;
            case INT16:
                return Signed16ArrayIO.INSTANCE;
            case UINT16:
                return Unsigned16ArrayIO.INSTANCE;
            case INT32:
                return Signed32ArrayIO.INSTANCE;
            case UINT32:
                return Unsigned32ArrayIO.INSTANCE;
            case INT64:
                return Signed64ArrayIO.INSTANCE;
            case UINT64:
                return Unsigned64ArrayIO.INSTANCE;
            default:
                return null;
        }

    }
    
    static final class Signed8ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, Util.int8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned8(runtime, io.getByte(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Signed8ArrayIO();
    }
    static final class Unsigned8ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putByte(offset, (byte) Util.uint8Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned8(runtime, io.getByte(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Unsigned8ArrayIO();
    }
    static final class Signed16ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, Util.int16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned16(runtime, io.getShort(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Signed16ArrayIO();
    }
    static final class Unsigned16ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putShort(offset, (short) Util.uint16Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned16(runtime, io.getShort(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Unsigned16ArrayIO();
    }
    static final class Signed32ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, Util.int32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned32(runtime, io.getInt(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Signed32ArrayIO();
    }
    static final class Unsigned32ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putInt(offset, (int) Util.uint32Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned32(runtime, io.getInt(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Unsigned32ArrayIO();
    }
    static final class Signed64ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.int64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newSigned64(runtime, io.getLong(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Signed64ArrayIO();
    }
    static final class Unsigned64ArrayIO extends StructLayout.ArrayMemberIO {
        public final void put(Ruby runtime, MemoryIO io, long offset, IRubyObject value) {
            io.putLong(offset, Util.uint64Value(value));
        }

        public final IRubyObject get(Ruby runtime, MemoryIO io, long offset) {
            return Util.newUnsigned64(runtime, io.getLong(offset));
        }
        static StructLayout.ArrayMemberIO INSTANCE = new Unsigned64ArrayIO();
    }
}
