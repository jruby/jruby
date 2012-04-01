package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ext.ffi.NativeType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

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
        mv.visitMaxs(30, 30);
        mv.visitEnd();
    }

    public void generate(AsmClassBuilder builder, SkinnyMethodAdapter mv, JITSignature signature) {
        final Class nativeIntType = getInvokerIntType();
        int pointerCount = 0;

        mv.aload(1); // load ThreadContext arg for result boxing
        mv.getstatic(p(JITNativeInvoker.class), "invoker", ci(com.kenai.jffi.Invoker.class));
        mv.aload(0);
        mv.getfield(p(JITNativeInvoker.class), "callContext", ci(CallContext.class));
        mv.aload(0);
        mv.getfield(p(JITNativeInvoker.class), "functionAddress", ci(long.class));

        // [ stack now contains: Invoker, CalLContext, function address ]

        final int firstParam = 2;
        int nextLocalVar = firstParam + signature.getParameterCount();
        final int heapPointerCountVar = nextLocalVar++;
        final int firstStrategyVar = nextLocalVar; nextLocalVar += signature.getParameterCount();
        int nextStrategyVar = firstStrategyVar;
        
        // Perform any generic data conversions on the parameters
        for (int i = 0; i < signature.getParameterCount(); i++) {
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
        for (int i = 0; i < signature.getParameterCount(); i++) {
            final NativeType parameterType = signature.getParameterType(i);
            final int paramVar = i + firstParam;
            mv.aload(paramVar);
            switch (parameterType) {
                case BOOL:
                    unbox(mv, "boolValue");
                    break;

                case CHAR:
                    unbox(mv, "s8Value");
                    break;
                
                case UCHAR:
                    unbox(mv, "u8Value");
                    break;
                
                case SHORT:
                    unbox(mv, "s16Value");
                    break;
                
                case USHORT:
                    unbox(mv, "u16Value");
                    break;
                
                case INT:
                    unbox(mv, "s32Value");
                    break;
                
                case UINT:
                    unbox(mv, "u32Value");
                    break;
                    
                case LONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        unbox(mv, "s32Value");
                    } else {
                        unbox(mv, "s64Value");
                    }
                    break;
                
                case ULONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        unbox(mv, "u32Value");
                    } else {
                        unbox(mv, "u64Value");
                    }
                    break;
                
                case LONG_LONG:
                    unbox(mv, "s64Value");
                    break;
                
                case ULONG_LONG:
                    unbox(mv, "u64Value");
                    break;
                
                case POINTER:
                case BUFFER_IN:
                case BUFFER_OUT:
                case BUFFER_INOUT:
                case STRING:
                case TRANSIENT_STRING:
                    Label address = new Label();
                    Label next = new Label();
                    if (pointerCount++ < 1) {
                        mv.pushInt(0);
                        mv.istore(heapPointerCountVar);
                    }

                    String strategyMethod = parameterType == NativeType.STRING
                            ? "stringParameterStrategy"
                            : parameterType == NativeType.TRANSIENT_STRING ? "transientStringParameterStrategy" : "pointerParameterStrategy";
                    mv.invokestatic(p(JITRuntime.class), strategyMethod,
                            sig(PointerParameterStrategy.class, IRubyObject.class));
                    mv.astore(nextStrategyVar);
                    mv.aload(nextStrategyVar);
                    mv.invokevirtual(p(ObjectParameterStrategy.class), "isDirect", sig(boolean.class));
                    mv.iftrue(address);
                    mv.iinc(heapPointerCountVar, 1);
                    mv.label(address);
                    // It is now direct, get the address, and convert to the native int type
                    mv.aload(nextStrategyVar);
                    mv.aload(paramVar);
                    mv.invokevirtual(p(ObjectParameterStrategy.class), "address", sig(long.class, Object.class));
                    narrow(mv, long.class, nativeIntType);
                    nextStrategyVar++;
                    mv.label(next);
                    break;

                case FLOAT:
                    unbox(mv, "f32Value");
                    break;

                case DOUBLE:
                    unbox(mv, "f64Value");
                    break;

                default:
                    throw new UnsupportedOperationException("unsupported parameter type " + parameterType);
            }
        }

        Label indirect = new Label();
        if (pointerCount > 0) {
            mv.iload(heapPointerCountVar);
            mv.ifne(indirect);
        }

        // stack now contains [ Invoker, Function, int/long args ]
        mv.invokevirtual(p(com.kenai.jffi.Invoker.class),
                getInvokerMethodName(signature),
                getInvokerSignature(signature.getParameterCount()));


        Label boxResult = new Label();
        if (pointerCount > 0) mv.label(boxResult);

        // box up the raw int/long result
        boxResult(mv, signature.getResultType());
        Label resultConversion = new Label();
        if (pointerCount > 0) mv.label(resultConversion);
        emitResultConversion(mv, builder, signature);
        mv.areturn();

        // Handle non-direct pointer parameters
        if (pointerCount > 0) {
            mv.label(indirect);

            if (int.class == nativeIntType) {
                final int firstIntParam = nextLocalVar;
                for (int i = 0; i < signature.getParameterCount() - 1; i++) {
                    mv.istore(firstIntParam + i);
                }

                mv.i2l();
                // reload the rest and convert to long
                for (int i = signature.getParameterCount() - 2; i >= 0; i--) {
                    mv.iload(firstIntParam + i);
                    mv.i2l();
                }
            }

            mv.iload(heapPointerCountVar);

            // Just load all the pointer parameters, conversion strategies and parameter info onto
            // the operand stack, so the helper functions can sort them out.
            for (int i = 0, ptrIdx = 0; i < signature.getParameterCount(); i++) {
                switch (signature.getParameterType(i)) {
                    case POINTER:
                    case BUFFER_IN:
                    case BUFFER_OUT:
                    case BUFFER_INOUT:
                    case STRING:
                    case TRANSIENT_STRING:
                        mv.aload(firstParam + i);
                        mv.aload(firstStrategyVar + ptrIdx);
                        mv.aload(0);
                        mv.getfield(p(JITNativeInvoker.class), "parameterInfo" + i, ci(ObjectParameterInfo.class));
                        ptrIdx++;
                        break;
                }
            }

                
            mv.invokevirtual(p(Invoker.class), "invokeN" + signature.getParameterCount(),
                    sig(long.class, makeObjectParamSignature(signature, pointerCount)));
            narrow(mv, long.class, nativeIntType);
            mv.go_to(boxResult);
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
    
    private static Class[] makeObjectParamSignature(JITSignature signature, int pointerCount) {
        Class[] paramTypes = new Class[3 + signature.getParameterCount() + (pointerCount * 3)];
        int idx = 0;

        paramTypes[idx++] = CallContext.class;
        paramTypes[idx++] = long.class;

        for (int i = 0; i < signature.getParameterCount(); i++) {
            paramTypes[idx++] = long.class;
        }
        
        paramTypes[idx++] = int.class;

        for (int i = 0; i < pointerCount; i++) {
            paramTypes[idx++] = Object.class;
            paramTypes[idx++] = ObjectParameterStrategy.class;
            paramTypes[idx++] = ObjectParameterInfo.class;
        }

        return paramTypes;
    }


    private void unbox(SkinnyMethodAdapter mv, String method) {
        mv.invokestatic(p(JITRuntime.class), getRuntimeMethod(method), sig(getInvokerIntType(), IRubyObject.class));
    }

    private String getRuntimeMethod(String method) {
        return method + (int.class == getInvokerIntType() ? "32" : "64");
    }

    private void boxResult(SkinnyMethodAdapter mv,
                           String boxMethodName) {
        mv.invokestatic(p(JITRuntime.class), boxMethodName,
                sig(IRubyObject.class, ThreadContext.class, getInvokerIntType()));
    }

    private void boxResult(SkinnyMethodAdapter mv, NativeType type) {
        switch (type) {
            case BOOL:
                boxResult(mv, "newBoolean");
                break;

            case CHAR:
                boxResult(mv, "newSigned8");
                break;

            case UCHAR:
                boxResult(mv, "newUnsigned8");
                break;

            case SHORT:
                boxResult(mv, "newSigned16");
                break;

            case USHORT:
                boxResult(mv, "newUnsigned16");
                break;

            case INT:
                boxResult(mv, "newSigned32");
                break;

            case UINT:
                boxResult(mv, "newUnsigned32");
                break;

            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, "newSigned32");
                } else {
                    boxResult(mv, "newSigned64");
                }
                break;

            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, "newUnsigned32");
                } else {
                    boxResult(mv, "newUnsigned64");
                }
                break;

            case LONG_LONG:
                boxResult(mv, "newSigned64");
                break;

            case ULONG_LONG:
                boxResult(mv, "newUnsigned64");
                break;
                
            case FLOAT:
                boxResult(mv, "newFloat32");
                break;
                
            case DOUBLE:
                boxResult(mv, "newFloat64");
                break;

            case VOID:
                if (int.class == getInvokerIntType()) mv.pop(); else mv.pop2();
                mv.getfield(p(ThreadContext.class), "nil", ci(IRubyObject.class));
                break;

            case POINTER:
                boxResult(mv, "newPointer" + Platform.getPlatform().addressSize());
                break;

            case STRING:
            case TRANSIENT_STRING:
                boxResult(mv, "newString");
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
            
            sb.append('(').append(ci(CallContext.class)).append(ci(long.class));
            
            for (int n = 0; n < i; n++) {
                sb.append(sigChar);
            }
            
            signatures[i] = sb.append(")").append(sigChar).toString();
        }
        
        return signatures;
    }
}
