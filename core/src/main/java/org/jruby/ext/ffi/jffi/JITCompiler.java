/*
 *
 */
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.MappedType;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.util.WeakIdentityHashMap;
import org.jruby.util.cli.Options;

/**
 *
 */
class JITCompiler {
    
    private final Map<JITSignature, HandleRef> 
            handles = new HashMap<JITSignature, HandleRef>();

    private final Map<Class<? extends NativeInvoker>, JITHandle>
            classes = new WeakHashMap();

    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    
    private final JITHandle failedHandle = new JITHandle(this,
            new JITSignature(NativeType.VOID, new NativeType[0], false, new boolean[0], CallingConvention.DEFAULT, false),
            true);

    private static class SingletonHolder {
        private static final JITCompiler INSTANCE = new JITCompiler();
    }
    
    public static JITCompiler getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private static final class HandleRef extends WeakReference<JITHandle> {
        JITSignature signature;

        public HandleRef(JITHandle handle, JITSignature signature, ReferenceQueue refqueue) {
            super(handle, refqueue);
            this.signature = signature;
        }
    }

    private void cleanup() {
        HandleRef ref;
        while ((ref = (HandleRef) referenceQueue.poll()) != null) {
            handles.remove(ref.signature);
        }
    }
    
    
    JITHandle getHandle(Signature signature, boolean unique) {
        
        boolean hasResultConverter = !(signature.getResultType() instanceof Type.Builtin);
        NativeType nativeResultType;
        Type resultType = signature.getResultType();
        
        if (resultType instanceof Type.Builtin || resultType instanceof CallbackInfo) {
            nativeResultType = resultType.getNativeType();
        
        } else if (resultType instanceof MappedType) {
            nativeResultType = ((MappedType) resultType).getRealType().getNativeType();
        
        } else {
            return failedHandle;
        }

        NativeType[] nativeParameterTypes = new NativeType[signature.getParameterCount()];
        boolean[] hasParameterConverter = new boolean[signature.getParameterCount()];
        
        for (int i = 0; i < hasParameterConverter.length; i++) {
            Type parameterType = signature.getParameterType(i);
            if (parameterType instanceof Type.Builtin || parameterType instanceof CallbackInfo) {
                nativeParameterTypes[i] = parameterType.getNativeType();
        
            } else if (parameterType instanceof MappedType) {
                nativeParameterTypes[i] = ((MappedType) parameterType).getRealType().getNativeType();
        
            } else {
                return failedHandle;
            }

            hasParameterConverter[i] = !(parameterType instanceof Type.Builtin)
                    || DataConverters.isEnumConversionRequired(parameterType, signature.getEnums());
        }
        
        JITSignature jitSignature = new JITSignature(nativeResultType, nativeParameterTypes, 
                hasResultConverter, hasParameterConverter, signature.getCallingConvention(), signature.isIgnoreError());
        
        if (unique) {
            return new JITHandle(this, jitSignature, "OFF".equalsIgnoreCase(Options.COMPILE_MODE.load()));
        }

        synchronized (this) {
            cleanup();
            HandleRef ref = handles.get(jitSignature);
            JITHandle handle = ref != null ? ref.get() : null;
            if (handle == null) {
                handle = new JITHandle(this, jitSignature, "OFF".equalsIgnoreCase(Options.COMPILE_MODE.load()));
                handles.put(jitSignature, new HandleRef(handle, jitSignature, referenceQueue));
            }
            
            return handle;
        }
    }

    void registerClass(JITHandle handle, Class<? extends NativeInvoker> klass) {
        classes.put(klass, handle);
    }
}
