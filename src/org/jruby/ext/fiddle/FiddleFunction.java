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
 * Copyright (C) 2011 Charles O Nutter <headius@headius.com>
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
package org.jruby.ext.fiddle;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallContextCache;
import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.Invoker;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.ext.ffi.jffi.DefaultMethodFactory;
import org.jruby.ext.ffi.jffi.MethodFactory;
import org.jruby.ext.ffi.jffi.NativeFunctionInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.platform.Platform;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

public class FiddleFunction extends RubyObject {
    public static void initFiddleFunction(Ruby runtime, RubyModule fiddle) {
        RubyClass fiddleFunction = runtime.defineClassUnder("Function", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new FiddleFunction(runtime, klazz);
            }
        }, fiddle);

        fiddleFunction.defineAnnotatedMethods(FiddleFunction.class);
        fiddleFunction.defineAnnotatedConstants(FiddleFunction.class);
    }

    public FiddleFunction(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyConstant
    public static final int DEFAULT = CallingConvention.DEFAULT.ordinal();
    @JRubyConstant
    public static final int STDCALL = CallingConvention.STDCALL.ordinal();

    @JRubyMethod(visibility = PRIVATE, required = 3, optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] ffiArgs) {
        this.ptr = ffiArgs[0].convertToInteger("to_i").getLongValue();

        if (ffiArgs[1] instanceof RubyArray) {
            RubyArray args = (RubyArray)ffiArgs[1].checkArrayType();
            argTypes= new Type[args.size()];
            for (int i = 0; i < argTypes.length; i++) {
                argTypes[i] = typeFromInt(context.runtime, (int)args.eltInternal(i).convertToInteger().getLongValue());
            }
        } else if (ffiArgs[1].isNil()) {
            argTypes = new Type[0];
        } else {
            throw context.runtime.newTypeError(ffiArgs[1], context.runtime.getArray());
        }

        this.retType = typeFromInt(context.runtime, (int)ffiArgs[2].convertToInteger().getLongValue());
        if (ffiArgs.length == 1) {
            this.abi = ffiArgs[3].isNil() ? CallingConvention.DEFAULT : CallingConvention.values()[(int)ffiArgs[3].convertToInteger().getLongValue()];
        }
        
        setInstanceVariable("@ptr", ffiArgs[0]);
        setInstanceVariable("@args", ffiArgs[1]);
        setInstanceVariable("@return_type", ffiArgs[2]);
        if (ffiArgs.length == 4) {
            if (this.abi == CallingConvention.DEFAULT) {
                setInstanceVariable("@abi", context.runtime.getClassFromPath("Fiddle::Function").getConstant("DEFAULT"));
            } else {
                setInstanceVariable("@abi", context.runtime.getClassFromPath("Fiddle::Function").getConstant("STDCALL"));
            }
        }

        NativeFunctionInfo nfi = new NativeFunctionInfo(context.runtime, retType, argTypes, abi);
        function = new Function(
                ptr,
                nfi.jffiReturnType,
                nfi.jffiParameterTypes,
                abi);

        method = MethodFactory.createDynamicMethod(context.runtime, metaClass, function, nfi.returnType, nfi.parameterTypes, nfi.convention, context.nil);
        
        return context.nil;
    }
    
    @JRubyMethod
    public IRubyObject call(ThreadContext context) {
        IRubyObject result = method.call(context, this, metaClass, "call");
        updateErrno(context);
        return result;
    }

    @JRubyMethod
    public IRubyObject call(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = method.call(context, this, metaClass, "call", arg0);
        updateErrno(context);
        return result;
    }

    @JRubyMethod
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = method.call(context, this, metaClass, "call", arg0, arg1);
        updateErrno(context);
        return result;
    }

    @JRubyMethod
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject result = method.call(context, this, metaClass, "call", arg0, arg1, arg2);
        updateErrno(context);
        return result;
    }

    @JRubyMethod(rest = true)
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = method.call(context, this, metaClass, "call", args);
        updateErrno(context);
        return result;
    }

    private static void updateErrno(ThreadContext context) {
        context.runtime.getModule("Fiddle").callMethod(context, "last_error=", context.runtime.newFixnum(context.runtime.getPosix().errno()));
        if (Platform.IS_WINDOWS) {
            context.runtime.getModule("Fiddle").callMethod(context, "win32_last_error=", context.runtime.newFixnum(context.runtime.getPosix().errno()));
        }
    }

    private static Type typeFromInt(Ruby runtime, int type) {
        // FIXME: slow
        RubyClass typeClass = (RubyClass)runtime.getClassFromPath("FFI::Type::Builtin");
        switch (type) {
        case FiddleLibrary.TYPE_VOID: return new Type.Builtin(runtime, typeClass, NativeType.VOID, "void");
        case FiddleLibrary.TYPE_VOIDP: return new Type.Builtin(runtime, typeClass, NativeType.POINTER, "voidp");
        case FiddleLibrary.TYPE_CHAR: return new Type.Builtin(runtime, typeClass, NativeType.CHAR, "char");
        case FiddleLibrary.TYPE_SHORT: return new Type.Builtin(runtime, typeClass, NativeType.SHORT, "short");
        case FiddleLibrary.TYPE_INT: return new Type.Builtin(runtime, typeClass, NativeType.INT, "int");
        case FiddleLibrary.TYPE_LONG: return new Type.Builtin(runtime, typeClass, NativeType.LONG, "long");
        case FiddleLibrary.TYPE_LONG_LONG: return new Type.Builtin(runtime, typeClass, NativeType.LONG_LONG, "long_long");
        case FiddleLibrary.TYPE_FLOAT: return new Type.Builtin(runtime, typeClass, NativeType.FLOAT, "float");
        case FiddleLibrary.TYPE_DOUBLE: return new Type.Builtin(runtime, typeClass, NativeType.DOUBLE, "double");
        default:
            throw runtime.newArgumentError("unknown FFI type: " + type);
        }
    }

    private volatile Function function;
    private volatile DynamicMethod method;
    private long ptr;
    private Type retType;
    private Type[] argTypes;
    private CallingConvention abi = CallingConvention.DEFAULT;
}
