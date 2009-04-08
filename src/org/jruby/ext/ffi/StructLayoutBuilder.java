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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
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
        this(runtime, runtime.fastGetModule("FFI").fastGetClass(CLASS_NAME));
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
    
    private static final int alignMember(int offset, int align) {
        return align + ((offset - 1) & ~(align - 1));
    }
    
    private static final IRubyObject createSymbolKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubySymbol) {
            return key;
        }
        return runtime.getSymbolTable().getSymbol(key.asJavaString());
    }

    private static IRubyObject createStringKey(Ruby runtime, IRubyObject key) {
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
    
    private final int calculateOffset(IRubyObject[] args, int index, int alignment) {
        return args.length > index && args[index] instanceof RubyInteger
                ? Util.int32Value(args[index])
                : isUnion ? 0 : alignMember(this.size, alignment);
    }

    @JRubyMethod(name = "add_field", required = 2, optional = 1)
    public IRubyObject add(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        
        if (!(args[0] instanceof RubyString || args[0] instanceof RubySymbol)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[0].getMetaClass().getName() + " (expected String or Symbol)");
        }

        if (!(args[1] instanceof Type)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected FFI::Type)");
        }

        Type type = (Type) args[1];
        int offset = calculateOffset(args, 2, type.getNativeAlignment());
        
        StructLayout.Member field = null;
        if (type instanceof Type.Builtin) {
            field = createBuiltinMember((Type.Builtin) type, fieldCount, offset);
        } else if (type instanceof CallbackInfo) {
            field = new CallbackMember((CallbackInfo) type, fieldCount, offset);
        }
        if (field == null) {
            throw runtime.newArgumentError("Unknown field type: " + type);
        }
        ++fieldCount;

        return storeField(runtime, args[0], field, type.getNativeAlignment(), type.getNativeSize());
    }

    @JRubyMethod(name = "add_struct", required = 2, optional = 1)
    public IRubyObject add_struct(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();

        if (!(args[0] instanceof RubyString || args[0] instanceof RubySymbol)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[0].getMetaClass().getName() + " (expected String or Symbol)");
        }

        if (!(args[1] instanceof RubyClass) || !(((RubyClass) args[1]).isKindOfModule(runtime.fastGetModule("FFI").fastGetClass("Struct")))) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected FFI::Struct subclass)");
        }
        final StructLayout layout = Struct.getStructLayout(runtime, args[1]);
        int offset = calculateOffset(args, 2, layout.getNativeAlignment());

        StructLayout.Member field = new StructMember(layout, (RubyClass) args[1], fieldCount++, offset);
        return storeField(runtime, args[0], field, layout.getNativeAlignment(), layout.getNativeSize());
    }

    @JRubyMethod(name = "add_array", required = 3, optional = 1)
    public IRubyObject add_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        
        if (!(args[0] instanceof RubyString || args[0] instanceof RubySymbol)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[0].getMetaClass().getName() + " (expected String or Symbol)");
        }

        if (!(args[1] instanceof Type)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected FFI::Type)");
        }

        if (!(args[2] instanceof RubyInteger)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[2].getMetaClass().getName() + " (expected Integer)");
        }

        Type type = (Type) args[1];

        int offset = calculateOffset(args, 3, type.getNativeAlignment());
        
        int length = Util.int32Value(args[2]);
        MemoryOp io = MemoryOp.getMemoryOp(type.getNativeType());
        if (io == null) {
            throw context.getRuntime().newNotImplementedError("Unsupported array field type: " + type);
        }
        StructLayout.Member field = new ArrayMember(fieldCount++, offset, io, type.getNativeSize() * 8, length);

        return storeField(runtime, args[0], field, type.getNativeAlignment(), (type.getNativeSize() * length));
    }

    @JRubyMethod(name = "add_char_array", required = 2, optional = 1)
    public IRubyObject add_char_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();

        if (!(args[0] instanceof RubyString || args[0] instanceof RubySymbol)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[0].getMetaClass().getName() + " (expected String or Symbol)");
        }

        if (!(args[1] instanceof RubyInteger)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected Integer)");
        }

        int strlen = Util.int32Value(args[1]);
        int offset = calculateOffset(args, 2, 1);

        return storeField(runtime, args[0], new CharArrayMember(fieldCount++, offset, strlen), 1, strlen);
    }

    /**
     * Creates a struct layout field for a given builtin type.
     *
     * @param type The type to create a field accessor for.
     * @param index The index of the field within the struct.
     * @param offset The offset in bytes of the field within the struct memory.
     * @return A new struct layout member for the type.
     */
    static StructLayout.Member createBuiltinMember(Type.Builtin type, final int index, final long offset) {

        switch (type.getNativeType()) {
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
            case INT64:
            case UINT64:
            case LONG:
            case ULONG:
            case FLOAT32:
            case FLOAT64:
                return new PrimitiveMember(type, index, offset);

            case POINTER:
                return new PointerMember(type, index, offset);

            case STRING:
            case RBXSTRING:
                return new StringMember(type, index, offset);
        }
        return null;
    }

    /**
     * Primitive (byte, short, int, long, float, double) types are all handled by
     * a PrimitiveMember type.
     */
    static final class PrimitiveMember extends StructLayout.Member {
        private final MemoryOp op;

        PrimitiveMember(Type type, int index, long offset) {
            super(type.getNativeType(), index, offset);
            op = MemoryOp.getMemoryOp(type.getNativeType());
        }
        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            op.put(runtime, getMemoryIO(ptr), offset, value);
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return op.get(runtime, getMemoryIO(ptr), offset);
        }
    }
    
    static final class PointerMember extends StructLayout.Member {

        PointerMember(Type type, int index, long offset) {
            super(type, index, offset);
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
    }
    
    static final class StringMember extends StructLayout.Member {
        StringMember(Type type, int index, long offset) {
            super(type, index, offset);
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
    }

    static final class CharArrayMember extends StructLayout.Member {
        private final int size;
        CharArrayMember(int index, long offset, int size) {
            super(NativeType.CHAR_ARRAY, index, offset);
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
    }

    static final class CallbackMember extends StructLayout.Member {
        private final CallbackInfo cbInfo;
        CallbackMember(CallbackInfo cbInfo, int index, long offset) {
            super(cbInfo, index, offset);
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
    }

    static final class StructMember extends StructLayout.Aggregate {
        private final RubyClass klass;
        private final StructLayout layout;

        StructMember(StructLayout layout, RubyClass klass, int index, long offset) {
            super(layout, index, offset);
            this.klass = klass;
            this.layout = layout;
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

        @Override
        public Collection<StructLayout.Member> getFields() {
            return layout.getFields();
        }
    }

    static final class ArrayMember extends StructLayout.Member {
        private final MemoryOp op;
        private final int length, typeSize;

        ArrayMember(int index, long offset, MemoryOp op, int typeSize, int length) {
            super(NativeType.ARRAY, index, offset);
            this.op = op;
            this.typeSize = typeSize;
            this.length = length;
        }

        public void put(Ruby runtime, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Array fields");
        }

        public IRubyObject get(Ruby runtime, IRubyObject ptr) {
            return new StructLayout.Array(runtime, ptr, offset, length, typeSize, op);
        }

        @Override
        public IRubyObject get(Ruby runtime, Struct struct) {
            IRubyObject s = struct.getCachedValue(this);
            if (s == null) {
                s = new StructLayout.Array(runtime, struct.getMemory(), offset, length, typeSize, op);
                struct.putCachedValue(this, s);
            }
            return s;
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }
    }
}
