package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.ext.ffi.*;
import org.jruby.ext.ffi.Type;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 */
public class NativeCallbackFactory {
    private final ConcurrentMap<Integer, ClosureRef> closures = new ConcurrentHashMap<Integer, ClosureRef>();
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
    private final Ruby runtime;
    private final ClosurePool closurePool;
    private final NativeFunctionInfo closureInfo;
    private final CallbackInfo callbackInfo;
    private final RubyClass callbackClass;

    private final class ClosureRef extends WeakReference<Object> {
        final NativeCallbackPointer ptr;
        final Integer key;
        volatile ClosureRef next;

        private ClosureRef(Object callable, ReferenceQueue<? super Object> referenceQueue, Integer key, NativeCallbackPointer ptr) {
            super(callable, referenceQueue);
            this.key = key;
            this.ptr = ptr;
        }
    }

    public NativeCallbackFactory(Ruby runtime, CallbackInfo cbInfo) {
        this.runtime = runtime;
        this.closureInfo = newFunctionInfo(runtime, cbInfo);
        this.closurePool = com.kenai.jffi.ClosureManager.getInstance().getClosurePool(closureInfo.callContext);
        this.callbackInfo = cbInfo;
        this.callbackClass = runtime.getModule("FFI").getClass("Callback");
    }

    private void expunge(Reference<? extends Object> ref) {
        ClosureRef cl = ClosureRef.class.cast(ref);
        cl.clear();

        // Fast case - no chained elements; can just remove from the hash map
        if (cl.next == null && closures.remove(cl.key, cl)) {
            return;
        }

        // Remove from chained list
        synchronized (closures) {
            if (cl.next == null && closures.remove(cl.key, cl)) {
                return;
            }

            if (cl.next != null && closures.replace(cl.key, cl, cl.next)) {
                return;
            }

            for (ClosureRef cr = closures.get(cl.key); cr != null; cr = cr.next) {
                if (cr.next == cl) {
                    cr.next = cl.next;
                    return;
                }
            }
        }
    }


    private void expunge() {
        Reference<? extends Object> ref;
        while ((ref = referenceQueue.poll()) != null) {
            expunge(ref);
        }
    }

    public final Pointer getCallback(Object callable) {
        if (callable instanceof Pointer) {
            return (Pointer) callable;
        }

        Integer key = System.identityHashCode(callable);
        ClosureRef cl = closures.get(key);
        expunge();

        if (cl != null) {
            // Simple case - no identity hash code clash - just return the ptr
            if (cl.get() == callable) {
                return cl.ptr;
            }

            // There has been a key clash, search the list
            synchronized (closures) {
                while ((cl = cl.next) != null) {
                    if (cl.get() == callable) {
                        return cl.ptr;
                    }
                }
            }
        }

        return insert(newCallback(callable), callable, key);
    }

    private NativeCallbackPointer insert(NativeCallbackPointer ptr, Object callable, Integer key) {
        ClosureRef cl = new ClosureRef(callable, referenceQueue, key, ptr);
        if (closures.putIfAbsent(key, cl) == null) {
            return ptr;
        }

        synchronized (closures) {
            do {
                // prepend and make new pointer the list head
                cl.next = closures.get(key);

                // If old value already removed (e.g. by expunge), just put the new value in
                if (cl.next == null && closures.putIfAbsent(key, cl) == null) {
                    break;
                }
            } while (closures.replace(key, cl.next, cl));
        }

        return ptr;
    }

    NativeCallbackPointer newCallback(Object callable) {
        return new NativeCallbackPointer(runtime, callbackClass,
                closurePool.newClosureHandle(new NativeClosureProxy(runtime, closureInfo, callable)),
                callbackInfo, closureInfo);
    }

    private final NativeFunctionInfo newFunctionInfo(Ruby runtime, CallbackInfo cbInfo) {

        org.jruby.ext.ffi.Type[] paramTypes = cbInfo.getParameterTypes();
        for (int i = 0; i < paramTypes.length; ++i) {
            if (!isParameterTypeValid(paramTypes[i]) || FFIUtil.getFFIType(paramTypes[i]) == null) {
                throw runtime.newTypeError("invalid callback parameter type: " + paramTypes[i]);
            }
        }

        if (!isReturnTypeValid(cbInfo.getReturnType()) || FFIUtil.getFFIType(cbInfo.getReturnType()) == null) {
            runtime.newTypeError("invalid callback return type: " + cbInfo.getReturnType());
        }

        return new NativeFunctionInfo(runtime, cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
    }


    /**
     * Checks if a type is a valid callback return type
     *
     * @param type The type to examine
     * @return <tt>true</tt> if <tt>type</tt> is a valid return type for a callback.
     */
    private static final boolean isReturnTypeValid(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
                case FLOAT:
                case DOUBLE:
                case POINTER:
                case VOID:
                case BOOL:
                    return true;
            }

        } else if (type instanceof CallbackInfo) {
            return true;

        } else if (type instanceof StructByValue) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a type is a valid parameter type for a callback
     *
     * @param type The type to examine
     * @return <tt>true</tt> if <tt>type</tt> is a valid parameter type for a callback.
     */
    private static final boolean isParameterTypeValid(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
                case FLOAT:
                case DOUBLE:
                case POINTER:
                case STRING:
                case BOOL:
                    return true;
            }
        } else if (type instanceof CallbackInfo) {
            return true;

        } else if (type instanceof StructByValue) {
            return true;

        } else if (type instanceof MappedType) {
            return isParameterTypeValid(((MappedType) type).getRealType());
        }

        return false;
    }
}
