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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.jruby.ext.ffi.StructLayout.Storage;
import org.jruby.runtime.Block;
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

    private final List<RubySymbol> fieldNames = new LinkedList<RubySymbol>();

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
        return new StructLayout(context.getRuntime(), fieldNames, fields, minAlign + ((size - 1) & ~(minAlign - 1)), minAlign);
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
    
    private static final RubySymbol createSymbolKey(Ruby runtime, IRubyObject key) {
        if (key instanceof RubySymbol) {
            return (RubySymbol) key;
        }
        return runtime.getSymbolTable().getSymbol(key.asJavaString());
    }

    private static IRubyObject createStringKey(Ruby runtime, IRubyObject key) {
        return RubyString.newString(runtime, key.asJavaString());
    }

    private final IRubyObject storeField(Ruby runtime, IRubyObject name, StructLayout.Member field, int align, int size) {
        
        fields.put(createStringKey(runtime, name), field);
        fields.put(createSymbolKey(runtime, name), field);
        fieldNames.add(createSymbolKey(runtime, name));
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

    private final static boolean checkFieldName(Ruby runtime, IRubyObject fieldName) {

        if (!(fieldName instanceof RubyString || fieldName instanceof RubySymbol)) {
            throw runtime.newTypeError("wrong argument type "
                    + fieldName.getMetaClass().getName() + " (expected String or Symbol)");
        }

        return true;
    }

    @JRubyMethod(name = "add_field", required = 2, optional = 1)
    public IRubyObject add(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject fieldName = args[0];

        checkFieldName(runtime, fieldName);

        if (!(args[1] instanceof Type)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected FFI::Type)");
        }

        Type type = (Type) args[1];
        int offset = calculateOffset(args, 2, type.getNativeAlignment());
        
        StructLayout.Member field = null;
        if (type instanceof Type.Array) {

            Type.Array arrayType = (Type.Array) type;
            field = new ArrayMember(fieldName, arrayType, fieldCount, offset,
                    MemoryOp.getMemoryOp(arrayType.getComponentType().getNativeType()));

        } else if (type instanceof StructByValue) {
            StructByValue sbv = (StructByValue) type;

            field = new StructMember(fieldName, sbv.getStructLayout(), sbv.getStructClass(), fieldCount, offset);
        
        } else if (type instanceof CallbackInfo) {

            field = new CallbackMember(fieldName, (CallbackInfo) type, fieldCount, offset);

        } if (type instanceof Type.Builtin) {

            field = createBuiltinMember(fieldName, (Type.Builtin) type, fieldCount, offset);
        }
        if (field == null) {
            throw runtime.newArgumentError("Unknown field type: " + type);
        }
        ++fieldCount;

        return storeField(runtime, fieldName, field, type.getNativeAlignment(), type.getNativeSize());
    }

    @JRubyMethod(name = "add_struct", required = 2, optional = 1)
    public IRubyObject add_struct(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject fieldName = args[0];

        checkFieldName(runtime, fieldName);

        if (!(args[1] instanceof RubyClass) || !(((RubyClass) args[1]).isKindOfModule(runtime.fastGetModule("FFI").fastGetClass("Struct")))) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected FFI::Struct subclass)");
        }
        final StructLayout layout = Struct.getStructLayout(runtime, args[1]);
        int offset = calculateOffset(args, 2, layout.getNativeAlignment());

        StructLayout.Member field = new StructMember(fieldName, layout, (RubyClass) args[1], fieldCount++, offset);
        return storeField(runtime, fieldName, field, layout.getNativeAlignment(), layout.getNativeSize());
    }

    @JRubyMethod(name = "add_array", required = 3, optional = 1)
    public IRubyObject add_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject fieldName = args[0];
        
        checkFieldName(runtime, fieldName);

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
        StructLayout.Member field = new ArrayMember(fieldName, new Type.Array(runtime, type, length), fieldCount++, offset, io);

        return storeField(runtime, fieldName, field, type.getNativeAlignment(), (type.getNativeSize() * length));
    }

    @JRubyMethod(name = "add_char_array", required = 2, optional = 1)
    public IRubyObject add_char_array(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.getRuntime();
        final IRubyObject fieldName = args[0];

        checkFieldName(runtime, fieldName);

        if (!(args[1] instanceof RubyInteger)) {
            throw runtime.newTypeError("wrong argument type "
                    + args[1].getMetaClass().getName() + " (expected Integer)");
        }

        int strlen = Util.int32Value(args[1]);
        int offset = calculateOffset(args, 2, 1);
        Type type = (Type) context.getRuntime().fastGetModule("FFI").fastGetClass("Type").fastFetchConstant("INT8");
        return storeField(runtime, args[0], new CharArrayMember(fieldName, type, fieldCount++, offset, strlen), 1, strlen);
    }

    /**
     * Creates a struct layout field for a given builtin type.
     *
     * @param type The type to create a field accessor for.
     * @param index The index of the field within the struct.
     * @param offset The offset in bytes of the field within the struct memory.
     * @return A new struct layout member for the type.
     */
    static StructLayout.Member createBuiltinMember(IRubyObject name, Type.Builtin type, final int index, final long offset) {

        switch (type.getNativeType()) {
            case BOOL:
            case CHAR:
            case UCHAR:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
            case LONG_LONG:
            case ULONG_LONG:
            case LONG:
            case ULONG:
            case FLOAT:
            case DOUBLE:
                return new PrimitiveMember(name, type, index, offset);

            case POINTER:
                return new PointerMember(name, type, index, offset);

            case STRING:
            case RBXSTRING:
                return new StringMember(name, type, index, offset);
        }
        return null;
    }

    /**
     * Primitive (byte, short, int, long, float, double) types are all handled by
     * a PrimitiveMember type.
     */
    static final class PrimitiveMember extends StructLayout.Member {
        private final MemoryOp op;

        PrimitiveMember(IRubyObject name, Type type, int index, long offset) {
            super(name, type, index, offset);
            op = MemoryOp.getMemoryOp(type.getNativeType());
        }
        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            op.put(runtime, getMemoryIO(ptr), offset, value);
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            return op.get(runtime, getMemoryIO(ptr), offset);
        }
    }
    
    static final class PointerMember extends StructLayout.Member {

        PointerMember(IRubyObject name, Type type, int index, long offset) {
            super(name, type, index, offset);
        }

        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            if (value instanceof Pointer) {
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), ((Pointer) value).getMemoryIO());
            } else if (value instanceof Struct) {
                MemoryIO mem = ((Struct) value).getMemoryIO();

                if (!(mem instanceof DirectMemoryIO)) {
                    throw runtime.newArgumentError("Struct memory not backed by a native pointer");
                }
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), mem);

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
            cache.putReference(this, value);
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            DirectMemoryIO memory = ((AbstractMemory) ptr).getMemoryIO().getMemoryIO(getOffset(ptr));
            IRubyObject old = cache.getCachedValue(this);
            if (old instanceof Pointer) {
                MemoryIO oldMemory = ((Pointer) old).getMemoryIO();
                if (memory.equals(oldMemory)) {
                    return old;
                }
            }
            Pointer retval = new Pointer(runtime, memory);
            cache.putCachedValue(this, retval);
            return retval;
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }

        @Override
        protected boolean isValueReferenceNeeded() {
            return true;
        }
    }
    
    static final class StringMember extends StructLayout.Member {
        StringMember(IRubyObject name, Type type, int index, long offset) {
            super(name, type, index, offset);
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }

        @Override
        protected boolean isValueReferenceNeeded() {
            return true;
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            MemoryIO io = getMemoryIO(ptr).getMemoryIO(getOffset(ptr));
            if (io == null || io.isNull()) {
                return runtime.getNil();
            }
            
            return RubyString.newStringNoCopy(runtime, io.getZeroTerminatedByteArray(0));
        }
        
        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            ByteList bl = value.convertToString().getByteList();

            MemoryPointer mem = MemoryPointer.allocate(runtime, 1, bl.length() + 1, false);
            //
            // Keep a reference to the temporary memory in the cache so it does
            // not get freed by the GC until the struct is freed
            //
            cache.putReference(this, mem);

            MemoryIO io = mem.getMemoryIO();            
            io.put(0, bl.unsafeBytes(), bl.begin(), bl.length());
            io.putByte(bl.length(), (byte) 0);

            getMemoryIO(ptr).putMemoryIO(getOffset(ptr), io);
        }
    }

    static final class CharArrayMember extends StructLayout.Member implements StructLayout.Aggregate {
        private final int length;
        CharArrayMember(IRubyObject name, Type type, int index, long offset, int size) {
            super(name, type, index, offset);
            this.length = size;
        }
        
        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            ByteList bl = value.convertToString().getByteList();
            getMemoryIO(ptr).putZeroTerminatedByteArray(offset, bl.unsafeBytes(), bl.begin(),
                    Math.min(bl.length(), length - 1));
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            return MemoryUtil.getTaintedString(runtime, getMemoryIO(ptr), getOffset(ptr), length);
        }

        public Collection<StructLayout.Member> getMembers() {

            ArrayList<StructLayout.Member> members = new ArrayList<StructLayout.Member>(length);

            for (int i = 0; i < length; ++i) {
                members.add(new PrimitiveMember(type.getRuntime().getNil(), type, i, i * type.getNativeSize()));
            }

            return members;
        }
    }

    static final class CallbackMember extends StructLayout.Member {
        private final CallbackInfo cbInfo;

        CallbackMember(IRubyObject name, CallbackInfo cbInfo, int index, long offset) {
            super(name, cbInfo, index, offset);
            this.cbInfo = cbInfo;
        }
        
        @Override
        protected boolean isCacheable() {
            return true;
        }

        @Override
        protected boolean isValueReferenceNeeded() {
            return true;
        }

        public void put(Ruby runtime, Storage cache, IRubyObject ptr, IRubyObject value) {
            if (value.isNil()) {
                getMemoryIO(ptr).putAddress(getOffset(ptr), 0L);
            } else {
                Pointer cb = Factory.getInstance().getCallbackManager().getCallback(runtime, cbInfo, value);
                getMemoryIO(ptr).putMemoryIO(getOffset(ptr), cb.getMemoryIO());
                cache.putCachedValue(this, cb);
                cache.putReference(this, cb);
            }
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            return Factory.getInstance().newFunction(runtime, ((Pointer) ptr).getPointer(runtime, getOffset(ptr)), cbInfo);
        }
    }

    static final class StructMember extends StructLayout.Member implements StructLayout.Aggregate {
        private final RubyClass klass;
        private final StructLayout layout;

        StructMember(IRubyObject name, StructLayout layout, RubyClass klass, int index, long offset) {
            super(name, layout, index, offset);
            this.klass = klass;
            this.layout = layout;
        }
        
        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Struct fields");
        }

        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            IRubyObject s = cache.getCachedValue(this);
            if (s == null) {
                s = klass.newInstance(runtime.getCurrentContext(),
                        new IRubyObject[] { ((AbstractMemory) ptr).slice(runtime, getOffset(ptr)) },
                        Block.NULL_BLOCK);
                cache.putCachedValue(this, s);
            }

            return s;
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }

        @Override
        protected boolean isValueReferenceNeeded() {
            return true;
        }

        public Collection<StructLayout.Member> getMembers() {
            return layout.getFields();
        }
    }

    static final class ArrayMember extends StructLayout.Member implements StructLayout.Aggregate {
        private final MemoryOp op;
        private final Type.Array arrayType;

        ArrayMember(IRubyObject name, Type.Array arrayType, int index, long offset, MemoryOp op) {
            super(name, arrayType, index, offset);
            this.op = op;
            this.arrayType = arrayType;
        }

        public void put(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr, IRubyObject value) {
            throw runtime.newNotImplementedError("Cannot set Array fields");
        }
        
        public IRubyObject get(Ruby runtime, StructLayout.Storage cache, IRubyObject ptr) {
            IRubyObject s = cache.getCachedValue(this);
            if (s == null) {
                s = new StructLayout.Array(runtime, ptr, offset, arrayType, op);
                cache.putCachedValue(this, s);
            }

            return s;
        }

        @Override
        protected boolean isCacheable() {
            return true;
        }

        public Collection<StructLayout.Member> getMembers() {

            ArrayList<StructLayout.Member> members = new ArrayList<StructLayout.Member>(arrayType.length());
            Type elemType = arrayType.getComponentType();
            for (int i = 0; i < arrayType.length(); ++i) {
                members.add(new PrimitiveMember(getRuntime().getNil(), elemType, i, i * elemType.getNativeSize()));
            }

            return members;
        }
    }
}
