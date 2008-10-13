package org.jruby.runtime.invokedynamic;

import java.dyn.CallSite;
import java.dyn.Linkage;
import java.dyn.MethodHandle;
import java.dyn.MethodHandles;
import java.dyn.MethodType;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class InvokeDynamicSupport {
    public static Object bootstrap(final CallSite site, Object... args) {
        // dynamic call
        IRubyObject self = (IRubyObject) args[0];
        ThreadContext context = (ThreadContext) args[1];
        String methodName = (String) args[2];
        CallType callType = CallType.NORMAL;
        String siteName = site.name();
        boolean iterator = siteName.length() == 2 && siteName.charAt(1) == 'b';
        
        switch (siteName.charAt(0)) {
        case 'c':
            callType = CallType.NORMAL;
            break;
        case 'f':
            callType = CallType.FUNCTIONAL;
            break;
        case 'v':
            callType = CallType.VARIABLE;
            break;
        }

        DynamicMethod method = self.getMetaClass().searchMethod(methodName);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, methodName, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, callType, Block.NULL_BLOCK);
        }

        String dispatcherName = iterator ? "invokeDynamicIter" : "invokeDynamic";
        MethodHandle outHandle = MethodHandles.findStatic(
                InvokeDynamicSupport.class,
                dispatcherName,
                MethodType.make(IRubyObject.class, DynamicMethod.class, site.type().parameterArray()));
        MethodHandle inHandle = MethodHandles.insertArgument(outHandle, method);
        site.setTarget(inHandle);

        // do normal invocation this time
        int length = args.length;
        if (args[length - 1] instanceof Block) {
            if (iterator) {
                switch (length) {
                case 4:
                    return invokeDynamicIter(method, self, context, methodName, (Block) args[length - 1]);
                case 5:
                    if (args[length - 2] instanceof IRubyObject[]) {
                        return invokeDynamicIter(method, self, context, methodName, (IRubyObject[]) args[length - 2], (Block) args[length - 1]);
                    } else {
                        return invokeDynamicIter(method, self, context, methodName, (IRubyObject) args[length - 2], (Block) args[length - 1]);
                    }
                case 6:
                    return invokeDynamicIter(method, self, context, methodName, (IRubyObject) args[length - 3], (IRubyObject) args[length - 2], (Block) args[length - 1]);
                case 7:
                    return invokeDynamicIter(method, self, context, methodName, (IRubyObject) args[length - 4], (IRubyObject) args[length - 3], (IRubyObject) args[length - 2], (Block) args[length - 1]);
                }
            } else {
                switch (length) {
                case 4:
                    return invokeDynamic(method, self, context, methodName, (Block) args[length - 1]);
                case 5:
                    if (args[length - 2] instanceof IRubyObject[]) {
                        return invokeDynamic(method, self, context, methodName, (IRubyObject[]) args[length - 2], (Block) args[length - 1]);
                    } else {
                        return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 2], (Block) args[length - 1]);
                    }
                case 6:
                    return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 3], (IRubyObject) args[length - 2], (Block) args[length - 1]);
                case 7:
                    return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 4], (IRubyObject) args[length - 3], (IRubyObject) args[length - 2], (Block) args[length - 1]);
                }
            }
        } else {
            switch (length) {
            case 3:
                return invokeDynamic(method, self, context, methodName);
            case 4:
                if (args[length - 1] instanceof IRubyObject[]) {
                    return invokeDynamic(method, self, context, methodName, (IRubyObject[]) args[length - 1]);
                } else {
                    return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 1]);
                }
            case 5:
                return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 2], (IRubyObject) args[length - 1]);
            case 6:
                return invokeDynamic(method, self, context, methodName, (IRubyObject) args[length - 3], (IRubyObject) args[length - 2], (IRubyObject) args[length - 1]);
            }
        }

        throw new RuntimeException("Unsupported method signature for dynamic call: " + site.type());
    }
    
    public static void installBytecode(MethodVisitor method, String classname) {
        SkinnyMethodAdapter clinitMethod = new SkinnyMethodAdapter(method);
        clinitMethod.ldc(c(classname));
        clinitMethod.invokestatic(p(Class.class), "forName", sig(Class.class, params(String.class)));
        clinitMethod.ldc(Type.getType(InvokeDynamicSupport.class));
        clinitMethod.ldc("bootstrap");
        clinitMethod.getstatic(p(Linkage.class), "BOOTSTRAP_METHOD_TYPE", ci(MethodType.class));
        clinitMethod.invokestatic(p(MethodHandles.class), "findStatic", sig(MethodHandle.class, Class.class, String.class, MethodType.class));
        clinitMethod.invokestatic(p(Linkage.class), "registerBootstrapMethod", sig(void.class, Class.class, MethodHandle.class));
    }

    private static boolean shouldCallMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return method.isUndefined() || notVisibleAndNotMethodMissing(method, name, caller, callType);
    }

    private static boolean notVisibleAndNotMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return !method.isCallableFrom(caller, callType) && !name.equals("method_missing");
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name) {
        IRubyObject self = (IRubyObject) selfObj;
        return method.call(context, self, self.getMetaClass(), name);
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public static IRubyObject invokeDynamicIter(DynamicMethod method, Object selfObj, ThreadContext context, String name, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg) {
        IRubyObject self = (IRubyObject) selfObj;
        return method.call(context, self, self.getMetaClass(), name, arg);
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public static IRubyObject invokeDynamicIter(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject self = (IRubyObject) selfObj;
        return method.call(context, self, self.getMetaClass(), name, arg0, arg1);
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg0, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public static IRubyObject invokeDynamicIter(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg0, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject self = (IRubyObject) selfObj;
        return method.call(context, self, self.getMetaClass(), name, arg0, arg1, arg2);
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg0, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public static IRubyObject invokeDynamicIter(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, arg0, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject[] args) {
        IRubyObject self = (IRubyObject) selfObj;
        return method.call(context, self, self.getMetaClass(), name, args);
    }

    public static IRubyObject invokeDynamic(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject[] args, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public static IRubyObject invokeDynamicIter(DynamicMethod method, Object selfObj, ThreadContext context, String name, IRubyObject[] args, Block block) {
        IRubyObject self = (IRubyObject) selfObj;
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            return method.call(context, self, selfType, name, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    private static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();

        RubyClass selfType = self.getMetaClass();

        return selfType;
    }

    private static IRubyObject handleBreakJump(ThreadContext context, JumpException.BreakJump bj) throws JumpException.BreakJump {
        // consume and rethrow or just keep rethrowing?
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    private static RaiseException retryJumpError(ThreadContext context) {
        return context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }
}
