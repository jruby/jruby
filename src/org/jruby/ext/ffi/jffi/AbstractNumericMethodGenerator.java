package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Platform;
import org.jruby.ext.ffi.Buffer;
import org.jruby.RubyString;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Struct;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 *
 */
abstract class AbstractNumericMethodGenerator implements JITMethodGenerator {

    public void generate(AsmClassBuilder builder, String functionName, JITSignature signature) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(builder.getClassVisitor(),
                ACC_PUBLIC | ACC_FINAL, functionName,
                sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, signature.getParameterCount())),
                null, null);

        mv.start();
        generate(builder, mv, signature);
        mv.visitMaxs(10, 10);
        mv.visitEnd();
    }

    public void generate(AsmClassBuilder builder, SkinnyMethodAdapter mv, JITSignature signature) {
        final Class nativeIntType = getInvokerIntType();
        int maxPointerIndex = -1;
        Label[] fallback = new Label[signature.getParameterCount()];
        for (int i = 0; i < signature.getParameterCount(); i++) {
            fallback[i] = new Label();
        }
        
        mv.aload(1); // load ThreadContext arg for result boxing

        mv.getstatic(builder.getClassName(), "invoker", ci(com.kenai.jffi.Invoker.class));
        mv.aload(0);
        mv.getfield(builder.getClassName(), builder.getFunctionFieldName(), ci(com.kenai.jffi.Function.class));
        // [ stack now contains: Invoker, Function ]
        final int firstParam = 2;
        
        // Perform any generic data conversions on the parameters
        for (int i = 0; i < signature.getParameterCount(); ++i) {
            if (signature.hasParameterConverter(i)) {
                mv.aload(0);
                mv.getfield(builder.getClassName(), builder.getParameterConverterFieldName(i), ci(NativeDataConverter.class));
                mv.aload(1);              // ThreadContext
                mv.aload(firstParam + i); // IRubyObject
                mv.invokevirtual(p(NativeDataConverter.class), "toNative", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
                mv.astore(firstParam + i);
            }
        }

        // Load and un-box parameters
        for (int i = 0; i < signature.getParameterCount(); ++i) {
            final NativeType parameterType = signature.getParameterType(i);
            final int paramVar = i + firstParam;
            mv.aload(paramVar);
            switch (parameterType) {
                case BOOL:
                    mv.invokestatic(p(JITRuntime.class), "boolValue", sig(boolean.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;

                case CHAR:
                    mv.invokestatic(p(JITRuntime.class), "s8Value", sig(int.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;
                
                case UCHAR:
                    mv.invokestatic(p(JITRuntime.class), "u8Value", sig(int.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;
                
                case SHORT:
                    mv.invokestatic(p(JITRuntime.class), "s16Value", sig(int.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;
                
                case USHORT:
                    mv.invokestatic(p(JITRuntime.class), "u16Value", sig(int.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;
                
                case INT:
                    mv.invokestatic(p(JITRuntime.class), "s32Value", sig(int.class, IRubyObject.class));
                    widen(mv, int.class, nativeIntType);
                    break;
                
                case UINT:
                    mv.invokestatic(p(JITRuntime.class), "u32Value", sig(long.class, IRubyObject.class));
                    narrow(mv, long.class, nativeIntType);
                    break;
                    
                case LONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        mv.invokestatic(p(JITRuntime.class), "s32Value", sig(int.class, IRubyObject.class));
                        widen(mv, int.class, nativeIntType);
                    } else {
                        mv.invokestatic(p(JITRuntime.class), "s64Value", sig(long.class, IRubyObject.class));
                    }
                    break;
                
                case ULONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        mv.invokestatic(p(JITRuntime.class), "u32Value", sig(long.class, IRubyObject.class));
                        narrow(mv, long.class, nativeIntType);
                    } else {
                        mv.invokestatic(p(JITRuntime.class), "u64Value", sig(long.class, IRubyObject.class));
                    }
                    break;
                
                case LONG_LONG:
                    mv.invokestatic(p(JITRuntime.class), "s64Value", sig(long.class, IRubyObject.class));
                    break;
                
                case ULONG_LONG:
                    mv.invokestatic(p(JITRuntime.class), "u64Value", sig(long.class, IRubyObject.class));
                    break;
                
                case POINTER:
                case BUFFER_IN:
                case BUFFER_OUT:
                case BUFFER_INOUT:
                    maxPointerIndex = i;
                    Label direct = new Label();
                    Label done = new Label();
                    Label nilTest = new Label();
                    Label stringTest = new Label();
                    Label converted = new Label();
                    
                    // If a direct pointer is passed in, jump straight to conversion
                    mv.instance_of(p(Pointer.class));
                    mv.iftrue(direct);
                    
                    // If the parameter is a struct, fetch its memory pointer
                    mv.aload(paramVar);
                    mv.instance_of(p(Struct.class));
                    mv.iffalse(nilTest);
                    
                    mv.aload(paramVar);
                    mv.checkcast(p(Struct.class));
                    mv.invokevirtual(p(Struct.class), "getMemory", sig(AbstractMemory.class));
                    mv.go_to(converted);
                    
                    // Convert nil -> 0
                    mv.label(nilTest);
                    mv.aload(paramVar);
                    mv.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
                    mv.iffalse(stringTest);
                    if (int.class == nativeIntType) mv.iconst_0(); else mv.lconst_0();
                    mv.go_to(done);
                    
                    // If it is a String or Buffer, it can only be handled via the fallback route
                    mv.label(stringTest);
                    mv.aload(paramVar);
                    mv.instance_of(p(RubyString.class));
                    mv.iftrue(fallback[i]);
                    
                    mv.aload(paramVar);
                    mv.instance_of(p(Buffer.class));
                    mv.iftrue(fallback[i]);
                    
                    mv.aload(1);
                    mv.aload(paramVar);
                    mv.invokestatic(p(JITRuntime.class), "other2ptr", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
                    mv.label(converted);
                    mv.dup();
                    mv.astore(paramVar);
                    mv.instance_of(p(Pointer.class));
                    mv.iffalse(fallback[i]);
                    
                    mv.label(direct);
                    // The parameter is guaranteed to be a direct pointer now
                    mv.aload(paramVar);
                    mv.checkcast(p(Pointer.class));
                    mv.invokevirtual(p(Pointer.class), "getAddress", sig(long.class));
                    narrow(mv, long.class, nativeIntType);
                    mv.label(done);
                    break;

                case FLOAT:
                    if (int.class == nativeIntType) {
                        mv.invokestatic(p(JITRuntime.class), "float2int", sig(int.class, IRubyObject.class));
                    } else {
                        mv.invokestatic(p(JITRuntime.class), "float2long", sig(long.class, IRubyObject.class));
                    }
                    break;

                case DOUBLE:
                    mv.invokestatic(p(JITRuntime.class), "double2long", sig(long.class, IRubyObject.class));
                    break;

                default:
                    throw new UnsupportedOperationException("unsupported parameter type " + parameterType);
            }
        }

        // stack now contains [ Invoker, Function, int/long args ]
        mv.invokevirtual(p(com.kenai.jffi.Invoker.class),
                getInvokerMethodName(signature),
                getInvokerSignature(signature.getParameterCount()));


        // box up the raw int/long result
        boxResult(mv, signature.getResultType());
        emitResultConversion(mv, builder, signature);;
        mv.areturn();
        
        // Generate code to pop all the converted arguments off the stack 
        // when falling back to buffer-invocation
        if (maxPointerIndex >= 0) {
            for (int i = maxPointerIndex; i > 0; i--) {
                mv.label(fallback[i]);
                if (int.class == nativeIntType) {
                    mv.pop();
                } else {
                    mv.pop2();
                }
            }

            mv.label(fallback[0]);
            // Pop ThreadContext, Invoker and Function
            mv.pop(); mv.pop(); mv.pop();
            
            // Call the fallback invoker
            mv.aload(0);
            mv.getfield(builder.getClassName(), builder.getFallbackInvokerFieldName(), ci(NativeInvoker.class));
            mv.aload(1);
            
            for (int i = 0; i < signature.getParameterCount(); i++) {
                mv.aload(2 + i);
            }
            
            mv.invokevirtual(p(NativeInvoker.class), "invoke", 
                    sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, signature.getParameterCount())));
            emitResultConversion(mv, builder, signature);
            mv.areturn();
        }
    }

    private void emitResultConversion(SkinnyMethodAdapter mv, AsmClassBuilder builder, JITSignature signature) {
        if (signature.hasResultConverter()) {
            mv.aload(0); // [ result, this ]
            mv.getfield(builder.getClassName(), builder.getResultConverterFieldName(), ci(NativeDataConverter.class));
            mv.swap();   // [ converter, result ]
            mv.aload(1);
            mv.swap();   // [ converter, thread context, result ]
            mv.invokevirtual(p(NativeDataConverter.class), "fromNative", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        }
    }
    
    private void boxResult(SkinnyMethodAdapter mv, NativeType type,
            String boxMethodName, Class primitiveType) {
        // convert to the appropriate primitiv result type
        narrow(mv, getInvokerIntType(), primitiveType);
        widen(mv, getInvokerIntType(), primitiveType);
        
        mv.invokestatic(p(JITRuntime.class), boxMethodName,
                sig(IRubyObject.class, ThreadContext.class, primitiveType));
    }

    private void boxResult(SkinnyMethodAdapter mv, NativeType type) {
        switch (type) {
            case BOOL:
                boxResult(mv, type, "newBoolean", getInvokerIntType());
                break;

            case CHAR:
                boxResult(mv, type, "newSigned8", byte.class);
                break;

            case UCHAR:
                boxResult(mv, type, "newUnsigned8", byte.class);
                break;

            case SHORT:
                boxResult(mv, type, "newSigned16", short.class);
                break;

            case USHORT:
                boxResult(mv, type, "newUnsigned16", short.class);
                break;

            case INT:
                boxResult(mv, type, "newSigned32", int.class);
                break;

            case UINT:
                boxResult(mv, type, "newUnsigned32", int.class);
                break;

            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, type, "newSigned32", int.class);
                } else {
                    boxResult(mv, type, "newSigned64", long.class);
                }
                break;

            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, type, "newUnsigned32", int.class);
                } else {
                    boxResult(mv, type, "newUnsigned64", long.class);
                }
                break;

            case LONG_LONG:
                boxResult(mv, type, "newSigned64", long.class);
                break;

            case ULONG_LONG:
                boxResult(mv, type, "newUnsigned64", long.class);
                break;
                
            case FLOAT:
                boxResult(mv, type, "newFloat32", getInvokerIntType());
                break;
                
            case DOUBLE:
                boxResult(mv, type, "newFloat64", long.class);
                break;

            case VOID:
                boxResult(mv, type, "newNil", getInvokerIntType());
                break;

            case POINTER:
                boxResult(mv, type, "newPointer" + Platform.getPlatform().addressSize(),
                    getInvokerIntType());
                break;

            case STRING:
                boxResult(mv, type, "newString", getInvokerIntType());
                break;


            default:
                throw new UnsupportedOperationException("native return type not supported: " + type);

        }
    }

    abstract String getInvokerMethodName(JITSignature signature);

    abstract String getInvokerSignature(int parameterCount);

    abstract Class getInvokerIntType();


    public static boolean isPrimitiveInt(Class c) {
        return byte.class == c || char.class == c || short.class == c || int.class == c || boolean.class == c;
    }

    public static final void widen(SkinnyMethodAdapter mv, Class from, Class to) {
        if (long.class == to && long.class != from && isPrimitiveInt(from)) {
            mv.i2l();
        }
    }

    public static final void narrow(SkinnyMethodAdapter mv, Class from, Class to) {
        if (!from.equals(to) && isPrimitiveInt(to)) {
            if (long.class == from) {
                mv.l2i();
            }

            if (byte.class == to) {
                mv.i2b();

            } else if (short.class == to) {
                mv.i2s();

            } else if (char.class == to) {
                mv.i2c();

            } else if (boolean.class == to) {
                // Ensure only 0x0 and 0x1 values are used for boolean
                mv.iconst_1();
                mv.iand();
            }
        }
    }
    
    protected static String[] buildSignatures(Class nativeIntClass, int maxParameters) {
        char sigChar = int.class == nativeIntClass ? 'I' : 'J';
        
        String[] signatures = new String[maxParameters + 1];
        for (int i = 0; i < signatures.length; i++) {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append('(').append(ci(com.kenai.jffi.Function.class));
            
            for (int n = 0; n < i; n++) {
                sb.append(sigChar);
            }
            
            signatures[i] = sb.append(")").append(sigChar).toString();
        }
        
        return signatures;
    }
}
