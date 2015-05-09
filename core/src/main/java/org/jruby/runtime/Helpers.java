package org.jruby.runtime;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;

import jnr.constants.platform.Errno;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.platform.Platform;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;
import org.jruby.util.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;

import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.util.CodegenUtils.sig;

import org.jruby.util.JavaNameMangler;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code.
 *
 */
public class Helpers {
    public static CallSite selectAttrAsgnCallSite(IRubyObject receiver, IRubyObject self, CallSite normalSite, CallSite variableSite) {
        if (receiver == self) return variableSite;
        return normalSite;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject arg1, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, arg1, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, arg1, arg2, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject[] args, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, args);
        return args[args.length - 1];
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject[] args, IRubyObject value, ThreadContext context, IRubyObject caller) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = value;
        callSite.call(context, caller, receiver, newArgs);
        return value;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject[] receivers) {
        for (int i = 0; i < receivers.length; i++) {
            IRubyObject receiver = receivers[i];
            if (invokeEqqForCaseWhen(callSite, context, caller, arg, receiver)) return true;
        }
        return false;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver) {
        IRubyObject result = callSite.call(context, caller, receiver, arg);
        if (result.isTrue()) return true;
        return false;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver0, IRubyObject receiver1) {
        IRubyObject result = callSite.call(context, caller, receiver0, arg);
        if (result.isTrue()) return true;
        return invokeEqqForCaseWhen(callSite, context, caller, arg, receiver1);
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver0, IRubyObject receiver1, IRubyObject receiver2) {
        IRubyObject result = callSite.call(context, caller, receiver0, arg);
        if (result.isTrue()) return true;
        return invokeEqqForCaseWhen(callSite, context, caller, arg, receiver1, receiver2);
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject[] receivers) {
        for (int i = 0; i < receivers.length; i++) {
            if (receivers[i].isTrue()) return true;
        }
        return false;
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver) {
        return receiver.isTrue();
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver0, IRubyObject receiver1) {
        return receiver0.isTrue() || receiver1.isTrue();
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver0, IRubyObject receiver1, IRubyObject receiver2) {
        return receiver0.isTrue() || receiver1.isTrue() || receiver2.isTrue();
    }

    public static String[] parseBlockDescriptor(String descriptor) {
        String[] mangled = descriptor.split(":");
        mangled[0] = JavaNameMangler.demangleMethodName(mangled[0]);
        mangled[4] = JavaNameMangler.demangleMethodName(mangled[4]);
        return mangled;
    }

    public static RubyClass getSingletonClass(Ruby runtime, IRubyObject receiver) {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton");
        } else {
            return receiver.getSingletonClass();
        }
    }

    // TODO: Only used by interface implementation; eliminate it
    public static IRubyObject invokeMethodMissing(IRubyObject receiver, String name, IRubyObject[] args) {
        ThreadContext context = receiver.getRuntime().getCurrentContext();

        // store call information so method_missing impl can use it
        context.setLastCallStatusAndVisibility(CallType.FUNCTIONAL, Visibility.PUBLIC);

        if (name.equals("method_missing")) {
            return RubyKernel.method_missing(context, receiver, args, Block.NULL_BLOCK);
        }

        IRubyObject[] newArgs = prepareMethodMissingArgs(args, context, name);

        return invoke(context, receiver, "method_missing", newArgs, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject[] args, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, args, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, arg1, arg2, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, block);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType) {
        Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = receiver.getMetaClass().searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, visibility, callType);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = selfClass.searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, visibility, callType);
    }

    public static DynamicMethod selectMethodMissing(RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        Ruby runtime = selfClass.getClassRuntime();

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = selfClass.searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, visibility, callType);
    }

    public static final Map<String, String> map(String... keyValues) {
        HashMap<String, String> map = new HashMap<String, String>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length;) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

    /**
     * Should be called on jumps out of blocks.  Inspects the jump, returning or rethrowing as appropriate
     */
    public static IRubyObject handleBlockJump(ThreadContext context, JumpException.FlowControlException jump, Block.Type type) {
        // 'next' and Lambda 'return' are local returns from the block, ending the call or yield
        if (jump instanceof JumpException.NextJump
                || (jump instanceof JumpException.ReturnJump && type == Block.Type.LAMBDA)) {
            return jump.getValue() == null ? context.runtime.getNil() : (IRubyObject)jump.getValue();
        }

        // other jumps propagate up
        throw jump;
    }

    public static boolean additionOverflowed(long original, long other, long result) {
        return (~(original ^ other) & (original ^ result) & RubyFixnum.SIGN_BIT) != 0;
    }

    public static boolean subtractionOverflowed(long original, long other, long result) {
        return (~(original ^ ~other) & (original ^ result) & RubyFixnum.SIGN_BIT) != 0;
    }

    public static Errno errnoFromException(Throwable t) {
        if (t instanceof ClosedChannelException) {
            return Errno.EBADF;
        }

        // TODO: this is kinda gross
        if(t.getMessage() != null) {
            String errorMessage = t.getMessage();

            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...

            if ("Bad file descriptor".equals(errorMessage)) {
                return Errno.EBADF;
            } else if ("File not open".equals(errorMessage)) {
                return null;
            } else if ("An established connection was aborted by the software in your host machine".equals(errorMessage)) {
                return Errno.ECONNABORTED;
            } else if (t.getMessage().equals("Broken pipe")) {
                return Errno.EPIPE;
            } else if ("Connection reset by peer".equals(errorMessage) ||
                       "An existing connection was forcibly closed by the remote host".equals(errorMessage) ||
                    (Platform.IS_WINDOWS && errorMessage.contains("connection was aborted"))) {
                return Errno.ECONNRESET;
            } else if (errorMessage.equals("No space left on device")) {
                return Errno.ENOSPC;
            }
        }
        return null;
    }

    public static RubyModule getNthScopeModule(StaticScope scope, int depth) {
        int n = depth;
        while (n > 0) {
            scope = scope.getEnclosingScope();
            if (scope.getScopeType() != null) {
                n--;
            }
        }
        return scope.getModule();
    }

    public static RubyArray viewArgsArray(ThreadContext context, RubyArray rubyArray, int preArgsCount, int postArgsCount) {
        int n = rubyArray.getLength();
        if ((preArgsCount >= n) || (preArgsCount + postArgsCount >= n)) {
            return RubyArray.newEmptyArray(context.runtime);
        } else {
            return (RubyArray)rubyArray.subseqLight(preArgsCount, n - preArgsCount - postArgsCount);
        }
    }

    public static Class[] getStaticMethodParams(Class target, int args) {
        switch (args) {
        case 0:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, Block.class};
        case 1:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 2:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 3:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 4:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class};
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    public static String getStaticMethodSignature(String classname, int args) {
        switch (args) {
        case 0:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, Block.class);
        case 1:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 2:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 3:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 4:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    private static class MethodMissingMethod extends DynamicMethod {
        private final DynamicMethod delegate;
        private final CallType lastCallStatus;
        private final Visibility lastVisibility;

        public MethodMissingMethod(DynamicMethod delegate, Visibility lastVisibility, CallType lastCallStatus) {
            this.delegate = delegate;
            this.lastCallStatus = lastCallStatus;
            this.lastVisibility = lastVisibility;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.delegate.call(context, self, clazz, "method_missing", prepareMethodMissingArgs(args, context, name), block);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }
    }

    private static DynamicMethod selectInternalMM(Ruby runtime, Visibility visibility, CallType callType) {
        if (visibility == Visibility.PRIVATE) {
            return runtime.getPrivateMethodMissing();
        } else if (visibility == Visibility.PROTECTED) {
            return runtime.getProtectedMethodMissing();
        } else if (callType == CallType.VARIABLE) {
            return runtime.getVariableMethodMissing();
        } else if (callType == CallType.SUPER) {
            return runtime.getSuperMethodMissing();
        } else {
            return runtime.getNormalMethodMissing();
        }
    }

    private static IRubyObject[] prepareMethodMissingArgs(IRubyObject[] args, ThreadContext context, String name) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = context.runtime.newSymbol(name);

        return newArgs;
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, Block block) {
        return self.getMetaClass().finvoke(context, self, name, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return self.getMetaClass().finvoke(context, self, name, args, block);
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name) {
        return self.getMetaClass().finvoke(context, self, name);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0) {
        return self.getMetaClass().finvoke(context, self, name, arg0);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return self.getMetaClass().finvoke(context, self, name, args);
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, CallType callType) {
        return Helpers.invoke(context, self, name, IRubyObject.NULL_ARRAY, callType, Block.NULL_BLOCK);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, args, callType, block);
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, arg, callType, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return asClass.finvoke(context, self, name, args, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, Block block) {
        return asClass.finvoke(context, self, name, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return asClass.finvoke(context, self, name, arg0, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, String name) {
        return self.getMetaClass().finvokeChecked(context, self, name);
    }

    /**
     * The protocol for super method invocation is a bit complicated
     * in Ruby. In real terms it involves first finding the real
     * implementation class (the super class), getting the name of the
     * method to call from the frame, and then invoke that on the
     * super class with the current self as the actual object
     * invoking.
     */
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return invokeSuper(context, self, context.getFrameKlazz(), context.getFrameName(), args, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, RubyModule klass, String name, IRubyObject[] args, Block block) {
        checkSuperDisabledOrOutOfMethod(context, klass, name);

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klass).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, args, block);
        }
        return method.call(context, self, superClass, name, args, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, block);
        }
        return method.call(context, self, superClass, name, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, block);
        }
        return method.call(context, self, superClass, name, arg0, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, arg1, block);
        }
        return method.call(context, self, superClass, name, arg0, arg1, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, arg1, arg2, block);
        }
        return method.call(context, self, superClass, name, arg0, arg1, arg2, block);
    }

    public static RubyArray ensureRubyArray(IRubyObject value) {
        return ensureRubyArray(value.getRuntime(), value);
    }

    public static RubyArray ensureRubyArray(Ruby runtime, IRubyObject value) {
        return value instanceof RubyArray ? (RubyArray)value : RubyArray.newArray(runtime, value);
    }

    public static RubyArray ensureMultipleAssignableRubyArray(IRubyObject value, Ruby runtime, boolean masgnHasHead) {
        if (!(value instanceof RubyArray)) {
            value = ArgsUtil.convertToRubyArray19(runtime, value, masgnHasHead);
        }
        return (RubyArray) value;
    }

    public static IRubyObject nullToNil(IRubyObject value, ThreadContext context) {
        return value != null ? value : context.nil;
    }

    public static IRubyObject nullToNil(IRubyObject value, Ruby runtime) {
        return value != null ? value : runtime.getNil();
    }

    public static IRubyObject nullToNil(IRubyObject value, IRubyObject nil) {
        return value != null ? value : nil;
    }

    public static RubyClass prepareSuperClass(Ruby runtime, IRubyObject rubyClass) {
        RubyClass.checkInheritable(rubyClass); // use the same logic as in EvaluationState
        return (RubyClass)rubyClass;
    }

    public static RubyModule prepareClassNamespace(ThreadContext context, StaticScope scope, IRubyObject rubyModule) {
        if (rubyModule == null || rubyModule.isNil()) {
            rubyModule = scope.getModule();

            if (rubyModule == null) {
                throw context.runtime.newTypeError("no outer class/module");
            }
        }

        if (rubyModule instanceof RubyModule) {
            return (RubyModule)rubyModule;
        } else {
            throw context.runtime.newTypeError(rubyModule + " is not a class/module");
        }
    }

    public static void handleArgumentSizes(ThreadContext context, Ruby runtime, int given, int required, int opt, int rest) {
        if (opt == 0) {
            if (rest < 0) {
                // no opt, no rest, exact match
                if (given != required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            } else {
                // only rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            }
        } else {
            if (rest < 0) {
                // opt but no rest, must be at least required and no more than required + opt
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                } else if (given > (required + opt)) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + (required + opt) + ")");
                }
            } else {
                // opt and rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            }
        }
    }

    public static String getLocalJumpTypeOrRethrow(RaiseException re) {
        RubyException exception = re.getException();
        Ruby runtime = exception.getRuntime();
        if (runtime.getLocalJumpError().isInstance(exception)) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            IRubyObject reason = jumpError.reason();

            return reason.asJavaString();
        }

        throw re;
    }

    public static IRubyObject unwrapLocalJumpErrorValue(RaiseException re) {
        return ((RubyLocalJumpError)re.getException()).exit_value();
    }

    public static IRubyObject processBlockArgument(Ruby runtime, Block block) {
        if (!block.isGiven()) {
            return runtime.getNil();
        }

        return processGivenBlock(block, runtime);
    }

    private static IRubyObject processGivenBlock(Block block, Ruby runtime) {
        RubyProc blockArg = block.getProcObject();

        if (blockArg == null) {
            blockArg = runtime.newBlockPassProc(Block.Type.PROC, block);
            blockArg.getBlock().type = Block.Type.PROC;
        }

        return blockArg;
    }

    public static Block getBlockFromBlockPassBody(Ruby runtime, IRubyObject proc, Block currentBlock) {
        // No block from a nil proc
        if (proc.isNil()) return Block.NULL_BLOCK;

        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
            proc = coerceProc(proc, runtime);
        }

        return getBlockFromProc(currentBlock, proc);
    }

    private static IRubyObject coerceProc(IRubyObject maybeProc, Ruby runtime) throws RaiseException {
        IRubyObject proc = TypeConverter.convertToType(maybeProc, runtime.getProc(), "to_proc", false);

        if (!(proc instanceof RubyProc)) {
            throw runtime.newTypeError("wrong argument type " + maybeProc.getMetaClass().getName() + " (expected Proc)");
        }

        return proc;
    }

    private static Block getBlockFromProc(Block currentBlock, IRubyObject proc) {
        // TODO: Add safety check for taintedness
        if (currentBlock != null && currentBlock.isGiven()) {
            RubyProc procObject = currentBlock.getProcObject();
            // The current block is already associated with proc.  No need to create a new one
            if (procObject != null && procObject == proc) {
                return currentBlock;
            }
        }

        return ((RubyProc) proc).getBlock();
    }

    public static Block getBlockFromBlockPassBody(IRubyObject proc, Block currentBlock) {
        return getBlockFromBlockPassBody(proc.getRuntime(), proc, currentBlock);

    }

    public static IRubyObject backref(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        if(backref instanceof RubyMatchData) {
            ((RubyMatchData)backref).use();
        }
        return backref;
    }

    public static IRubyObject backrefLastMatch(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.last_match(backref);
    }

    public static IRubyObject backrefMatchPre(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_pre(backref);
    }

    public static IRubyObject backrefMatchPost(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_post(backref);
    }

    public static IRubyObject backrefMatchLast(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_last(backref);
    }

    public static IRubyObject[] appendToObjectArray(IRubyObject[] array, IRubyObject add) {
        IRubyObject[] newArray = new IRubyObject[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = add;
        return newArray;
    }

    public static IRubyObject breakLocalJumpError(Ruby runtime, IRubyObject value) {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, value, "unexpected break");
    }

    public static IRubyObject[] concatObjectArrays(IRubyObject[] array, IRubyObject[] add) {
        return toArray(array, add);
    }

    public static IRubyObject[] toArray(IRubyObject[] array, IRubyObject... add) {
        IRubyObject[] newArray = new IRubyObject[array.length + add.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(add, 0, newArray, array.length, add.length);
        return newArray;
    }

    public static IRubyObject[] toArray(IRubyObject obj, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 1];
        newArray[0] = obj;
        System.arraycopy(rest, 0, newArray, 1, rest.length);
        return newArray;
    }

    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 2];
        newArray[0] = obj0;
        newArray[1] = obj1;
        System.arraycopy(rest, 0, newArray, 2, rest.length);
        return newArray;
    }

    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject obj2, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 3];
        newArray[0] = obj0;
        newArray[1] = obj1;
        newArray[2] = obj2;
        System.arraycopy(rest, 0, newArray, 3, rest.length);
        return newArray;
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject[] exceptions, ThreadContext context) {
        for (int i = 0; i < exceptions.length; i++) {
            IRubyObject result = isExceptionHandled(currentException, exceptions[i], context);
            if (result.isTrue()) return result;
        }
        return context.runtime.getFalse();
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception, ThreadContext context) {
        return isExceptionHandled((IRubyObject) currentException, exception, context);
    }

    public static IRubyObject isExceptionHandled(IRubyObject currentException, IRubyObject exception, ThreadContext context) {
        Ruby runtime = context.runtime;
        if (!runtime.getModule().isInstance(exception)) {
            throw runtime.newTypeError("class or module required for rescue clause");
        }
        IRubyObject result = invoke(context, exception, "===", currentException);
        if (result.isTrue()) return result;
        return runtime.getFalse();
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, context);
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, IRubyObject exception2, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, exception2, context);
    }

    public static boolean checkJavaException(Throwable throwable, IRubyObject catchable, ThreadContext context) {
        Ruby runtime = context.runtime;
        if (
                // rescue exception needs to catch Java exceptions
                runtime.getException() == catchable ||

                // rescue Object needs to catch Java exceptions
                runtime.getObject() == catchable ||

                // rescue StandardError needs to catch Java exceptions
                runtime.getStandardError() == catchable) {

            if (throwable instanceof RaiseException) {
                return isExceptionHandled(((RaiseException)throwable).getException(), catchable, context).isTrue();
            }

            // let Ruby exceptions decide if they handle it
            return isExceptionHandled(JavaUtil.convertJavaToUsableRubyObject(runtime, throwable), catchable, context).isTrue();

        } else if (runtime.getNativeException() == catchable) {
            // NativeException catches Java exceptions, lazily creating the wrapper
            return true;

        } else if (catchable instanceof RubyClass && catchable.getInstanceVariables().hasInstanceVariable("@java_class")) {
            RubyClass rubyClass = (RubyClass)catchable;
            JavaClass javaClass = (JavaClass)rubyClass.getInstanceVariable("@java_class");
            if (javaClass != null) {
                Class cls = javaClass.javaClass();
                if (cls.isInstance(throwable)) {
                    return true;
                }
            }

        } else if (catchable instanceof RubyModule) {
            IRubyObject exception = JavaUtil.convertJavaToUsableRubyObject(runtime, throwable);
            IRubyObject result = invoke(context, catchable, "===", exception);
            return result.isTrue();

        }

        return false;
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject[] throwables, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException) currentThrowable).getException(), throwables, context);
        } else {
            if (throwables.length == 0) {
                // no rescue means StandardError, which rescues Java exceptions
                return context.runtime.getTrue();
            } else {
                for (int i = 0; i < throwables.length; i++) {
                    if (checkJavaException(currentThrowable, throwables[i], context)) {
                        return context.runtime.getTrue();
                    }
                }
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException) currentThrowable).getException(), throwable, context);
        } else {
            if (checkJavaException(currentThrowable, throwable, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, IRubyObject throwable2, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, throwable2, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable2, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static void storeExceptionInErrorInfo(Throwable currentThrowable, ThreadContext context) {
        IRubyObject exception = null;
        if (currentThrowable instanceof RaiseException) {
            exception = ((RaiseException)currentThrowable).getException();
        } else {
            exception = JavaUtil.convertJavaToUsableRubyObject(context.runtime, currentThrowable);
        }
        context.setErrorInfo(exception);
    }

    public static void storeNativeExceptionInErrorInfo(Throwable currentThrowable, ThreadContext context) {
        IRubyObject exception = null;
        if (currentThrowable instanceof RaiseException) {
            exception = ((RaiseException)currentThrowable).getException();
        } else {
            Ruby runtime = context.runtime;

            // wrap Throwable in a NativeException object
            exception = new NativeException(runtime, runtime.getNativeException(), currentThrowable);
            ((NativeException)exception).prepareIntegratedBacktrace(context, currentThrowable.getStackTrace());
        }
        context.setErrorInfo(exception);
    }

    public static void clearErrorInfo(ThreadContext context) {
        context.setErrorInfo(context.runtime.getNil());
    }

    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context) {
        checkSuperDisabledOrOutOfMethod(context, context.getFrameKlazz(), context.getFrameName());
    }

    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule klass, String name) {
        if (klass == null) {
            if (name != null) {
                throw context.runtime.newNameError("superclass method '" + name + "' disabled", name);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.nil);
            }
        }
    }

    public static Block ensureSuperBlock(Block given, Block parent) {
        if (!given.isGiven()) {
            return parent;
        }
        return given;
    }

    public static RubyModule findImplementerIfNecessary(RubyModule clazz, RubyModule implementationClass) {
        if (implementationClass.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            return clazz.findImplementer(implementationClass);
        } else {
            // classes are directly in the hierarchy, so no special logic is necessary for implementer
            return implementationClass;
        }
    }

    public static RubyArray createSubarray(RubyArray input, int start) {
        return (RubyArray)input.subseqLight(start, input.size() - start);
    }

    public static RubyArray createSubarray(RubyArray input, int start, int post) {
        return (RubyArray)input.subseqLight(start, input.size() - post - start);
    }

    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start) {
        if (start >= input.length) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayNoCopy(runtime, input, start);
        }
    }

    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start, int exclude) {
        int length = input.length - exclude - start;
        if (length <= 0) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayNoCopy(runtime, input, start, length);
        }
    }

    public static IRubyObject elementOrNull(IRubyObject[] input, int element) {
        if (element >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject optElementOrNull(IRubyObject[] input, int element, int postCount) {
        if (element + postCount >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject elementOrNil(IRubyObject[] input, int element, IRubyObject nil) {
        if (element >= input.length) {
            return nil;
        } else {
            return input[element];
        }
    }

    public static IRubyObject postElementOrNil(IRubyObject[] input, int postCount, int postIndex, IRubyObject nil) {
        int aryIndex = input.length - postCount + postIndex;
        if (aryIndex >= input.length || aryIndex < 0) {
            return nil;
        } else {
            return input[aryIndex];
        }
    }

    public static RubyBoolean isWhenTriggered(IRubyObject expression, IRubyObject expressionsObject, ThreadContext context) {
        RubyArray expressions = Helpers.splatValue(expressionsObject);
        for (int j = 0,k = expressions.getLength(); j < k; j++) {
            IRubyObject condition = expressions.eltInternal(j);

            if ((expression != null && condition.callMethod(context, "===", expression).isTrue()) ||
                    (expression == null && condition.isTrue())) {
                return context.runtime.getTrue();
            }
        }

        return context.runtime.getFalse();
    }

    public static IRubyObject setConstantInModule(ThreadContext context, String name, IRubyObject value, IRubyObject module) {
        if (!(module instanceof RubyModule)) {
            throw context.runtime.newTypeError(module.toString() + " is not a class/module");
        }
        ((RubyModule) module).setConstant(name, value);

        return value;
    }

    public static final int MAX_SPECIFIC_ARITY_OBJECT_ARRAY = 10;

    public static IRubyObject[] anewarrayIRubyObjects(int size) {
        return new IRubyObject[size];
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, int start) {
        ary[start] = one;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, int start) {
        ary[start] = one;
        ary[start+1] = two;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        ary[start+9] = ten;
        return ary;
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one) {
        return new IRubyObject[] {one};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two) {
        return new IRubyObject[] {one, two};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three) {
        return new IRubyObject[] {one, two, three};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return new IRubyObject[] {one, two, three, four};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return new IRubyObject[] {one, two, three, four, five};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return new IRubyObject[] {one, two, three, four, five, six};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return new IRubyObject[] {one, two, three, four, five, six, seven};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one) {
        return RubyArray.newArrayLight(runtime, one);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two) {
        return RubyArray.newArrayLight(runtime, one, two);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three) {
        return RubyArray.newArrayLight(runtime, one, two, three);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return RubyArray.newArrayLight(runtime, one, two, three, four);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine, ten);
    }

    public static String[] constructStringArray(String one) {
        return new String[] {one};
    }

    public static String[] constructStringArray(String one, String two) {
        return new String[] {one, two};
    }

    public static String[] constructStringArray(String one, String two, String three) {
        return new String[] {one, two, three};
    }

    public static String[] constructStringArray(String one, String two, String three, String four) {
        return new String[] {one, two, three, four};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five) {
        return new String[] {one, two, three, four, five};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six) {
        return new String[] {one, two, three, four, five, six};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven) {
        return new String[] {one, two, three, four, five, six, seven};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight) {
        return new String[] {one, two, three, four, five, six, seven, eight};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine, String ten) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    public static final int MAX_SPECIFIC_ARITY_HASH = 5;

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                         IRubyObject key4, IRubyObject value4, boolean prepareString4) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        hash.fastASet(runtime, key4, value4, prepareString4);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                         IRubyObject key4, IRubyObject value4, boolean prepareString4,
                                         IRubyObject key5, IRubyObject value5, boolean prepareString5) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        hash.fastASet(runtime, key4, value4, prepareString4);
        hash.fastASet(runtime, key5, value5, prepareString5);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1, boolean prepareString1) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmall(runtime, key1, value1, prepareString1);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                              IRubyObject key2, IRubyObject value2, boolean prepareString2) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmall(runtime, key1, value1, prepareString1);
        hash.fastASetSmall(runtime, key2, value2, prepareString2);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                              IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                              IRubyObject key3, IRubyObject value3, boolean prepareString3) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmall(runtime, key1, value1, prepareString1);
        hash.fastASetSmall(runtime, key2, value2, prepareString2);
        hash.fastASetSmall(runtime, key3, value3, prepareString3);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                              IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                              IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                              IRubyObject key4, IRubyObject value4, boolean prepareString4) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmall(runtime, key1, value1, prepareString1);
        hash.fastASetSmall(runtime, key2, value2, prepareString2);
        hash.fastASetSmall(runtime, key3, value3, prepareString3);
        hash.fastASetSmall(runtime, key4, value4, prepareString4);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                              IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                              IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                              IRubyObject key4, IRubyObject value4, boolean prepareString4,
                                              IRubyObject key5, IRubyObject value5, boolean prepareString5) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmall(runtime, key1, value1, prepareString1);
        hash.fastASetSmall(runtime, key2, value2, prepareString2);
        hash.fastASetSmall(runtime, key3, value3, prepareString3);
        hash.fastASetSmall(runtime, key4, value4, prepareString4);
        hash.fastASetSmall(runtime, key5, value5, prepareString5);
        return hash;
    }

    public static IRubyObject undefMethod(ThreadContext context, Object nameArg) {
        RubyModule module = null; // context.getRubyClass();
        String name = (nameArg instanceof String) ?
            (String) nameArg : nameArg.toString();

        if (module == null) {
            throw context.runtime.newTypeError("No class to undef method '" + name + "'.");
        }

        module.undef(context, name);

        return context.runtime.getNil();
    }

    public static IRubyObject defineAlias(ThreadContext context, IRubyObject self, Object newNameArg, Object oldNameArg) {
        Ruby runtime = context.runtime;
        RubyModule module = null; // context.getRubyClass();

        if (module == null || self instanceof RubyFixnum || self instanceof RubySymbol){
            throw runtime.newTypeError("no class to make alias");
        }

        String newName = newNameArg.toString();
        String oldName = oldNameArg.toString();

        module.defineAlias(newName, oldName);
        module.callMethod(context, "method_added", runtime.newSymbol(newName));

        return runtime.getNil();
    }

    public static IRubyObject negate(IRubyObject value, Ruby runtime) {
        if (value.isTrue()) return runtime.getFalse();
        return runtime.getTrue();
    }

    public static IRubyObject stringOrNil(ByteList value, ThreadContext context) {
        if (value == null) return context.nil;
        return RubyString.newStringShared(context.runtime, value);
    }

    public static StaticScope preLoad(ThreadContext context, String[] varNames) {
        StaticScope staticScope = context.runtime.getStaticScopeFactory().newLocalScope(null, varNames);
        preLoadCommon(context, staticScope, false);

        return staticScope;
    }

    public static void preLoadCommon(ThreadContext context, StaticScope staticScope, boolean wrap) {
        if (wrap) {
            staticScope.setModule(RubyModule.newModule(context.runtime));
        } else {
            staticScope.setModule(context.runtime.getObject());
        }
        DynamicScope scope = DynamicScope.newDynamicScope(staticScope);

        // Each root node has a top-level scope that we need to push
        context.preScopedBody(scope);
        context.preNodeEval(context.runtime.getTopSelf());
    }

    public static void postLoad(ThreadContext context) {
        context.postNodeEval();
        context.postScopedBody();
    }

    public static void registerEndBlock(Block block, Ruby runtime) {
        runtime.pushExitBlock(runtime.newProc(Block.Type.LAMBDA, block));
    }

    public static IRubyObject match3(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match(context, value);
        } else {
            return value.callMethod(context, "=~", regexp);
        }
    }

    public static IRubyObject match3_19(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match19(context, value);
        } else {
            return value.callMethod(context, "=~", regexp);
        }
    }

    public static IRubyObject getErrorInfo(Ruby runtime) {
        return runtime.getGlobalVariables().get("$!");
    }

    public static void setErrorInfo(Ruby runtime, IRubyObject error) {
        runtime.getGlobalVariables().set("$!", error);
    }

    public static IRubyObject setLastLine(Ruby runtime, ThreadContext context, IRubyObject value) {
        return context.setLastLine(value);
    }

    public static IRubyObject getLastLine(Ruby runtime, ThreadContext context) {
        return context.getLastLine();
    }

    public static IRubyObject setBackref(Ruby runtime, ThreadContext context, IRubyObject value) {
        if (!value.isNil() && !(value instanceof RubyMatchData)) throw runtime.newTypeError(value, runtime.getMatchData());
        return context.setBackRef(value);
    }

    public static IRubyObject getBackref(Ruby runtime, ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        if (backref instanceof RubyMatchData) ((RubyMatchData)backref).use();
        return backref;
    }

    public static IRubyObject preOpAsgnWithOrAnd(IRubyObject receiver, ThreadContext context, IRubyObject self, CallSite varSite) {
        return varSite.call(context, self, receiver);
    }

    public static IRubyObject postOpAsgnWithOrAnd(IRubyObject receiver, IRubyObject value, ThreadContext context, IRubyObject self, CallSite varAsgnSite) {
        varAsgnSite.call(context, self, receiver, value);
        return value;
    }

    public static IRubyObject opAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, CallSite varSite, CallSite opSite, CallSite opAsgnSite) {
        IRubyObject var = varSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, arg);
        opAsgnSite.call(context, self, receiver, result);

        return result;
    }

    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, result);

        return result;
    }

    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, arg, result);

        return result;
    }

    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg1, IRubyObject arg2, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg1, arg2);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, arg1, arg2, result);

        return result;
    }

    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg1, arg2, arg3);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, new IRubyObject[] {arg1, arg2, arg3, result});

        return result;
    }

    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, appendToObjectArray(args, result));

        return result;
    }


    public static IRubyObject opElementAsgnWithOrPartTwoOneArg(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, arg, value);
        return value;
    }

    public static IRubyObject opElementAsgnWithOrPartTwoTwoArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, args[0], args[1], value);
        return value;
    }

    public static IRubyObject opElementAsgnWithOrPartTwoThreeArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, new IRubyObject[] {args[0], args[1], args[2], value});
        return value;
    }

    public static IRubyObject opElementAsgnWithOrPartTwoNArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = value;
        asetSite.call(context, self, receiver, newArgs);
        return value;
    }

    public static RubyArray arrayValue(IRubyObject value) {
        Ruby runtime = value.getRuntime();
        return arrayValue(runtime.getCurrentContext(), runtime, value);
    }

    public static RubyArray arrayValue(ThreadContext context, Ruby runtime, IRubyObject value) {
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
            // remove this hack too.

            if (value.respondsTo("to_a") && value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                IRubyObject avalue = value.callMethod(context, "to_a");
                if (!(avalue instanceof RubyArray)) {
                    if (avalue.isNil()) {
                        return runtime.newArray(value);
                    } else {
                        throw runtime.newTypeError("`to_a' did not return Array");
                    }
                }
                return (RubyArray)avalue;
            } else {
                DynamicMethod methodMissing = value.getMetaClass().searchMethod("method_missing");
                if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
                    return runtime.newArray(value);
                } else {
                    IRubyObject avalue = methodMissing.call(context, value, value.getMetaClass(), "to_a", new IRubyObject[] {runtime.newSymbol("to_a")}, Block.NULL_BLOCK);
                    if (!(avalue instanceof RubyArray)) {
                        if (avalue.isNil()) {
                            return runtime.newArray(value);
                        } else {
                            throw runtime.newTypeError("`to_a' did not return Array");
                        }
                    }
                    return (RubyArray)avalue;
                }
            }
        }
        RubyArray arr = (RubyArray) tmp;

        return arr.aryDup();
    }

    public static RubyArray asArray18(ThreadContext context, IRubyObject value) {
        Ruby runtime = context.runtime;
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
            // remove this hack too.

            if (value.respondsTo("to_a") && value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                IRubyObject avalue = value.callMethod(context, "to_a");
                if (!(avalue instanceof RubyArray)) {
                    if (avalue.isNil()) {
                        return runtime.newArray(value);
                    } else {
                        throw runtime.newTypeError("`to_a' did not return Array");
                    }
                }
                return (RubyArray)avalue;
            } else {
                return runtime.newArray(value);
            }
        }

        return (RubyArray) tmp;
    }

    // mri: rb_Array
    // FIXME: Replace arrayValue/asArray18 with this on 9k (currently dead -- respond_to? logic broken further down the line -- fix that first)
    public static RubyArray asArray(ThreadContext context, IRubyObject value) {
        return TypeConverter.rb_Array(context, value);
    }

    public static IRubyObject aryToAry(IRubyObject value) {
        if (value instanceof RubyArray) return value;

        if (value.respondsTo("to_ary")) {
            return TypeConverter.convertToType(value, value.getRuntime().getArray(), "to_ary", false);
        }

        return value.getRuntime().newArray(value);
    }

    public static IRubyObject aValueSplat(IRubyObject value) {
        if (!(value instanceof RubyArray) || ((RubyArray) value).length().getLongValue() == 0) {
            return value.getRuntime().getNil();
        }

        RubyArray array = (RubyArray) value;

        return array.getLength() == 1 ? array.first() : array;
    }

    public static IRubyObject aValueSplat19(IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            return value.getRuntime().getNil();
        }

        return (RubyArray) value;
    }

    public static RubyArray splatValue(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().newArray(value);
        }

        return arrayValue(value);
    }

    public static RubyArray splatValue19(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().newEmptyArray();
        }

        return arrayValue(value);
    }

    public static IRubyObject unsplatValue19(IRubyObject argsResult) {
        if (argsResult instanceof RubyArray) {
            RubyArray array = (RubyArray) argsResult;

            if (array.size() == 1) {
                IRubyObject newResult = array.eltInternal(0);
                if (!((newResult instanceof RubyArray) && ((RubyArray) newResult).size() == 0)) {
                    argsResult = newResult;
                }
            }
        }
        return argsResult;
    }

    public static IRubyObject[] splatToArguments(IRubyObject value) {
        Ruby runtime = value.getRuntime();

        if (value.isNil()) {
            return runtime.getSingleNilArray();
        }

        return splatToArgumentsCommon(runtime, value);
    }

    public static IRubyObject[] splatToArguments19(IRubyObject value) {
        Ruby runtime = value.getRuntime();

        if (value.isNil()) {
            return IRubyObject.NULL_ARRAY;
        }

        return splatToArgumentsCommon(runtime, value);
    }

    private static IRubyObject[] splatToArgumentsCommon(Ruby runtime, IRubyObject value) {

        if (value.isNil()) {
            return runtime.getSingleNilArray();
        }

        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            return convertSplatToJavaArray(runtime, value);
        }
        return ((RubyArray)tmp).toJavaArrayMaybeUnsafe();
    }

    private static IRubyObject[] convertSplatToJavaArray(Ruby runtime, IRubyObject value) {
        // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
        // remove this hack too.

        RubyClass metaClass = value.getMetaClass();
        DynamicMethod method = metaClass.searchMethod("to_a");
        if (method.isUndefined() || method.isImplementedBy(runtime.getKernel())) {
            return new IRubyObject[] {value};
        }

        IRubyObject avalue = method.call(runtime.getCurrentContext(), value, metaClass, "to_a");
        if (!(avalue instanceof RubyArray)) {
            if (avalue.isNil()) {
                return new IRubyObject[] {value};
            } else {
                throw runtime.newTypeError("`to_a' did not return Array");
            }
        }
        return ((RubyArray)avalue).toJavaArray();
    }

    public static IRubyObject[] argsCatToArguments(IRubyObject[] args, IRubyObject cat) {
        IRubyObject[] ary = splatToArguments(cat);
        return argsCatToArgumentsCommon(args, ary);
    }

    public static IRubyObject[] argsCatToArguments19(IRubyObject[] args, IRubyObject cat) {
        IRubyObject[] ary = splatToArguments19(cat);
        return argsCatToArgumentsCommon(args, ary);
    }

    private static IRubyObject[] argsCatToArgumentsCommon(IRubyObject[] args, IRubyObject[] ary) {
        if (ary.length > 0) {
            IRubyObject[] newArgs = new IRubyObject[args.length + ary.length];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            System.arraycopy(ary, 0, newArgs, args.length, ary.length);
            args = newArgs;
        }

        return args;
    }

    public static RubySymbol addInstanceMethod(RubyModule containingClass, String name, DynamicMethod method, Visibility visibility, ThreadContext context, Ruby runtime) {
        containingClass.addMethod(name, method);

        RubySymbol sym = runtime.fastNewSymbol(name);
        if (visibility == Visibility.MODULE_FUNCTION) {
            addModuleMethod(containingClass, name, method, context, sym);
        }

        if (!containingClass.isRefinement()) {
            callNormalMethodHook(containingClass, context, sym);
        }

        return sym;
    }

    private static void addModuleMethod(RubyModule containingClass, String name, DynamicMethod method, ThreadContext context, RubySymbol sym) {
        containingClass.getSingletonClass().addMethod(name, new WrapperMethod(containingClass.getSingletonClass(), method, Visibility.PUBLIC));
        containingClass.callMethod(context, "singleton_method_added", sym);
    }

    private static void callNormalMethodHook(RubyModule containingClass, ThreadContext context, RubySymbol name) {
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            callSingletonMethodHook(((MetaClass) containingClass).getAttached(), context, name);
        } else {
            containingClass.callMethod(context, "method_added", name);
        }
    }

    private static void callSingletonMethodHook(IRubyObject receiver, ThreadContext context, RubySymbol name) {
        receiver.callMethod(context, "singleton_method_added", name);
    }

    private static DynamicMethod constructNormalMethod(
            MethodFactory factory,
            String javaName,
            String name,
            RubyModule containingClass,
            ISourcePosition position,
            int arity,
            StaticScope scope,
            Visibility visibility,
            Object scriptObject,
            CallConfiguration callConfig,
            String parameterDesc,
            MethodNodes methodNodes) {

        DynamicMethod method;
        final Ruby runtime = containingClass.getRuntime();

        if (name.equals("initialize") || name.equals("initialize_copy") || name.equals("initialize_clone") || name.equals("initialize_dup") || name.equals("respond_to_missing?") || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }

        if (RubyInstanceConfig.LAZYHANDLES_COMPILE) {
            method = factory.getCompiledMethodLazily(
                    containingClass,
                    name,
                    javaName,
                    visibility,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc,
                    methodNodes);
        } else {
            method = factory.getCompiledMethod(
                    containingClass,
                    name,
                    javaName,
                    visibility,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc,
                    methodNodes);
        }

        return method;
    }

    private static DynamicMethod constructSingletonMethod(
            MethodFactory factory,
            String rubyName,
            String javaName,
            RubyClass rubyClass,
            ISourcePosition position,
            StaticScope scope,
            Object scriptObject,
            CallConfiguration callConfig,
            String parameterDesc,
            MethodNodes methodNodes) {

        if (RubyInstanceConfig.LAZYHANDLES_COMPILE) {
            return factory.getCompiledMethodLazily(
                    rubyClass,
                    rubyName,
                    javaName,
                    Visibility.PUBLIC,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc,
                    methodNodes);
        } else {
            return factory.getCompiledMethod(
                    rubyClass,
                    rubyName,
                    javaName,
                    Visibility.PUBLIC,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc,
                    methodNodes);
        }
    }

    public static String encodeScope(StaticScope scope) {
        StringBuilder namesBuilder = new StringBuilder(scope.getType().name()); // 0

        namesBuilder.append(',');  // 1

        boolean first = true;
        for (String name : scope.getVariables()) {
            if (!first) namesBuilder.append(';');
            first = false;
            namesBuilder.append(name);
        }
        namesBuilder.append(',').append(scope.getSignature().encode()); // 2
        namesBuilder.append(',').append(scope.getScopeType());          // 3

        return namesBuilder.toString();
    }

    public static StaticScope decodeScope(ThreadContext context, StaticScope parent, String scopeString) {
        String[][] decodedScope = decodeScopeDescriptor(scopeString);
        String scopeTypeName = decodedScope[0][0];
        String[] names = decodedScope[1];
        StaticScope scope = null;
        switch (StaticScope.Type.valueOf(scopeTypeName)) {
            case BLOCK:
                scope = context.runtime.getStaticScopeFactory().newBlockScope(parent, names);
                break;
            case EVAL:
                scope = context.runtime.getStaticScopeFactory().newEvalScope(parent, names);
                break;
            case LOCAL:
                scope = context.runtime.getStaticScopeFactory().newLocalScope(parent, names);
                break;
        }
        setAritiesFromDecodedScope(scope, decodedScope[0][2]);
        scope.setScopeType(IRScopeType.valueOf(decodedScope[0][3]));
        return scope;
    }

    private static String[][] decodeScopeDescriptor(String scopeString) {
        String[] scopeElements = scopeString.split(",");
        String[] scopeNames = scopeElements[1].length() == 0 ? new String[0] : getScopeNames(scopeElements[1]);
        return new String[][] {scopeElements, scopeNames};
    }

    private static void setAritiesFromDecodedScope(StaticScope scope, String encodedSignature) {
        scope.setSignature(Signature.decode(Long.parseLong(encodedSignature)));
    }

    public static StaticScope decodeScopeAndDetermineModule(ThreadContext context, StaticScope parent, String scopeString) {
        StaticScope scope = decodeScope(context, parent, scopeString);
        scope.determineModule();

        return scope;
    }

    public static Visibility performNormalMethodChecksAndDetermineVisibility(Ruby runtime, RubyModule clazz, String name, Visibility visibility) throws RaiseException {
        if (clazz == runtime.getDummy()) {
            throw runtime.newTypeError("no class/module to add method");
        }

        if (clazz == runtime.getObject() && "initialize".equals(name)) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
        }

        if ("__id__".equals(name) || "__send__".equals(name)) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining `" + name + "' may cause serious problem");
        }

        if ("initialize".equals(name) || "initialize_copy".equals(name) || name.equals("initialize_dup") || name.equals("initialize_clone") || name.equals("respond_to_missing?") || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }

        return visibility;
    }

    public static RubyClass performSingletonMethodChecks(Ruby runtime, IRubyObject receiver, String name) throws RaiseException {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + name + "\" for " + receiver.getMetaClass().getBaseName());
        }

        if (receiver.isFrozen()) {
            throw runtime.newFrozenError("object");
        }

        RubyClass rubyClass = receiver.getSingletonClass();

        return rubyClass;
    }

    public static IRubyObject arrayEntryOrNil(RubyArray array, int index) {
        if (index < array.getLength()) {
            return array.eltInternal(index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilZero(RubyArray array) {
        if (0 < array.getLength()) {
            return array.eltInternal(0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilOne(RubyArray array) {
        if (1 < array.getLength()) {
            return array.eltInternal(1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilTwo(RubyArray array) {
        if (2 < array.getLength()) {
            return array.eltInternal(2);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNil(RubyArray array, int pre, int post, int index) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + index);
        } else if (pre + index < array.getLength()) {
            return array.eltInternal(pre + index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilZero(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 0);
        } else if (pre + 0 < array.getLength()) {
            return array.eltInternal(pre + 0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilOne(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 1);
        } else if (pre + 1 < array.getLength()) {
            return array.eltInternal(pre + 1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilTwo(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 2);
        } else if (pre + 2 < array.getLength()) {
            return array.eltInternal(pre + 2);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index) {
        if (index < array.getLength()) {
            return createSubarray(array, index);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }

    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index, int post) {
        if (index + post < array.getLength()) {
            return createSubarray(array, index, post);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }

    public static RubyModule checkIsModule(IRubyObject maybeModule) {
        if (maybeModule instanceof RubyModule) return (RubyModule)maybeModule;

        throw maybeModule.getRuntime().newTypeError(maybeModule + " is not a class/module");
    }

    public static IRubyObject getGlobalVariable(Ruby runtime, String name) {
        return runtime.getGlobalVariables().get(name);
    }

    public static IRubyObject setGlobalVariable(IRubyObject value, Ruby runtime, String name) {
        return runtime.getGlobalVariables().set(name, value);
    }

    public static IRubyObject getInstanceVariable(IRubyObject self, Ruby runtime, String internedName) {
        IRubyObject result = self.getInstanceVariables().getInstanceVariable(internedName);
        if (result != null) return result;
        if (runtime.isVerbose()) warnAboutUninitializedIvar(runtime, internedName);
        return runtime.getNil();
    }

    public static IRubyObject getInstanceVariableNoWarn(IRubyObject self, ThreadContext context, String internedName) {
        IRubyObject result = self.getInstanceVariables().getInstanceVariable(internedName);
        if (result != null) return result;
        return context.nil;
    }

    private static void warnAboutUninitializedIvar(Ruby runtime, String internedName) {
        runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, "instance variable " + internedName + " not initialized");
    }

    public static IRubyObject setInstanceVariable(IRubyObject value, IRubyObject self, String name) {
        return self.getInstanceVariables().setInstanceVariable(name, value);
    }

    public static RubyProc newLiteralLambda(ThreadContext context, Block block, IRubyObject self) {
        return RubyProc.newProc(context.runtime, block, Block.Type.LAMBDA);
    }

    public static void fillNil(IRubyObject[]arr, int from, int to, Ruby runtime) {
        if (arr.length == 0) return;
        IRubyObject nils[] = runtime.getNilPrefilledArray();
        int i;

        for (i = from; i + Ruby.NIL_PREFILLED_ARRAY_SIZE < to; i += Ruby.NIL_PREFILLED_ARRAY_SIZE) {
            System.arraycopy(nils, 0, arr, i, Ruby.NIL_PREFILLED_ARRAY_SIZE);
        }
        System.arraycopy(nils, 0, arr, i, to - i);
    }

    public static void fillNil(IRubyObject[]arr, Ruby runtime) {
        if (arr.length == 0) return;
        fillNil(arr, 0, arr.length, runtime);
    }

    public static boolean isFastSwitchableString(IRubyObject str) {
        return str instanceof RubyString;
    }

    public static boolean isFastSwitchableSingleCharString(IRubyObject str) {
        return str instanceof RubyString && ((RubyString)str).getByteList().length() == 1;
    }

    public static int getFastSwitchString(IRubyObject str) {
        ByteList byteList = ((RubyString)str).getByteList();
        return byteList.hashCode();
    }

    public static int getFastSwitchSingleCharString(IRubyObject str) {
        ByteList byteList = ((RubyString)str).getByteList();
        return byteList.get(0);
    }

    public static boolean isFastSwitchableSymbol(IRubyObject sym) {
        return sym instanceof RubySymbol;
    }

    public static boolean isFastSwitchableSingleCharSymbol(IRubyObject sym) {
        return sym instanceof RubySymbol && ((RubySymbol)sym).asJavaString().length() == 1;
    }

    public static int getFastSwitchSymbol(IRubyObject sym) {
        String str = ((RubySymbol)sym).asJavaString();
        return str.hashCode();
    }

    public static int getFastSwitchSingleCharSymbol(IRubyObject sym) {
        String str = ((RubySymbol)sym).asJavaString();
        return (int)str.charAt(0);
    }

    public static Block getBlock(ThreadContext context, IRubyObject self, Node node) {
        throw new RuntimeException("Should not be called");
    }

    public static Block getBlock(Ruby runtime, ThreadContext context, IRubyObject self, Node node, Block aBlock) {
        throw new RuntimeException("Should not be called");
    }

    /**
     * Equivalent to rb_equal in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEqual(ThreadContext context, IRubyObject a, IRubyObject b) {
        Ruby runtime = context.runtime;
        if (a == b) return runtime.getTrue();
        IRubyObject res = invokedynamic(context, a, OP_EQUAL, b);
        return runtime.newBoolean(res.isTrue());
    }

    /**
     * Equivalent to rb_eql in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEql(ThreadContext context, IRubyObject a, IRubyObject b) {
        Ruby runtime = context.runtime;
        if (a == b) return runtime.getTrue();
        IRubyObject res = invokedynamic(context, a, EQL, b);
        return runtime.newBoolean(res.isTrue());
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param min minimum required
     * @param max maximum allowed
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int min, int max) {
        checkArgumentCount(context, args.length, min, max);
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param req required number
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int req) {
        checkArgumentCount(context, args.length, req, req);
    }

    public static void checkArgumentCount(ThreadContext context, int length, int min, int max) {
        int expected = 0;
        if (length < min) {
            expected = min;
        } else if (max > -1 && length > max) {
            expected = max;
        } else {
            return;
        }
        throw context.runtime.newArgumentError(length, expected);
    }

    public static boolean isModuleAndHasConstant(IRubyObject left, String name) {
        return left instanceof RubyModule && ((RubyModule) left).getConstantFromNoConstMissing(name, false) != null;
    }

    public static RubyString getDefinedConstantOrBoundMethod(IRubyObject left, String name) {
        if (isModuleAndHasConstant(left, name)) return left.getRuntime().getDefinedMessage(DefinedMessage.CONSTANT);
        if (left.getMetaClass().isMethodBound(name, true)) left.getRuntime().getDefinedMessage(DefinedMessage.METHOD);
        return null;
    }

    public static RubyModule getSuperClassForDefined(Ruby runtime, RubyModule klazz) {
        RubyModule superklazz = klazz.getSuperClass();

        if (superklazz == null && klazz.isModule()) superklazz = runtime.getObject();

        return superklazz;
    }

    public static boolean isGenerationEqual(IRubyObject object, int generation) {
        RubyClass metaClass;
        if (object instanceof RubyBasicObject) {
            metaClass = ((RubyBasicObject)object).getMetaClass();
        } else {
            metaClass = object.getMetaClass();
        }
        return metaClass.getGeneration() == generation;
    }

    public static String[] getScopeNames(String scopeNames) {
        StringTokenizer toker = new StringTokenizer(scopeNames, ";");
        ArrayList list = new ArrayList(10);
        while (toker.hasMoreTokens()) {
            list.add(toker.nextToken().intern());
        }
        return (String[])list.toArray(new String[list.size()]);
    }

    public static IRubyObject[] arraySlice1N(IRubyObject arrayish) {
        arrayish = aryToAry(arrayish);
        RubyArray arrayish2 = ensureMultipleAssignableRubyArray(arrayish, arrayish.getRuntime(), true);
        return new IRubyObject[] {arrayEntryOrNilZero(arrayish2), subarrayOrEmpty(arrayish2, arrayish2.getRuntime(), 1)};
    }

    public static IRubyObject arraySlice1(IRubyObject arrayish) {
        arrayish = aryToAry(arrayish);
        RubyArray arrayish2 = ensureMultipleAssignableRubyArray(arrayish, arrayish.getRuntime(), true);
        return arrayEntryOrNilZero(arrayish2);
    }

    public static RubyClass metaclass(IRubyObject object) {
        return object instanceof RubyBasicObject ?
            ((RubyBasicObject)object).getMetaClass() :
            object.getMetaClass();
    }

    public static String rawBytesToString(byte[] bytes) {
        // stuff bytes into chars
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) chars[i] = (char)bytes[i];
        return new String(chars);
    }

    public static byte[] stringToRawBytes(String string) {
        char[] chars = string.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) bytes[i] = (byte)chars[i];
        return bytes;
    }

    public static String encodeCaptureOffsets(int[] scopeOffsets) {
        char[] encoded = new char[scopeOffsets.length * 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            int offDepth = scopeOffsets[i];
            char off = (char)(offDepth & 0xFFFF);
            char depth = (char)(offDepth >> 16);
            encoded[2 * i] = off;
            encoded[2 * i + 1] = depth;
        }
        return new String(encoded);
    }

    public static int[] decodeCaptureOffsets(String encoded) {
        char[] chars = encoded.toCharArray();
        int[] scopeOffsets = new int[chars.length / 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            char off = chars[2 * i];
            char depth = chars[2 * i + 1];
            scopeOffsets[i] = (((int)depth) << 16) | (int)off;
        }
        return scopeOffsets;
    }

    public static IRubyObject match2AndUpdateScope(IRubyObject receiver, ThreadContext context, IRubyObject value, String scopeOffsets) {
        IRubyObject match = ((RubyRegexp)receiver).op_match(context, value);
        updateScopeWithCaptures(context, decodeCaptureOffsets(scopeOffsets), match);
        return match;
    }

    public static IRubyObject match2AndUpdateScope19(IRubyObject receiver, ThreadContext context, IRubyObject value, String scopeOffsets) {
        IRubyObject match = ((RubyRegexp)receiver).op_match19(context, value);
        updateScopeWithCaptures(context, decodeCaptureOffsets(scopeOffsets), match);
        return match;
    }

    public static void updateScopeWithCaptures(ThreadContext context, int[] scopeOffsets, IRubyObject result) {
        Ruby runtime = context.runtime;
        if (result.isNil()) { // match2 directly calls match so we know we can count on result
            IRubyObject nil = runtime.getNil();

            for (int i = 0; i < scopeOffsets.length; i++) {
                // SSS FIXME: This is not doing the offset/depth extraction as in the else case
                context.getCurrentScope().setValue(nil, scopeOffsets[i], 0);
            }
        } else {
            RubyMatchData matchData = (RubyMatchData)context.getBackRef();
            // FIXME: Mass assignment is possible since we know they are all locals in the same
            //   scope that are also contiguous
            IRubyObject[] namedValues = matchData.getNamedBackrefValues(runtime);

            for (int i = 0; i < scopeOffsets.length; i++) {
                context.getCurrentScope().setValue(namedValues[i], scopeOffsets[i] & 0xffff, scopeOffsets[i] >> 16);
            }
        }
    }

    public static RubyArray argsPush(RubyArray first, IRubyObject second) {
        return ((RubyArray)first.dup()).append(second);
    }

    public static RubyArray argsCat(IRubyObject first, IRubyObject second) {
        Ruby runtime = first.getRuntime();
        IRubyObject secondArgs;
        secondArgs = Helpers.splatValue19(second);

        return ((RubyArray) Helpers.ensureRubyArray(runtime, first).dup()).concat(secondArgs);
    }

    /** Use an ArgsNode (used for blocks) to generate ArgumentDescriptors */
    public static ArgumentDescriptor[] argsNodeToArgumentDescriptors(ArgsNode argsNode) {
        ArrayList<ArgumentDescriptor> descs = new ArrayList<>();

        if (argsNode.getPre() != null) {
            for (Node preNode : argsNode.getPre().childNodes()) {
                if (preNode instanceof MultipleAsgnNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentNode) preNode).getName()));
                }
            }
        }

        if (argsNode.getOptArgs() != null) {
            for (Node optNode : argsNode.getOptArgs().childNodes()) {
                ArgumentType type = ArgumentType.opt;
                String name = null;
                if (optNode instanceof OptArgNode) {
                    name = ((OptArgNode)optNode).getName();
                } else if (optNode instanceof LocalAsgnNode) {
                    name = ((LocalAsgnNode)optNode).getName();
                } else if (optNode instanceof DAsgnNode) {
                    name = ((DAsgnNode)optNode).getName();
                } else {
                    type = ArgumentType.anonopt;
                }
                descs.add(new ArgumentDescriptor(type, name));
            }
        }

        ArgumentNode restArg = argsNode.getRestArgNode();
        if (restArg != null) {
            if (restArg instanceof UnnamedRestArgNode) {
                if (((UnnamedRestArgNode) restArg).isStar()) descs.add(new ArgumentDescriptor(ArgumentType.anonrest));
            } else {
                descs.add(new ArgumentDescriptor(ArgumentType.rest, restArg.getName()));
            }
        }

        if (argsNode.getPost() != null) {
            for (Node postNode : argsNode.getPost().childNodes()) {
                if (postNode instanceof MultipleAsgnNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentNode)postNode).getName()));
                }
            }
        }

        if (argsNode.getKeywords() != null) {
            for (Node keyWordNode : argsNode.getKeywords().childNodes()) {
                for (Node asgnNode : keyWordNode.childNodes()) {
                    if (isRequiredKeywordArgumentValueNode(asgnNode)) {
                        descs.add(new ArgumentDescriptor(ArgumentType.keyreq, ((INameNode) asgnNode).getName()));
                    } else {
                        descs.add(new ArgumentDescriptor(ArgumentType.key, ((INameNode) asgnNode).getName()));
                    }
                }
            }
        }

        if (argsNode.getKeyRest() != null) {
            String argName = argsNode.getKeyRest().getName();
            if (argName == null || argName.length() == 0) {
                descs.add(new ArgumentDescriptor(ArgumentType.anonkeyrest, argName));
            } else {
                descs.add(new ArgumentDescriptor(ArgumentType.keyrest, argsNode.getKeyRest().getName()));
            }
        }
        if (argsNode.getBlock() != null) descs.add(new ArgumentDescriptor(ArgumentType.block, argsNode.getBlock().getName()));

        return descs.toArray(new ArgumentDescriptor[descs.size()]);
    }

    /** Convert a parameter list from prefix format to ArgumentDescriptor format */
    public static ArgumentDescriptor[] parameterListToArgumentDescriptors(String[] parameterList, boolean isLambda) {
        ArgumentDescriptor[] parms = new ArgumentDescriptor[parameterList.length];

        for (int i = 0; i < parameterList.length; i++) {
            String param = parameterList[i];

            if (param.equals("NONE")) break;
            if (param.equals("nil")) param = "n"; // make length 1 so we don't look for a name

            ArgumentType type = ArgumentType.valueOf(param.charAt(0));

            // for lambdas, we call required args optional
            if (type == ArgumentType.req && !isLambda) type = ArgumentType.opt;

            // 'R', 'o', 'n' forms can get here without a name
            if (param.length() > 1) {
                parms[i] = new ArgumentDescriptor(type, param.substring(1));
            } else {
                parms[i] = new ArgumentDescriptor(type);
            }
        }

        return parms;
    }

    /** Convert a parameter list from ArgumentDescriptor format to "Array of Array" format */
    public static RubyArray argumentDescriptorsToParameters(Ruby runtime, ArgumentDescriptor[] argsDesc, boolean isLambda) {
        if (argsDesc == null) Thread.dumpStack();

        RubyArray parms = RubyArray.newArray(runtime, argsDesc.length);

        for (ArgumentDescriptor param : argsDesc) parms.add(param.toArrayForm(runtime, isLambda));

        return parms;
    }

    public static ArgumentDescriptor[] methodToArgumentDescriptors(DynamicMethod method) {
        method = method.getRealMethod();

        if (method instanceof MethodArgs2) {
            return parameterListToArgumentDescriptors(((MethodArgs2) method).getParameterList(), true);
        } else if (method instanceof IRMethodArgs) {
            return ((IRMethodArgs) method).getArgumentDescriptors();
        } else {
            return new ArgumentDescriptor[]{new ArgumentDescriptor(ArgumentType.rest)};
        }
    }

    public static IRubyObject methodToParameters(Ruby runtime, AbstractRubyMethod recv) {
        DynamicMethod method = recv.getMethod().getRealMethod();

        return argumentDescriptorsToParameters(runtime, methodToArgumentDescriptors(method), true);
    }

    public static RubyString getDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name) {
        RubyClass metaClass = receiver.getMetaClass();
        DynamicMethod method = metaClass.searchMethod(name);
        Visibility visibility = method.getVisibility();

        if (visibility != Visibility.PRIVATE &&
                (visibility != Visibility.PROTECTED || metaClass.getRealClass().isInstance(self)) && !method.isUndefined()) {
            return context.runtime.getDefinedMessage(DefinedMessage.METHOD);
        }

        if (receiver.callMethod(context, "respond_to_missing?",
            new IRubyObject[]{context.runtime.newSymbol(name), context.runtime.getFalse()}).isTrue()) {
            return context.runtime.getDefinedMessage(DefinedMessage.METHOD);
        }
        return null;
    }

    public static RubyString getDefinedNot(Ruby runtime, RubyString definition) {
        if (definition != null) {
            definition = runtime.getDefinedMessage(DefinedMessage.METHOD);
        }

        return definition;
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        return getMethodCached(context, metaclass, method.ordinal(), name).call(context, self, metaclass, name);
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method, IRubyObject arg0) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        return getMethodCached(context, metaclass, method.ordinal(), name).call(context, self, metaclass, name, arg0);
    }

    private static DynamicMethod getMethodCached(ThreadContext context, RubyClass metaclass, int index, String name) {
        if (metaclass.getClassIndex() == ClassIndex.NO_INDEX) return metaclass.searchMethod(name);
        return context.runtimeCache.getMethod(context, metaclass, metaclass.getClassIndex().ordinal() * (index + 1), name);
    }

    public static IRubyObject lastElement(IRubyObject[] ary) {
        return ary[ary.length - 1];
    }

    public static RubyString appendAsString(RubyString target, IRubyObject other) {
        return target.append(other.asString());
    }

    public static RubyString appendAsString19(RubyString target, IRubyObject other) {
        return target.append19(other.asString());
    }

    /**
     * We need to splat incoming array to a block when |a, *b| (any required +
     * rest) or |a, b| (>1 required).
     */
    public static boolean needsSplat19(int requiredCount, boolean isRest) {
        return (isRest && requiredCount > 0) || (!isRest && requiredCount > 1);
    }

    // . Array given to rest should pass itself
    // . Array with rest + other args should extract array
    // . Array with multiple values and NO rest should extract args if there are more than one argument
    // Note: In 1.9 alreadyArray is only relevent from our internal Java code in core libs.  We never use it
    // from interpreter or JIT.  FIXME: Change core lib consumers to stop using alreadyArray param.
    public static IRubyObject[] restructureBlockArgs19(IRubyObject value, Signature signature, Block.Type type, boolean needsSplat, boolean alreadyArray) {
        if (!type.checkArity && signature == Signature.NO_ARGUMENTS) return IRubyObject.NULL_ARRAY;

        if (value != null && !(value instanceof RubyArray) && needsSplat) value = Helpers.aryToAry(value);

        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && (alreadyArray || needsSplat)) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }

        return parameters;
    }

    public static boolean BEQ(ThreadContext context, IRubyObject value1, IRubyObject value2) {
        return value1.op_equal(context, value2).isTrue();
    }

    public static boolean BNE(ThreadContext context, IRubyObject value1, IRubyObject value2) {
        boolean eql = value2 == context.nil || value2 == UndefinedValue.UNDEFINED ?
                value1 == value2 : value1.op_equal(context, value2).isTrue();

        return !eql;
    }

    public static RubyModule checkIsRubyModule(ThreadContext context, Object object) {
        if (!(object instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        return (RubyModule)object;
    }

    public static RubyClass newClassForIR(ThreadContext context, String name, IRubyObject self, RubyModule classContainer, Object superClass, boolean meta) {
        if (meta) return classContainer.getMetaClass();

        RubyClass sc = null;

        if (superClass == UndefinedValue.UNDEFINED) {
            sc = null;
        } else if (superClass != null) {
            RubyClass.checkInheritable((IRubyObject)superClass);

            sc = (RubyClass) superClass;
        }


        return classContainer.defineOrGetClassUnder(name, sc);
    }

    public static RubyString appendByteList(RubyString target, ByteList source) {
        target.getByteList().append(source);
        return target;
    }

    public static RubyString appendByteList19(RubyString target, ByteList source, int codeRange) {
        target.cat19(source, codeRange);
        return target;
    }

    public static RubyString shortcutAppend18(RubyString string, IRubyObject object) {
        if (object instanceof RubyFixnum || object instanceof RubyFloat || object instanceof RubySymbol) {
            return string.append(object);
        } else {
            return string.append(object.asString());
        }
    }

    public static RubyString shortcutAppend(RubyString string, IRubyObject object) {
        if (object instanceof RubyFixnum || object instanceof RubyFloat || object instanceof RubySymbol) {
            return string.append19(object);
        } else {
            return string.append19(object.asString());
        }
    }

    public static void irCheckArgsArrayArity(ThreadContext context, RubyArray args, int required, int opt, boolean rest) {
        int numArgs = args.size();
        if (numArgs < required || (!rest && numArgs > (required + opt))) {
            Arity.raiseArgumentError(context.runtime, numArgs, required, required + opt);
        }
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index) {
        return invokedynamic(context, self, MethodNames.values()[index]);
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index, IRubyObject arg0) {
        return invokedynamic(context, self, MethodNames.values()[index], arg0);
    }

    public static void throwException(final Throwable e) {
        Helpers.<RuntimeException>throwsUnchecked(e);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwsUnchecked(final Throwable e) throws T {
        throw (T) e;
    }

    /**
     * Decode the given value to a Java string using the following rules:
     *
     * * If the string is all US-ASCII characters, it will be decoded as US-ASCII.
     * * If the string is a unicode encoding, it will be decoded as such.
     * * If the string is any other encoding, it will be encoded as raw bytes
     *   using ISO-8859-1.
     *
     * This allows non-unicode, non-US-ASCII encodings to be represented in the
     * symbol table as their raw versions, but properly decodes unicode-
     * encoded strings.
     *
     * @param value the value to decode
     * @return the resulting symbol string
     */
    public static String symbolBytesToString(ByteList value) {
        Encoding encoding = value.getEncoding();
        if (encoding == USASCIIEncoding.INSTANCE || encoding == ASCIIEncoding.INSTANCE) {
            return value.toString(); // raw
        } else if (encoding instanceof UnicodeEncoding) {
            return new String(value.getUnsafeBytes(), value.getBegin(), value.getRealSize(), value.getEncoding().getCharset());
        } else {
            return value.toString(); // raw
        }
    }

    /**
     * Decode a given ByteList to a Java string.
     *
     * @param runtime the current runtime
     * @param value the bytelist
     * @return a Java String representation of the ByteList
     */
    public static String decodeByteList(Ruby runtime, ByteList value) {
        byte[] unsafeBytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int length = value.length();

        Encoding encoding = value.getEncoding();

        if (encoding == UTF8Encoding.INSTANCE) {
                return RubyEncoding.decodeUTF8(unsafeBytes, begin, length);
            }

            Charset charset = runtime.getEncodingService().charsetForEncoding(encoding);

            if (charset == null) {
                try {
                    return new String(unsafeBytes, begin, length, encoding.toString());
                } catch (UnsupportedEncodingException uee) {
                    return value.toString();
                }
            }

            return RubyEncoding.decode(unsafeBytes, begin, length, charset);
        }

    /**
     * Convert a ByteList into a Java String by using its Encoding's Charset. If
     * the Charset is not available, fall back on other logic.
     *
     * @param bytes the bytelist to decode
     * @return the decoded string
     */
    public static String byteListToString(final ByteList bytes) {
        final Charset charset = bytes.getEncoding().getCharset();
        if ( charset != null ) {
            return new String(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize(), charset);
        }
        return bytes.toString();
    }

    public static IRubyObject rewriteStackTraceAndThrow(ThreadContext context, Throwable t) {
        Ruby runtime = context.runtime;

        StackTraceElement[] javaTrace = t.getStackTrace();
        BacktraceData backtraceData = runtime.getInstanceConfig().getTraceType().getIntegratedBacktrace(context, javaTrace);
        t.setStackTrace(RaiseException.javaTraceFromRubyTrace(backtraceData.getBacktrace(runtime)));
        throwException(t);
        return null; // not reached
    }

    public static void rewriteStackTrace(final Ruby runtime, final Throwable e) {
        final StackTraceElement[] javaTrace = e.getStackTrace();
        BacktraceData backtraceData = runtime.getInstanceConfig().getTraceType().getIntegratedBacktrace(runtime.getCurrentContext(), javaTrace);
        e.setStackTrace(RaiseException.javaTraceFromRubyTrace(backtraceData.getBacktrace(runtime)));
    }

    public static String stringJoin(String delimiter, String[] strings) {
        if (strings.length == 0) return "";
        StringBuilder sb = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            sb.append(delimiter)
                    .append(strings[i]);
        }
        return sb.toString();
    }

    public static <T> T[] arrayOf(T... values) {
        return values;
    }

    public static <T> T[] arrayOf(Class<T> t, int size, T fill) {
        T[] ary = (T[])Array.newInstance(t, size);
        Arrays.fill(ary, fill);
        return ary;
    }

    public static int memchr(boolean[] ary, int start, int len, boolean find) {
        for (int i = 0; i < len; i++) {
            if (ary[i + start] == find) return i + start;
        }
        return -1;
    }

    private static boolean isRequiredKeywordArgumentValueNode(Node asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueNode;
    }

    @Deprecated
    public static String encodeParameterList(List<String[]> args) {
        if (args.size() == 0) return "NONE";

        StringBuilder builder = new StringBuilder();

        boolean added = false;
        for (String[] desc : args) {
            if (added) builder.append(';');
            builder.append(desc[0]).append(desc[1]);
            added = true;
        }

        return builder.toString();
    }

}
