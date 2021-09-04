/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;

/**
 * The Java representation of a Ruby NameError.
 *
 * @see NameError
 * @author Anders Bengtsson
 */
@JRubyClass(name="NameError", parent="StandardError")
public class RubyNameError extends RubyStandardError {
    private IRubyObject name;
    private IRubyObject receiver;
    protected boolean privateCall;

    /**
     * Nested class whose instances act as thunks reacting to to_str method
     * called from (Exception#to_str, Exception#message)
     * MRI equivalent: rb_cNameErrorMesg, class name: "message", construction method: "!",
     * to_str implementation: "name_err_mesg_to_str"
     *
     * TODO: this class should not be lookupable
     */
    @JRubyClass(name = "NameError::Message", parent = "Data")
    public static final class RubyNameErrorMessage extends RubyObject {

        private final String message;
        private final IRubyObject object;
        private final IRubyObject name;

        RubyNameErrorMessage(Ruby runtime, RubyClass klazz) {
            super(runtime, klazz);
            this.message = null;
            this.object = null;
            this.name = null;
        }

        RubyNameErrorMessage(Ruby runtime, String message, IRubyObject object, IRubyObject name) {
            super(runtime, runtime.getNameErrorMessage(), false);
            this.message = message;
            this.object = object;
            this.name = name;
        }

        @JRubyMethod(name = "_load", meta = true)
        public static IRubyObject load(IRubyObject recv, IRubyObject arg) {
            return arg;
        }

        static RubyClass define(Ruby runtime, RubyClass NameError) {
            RubyClass Message = NameError.defineClassUnder("Message", runtime.getClass("Data"), RubyNameErrorMessage::new);
            NameError.setConstantVisibility(runtime, "Message", true);
            Message.defineAnnotatedMethods(RubyNameErrorMessage.class);
            return Message;
        }

        @JRubyMethod(name = "_dump")
        public IRubyObject dump(ThreadContext context, IRubyObject arg) {
            return to_str(context);
        }

        @JRubyMethod
        public IRubyObject to_str(ThreadContext context) {
            if (message == null) return context.nil;

            final Ruby runtime = context.runtime;

            RubyString description = null;
            boolean singleton = false;

            if (object == context.nil) {
                description = RubyNil.inspect(runtime); // "nil"
            } else if (object == context.tru) {
                description = RubyString.newStringShared(runtime, RubyBoolean.TRUE_BYTES); // "true"
            } else if (object == context.fals) {
                description = RubyString.newStringShared(runtime, RubyBoolean.FALSE_BYTES); // "false"
            } else {
                try {
                    description = RubyObject.inspect(context, object).asString();
                } catch (JumpException e) {
                    context.setErrorInfo(context.nil);
                }

                if (description == null || description.size() > 65) {
                    description = object.anyToString().asString();
                }

                singleton = description.size() > 0 && description.getByteList().get(0) == '#';
            }

            RubyString separator;
            RubyString className;

            if (!singleton) {
                separator = RubyString.newString(runtime, (byte) ':');
                className = RubyString.newString(runtime, object.getMetaClass().getRealClass().getName());
            } else {
                className = separator = RubyString.newEmptyString(runtime);
            }

            // RubyString name = this.name.asString(); // Symbol -> String

            RubyArray arr = RubyArray.newArray(runtime, this.name, description, separator, className);

            ByteList msgBytes = new ByteList(message.length() + description.size() + 16); // name.size()
            Sprintf.sprintf(msgBytes, message, arr);

            return runtime.newString(msgBytes);
        }
    }

    static RubyClass define(Ruby runtime, RubyClass StandardError) {
        RubyClass NameError = runtime.defineClass("NameError", StandardError, RubyNameError::new);
        NameError.defineAnnotatedMethods(RubyNameError.class);
        NameError.setReifiedClass(RubyNameError.class);
        return NameError;
    }

    protected RubyNameError(Ruby runtime, RubyClass exceptionClass) {
        this(runtime, exceptionClass, exceptionClass.getName());
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message) {
        this(runtime, exceptionClass, message, (String) null);
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message, String name) {
        super(runtime, exceptionClass, message);
        this.name = name == null ? runtime.getNil() : RubySymbol.newSymbol(runtime, name);
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message, IRubyObject name) {
        super(runtime, exceptionClass, message);
        this.name = name;
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new NameError(message, this);
    }

    @JRubyMethod(name = "exception", meta = true)
    public static IRubyObject exception(ThreadContext context, IRubyObject recv) {
        return newNameError(recv, NULL_ARRAY);
    }

    @JRubyMethod(name = "exception", meta = true)
    public static RubyException exception(ThreadContext context, IRubyObject recv, IRubyObject message) {
        return newNameError(recv, new IRubyObject[] { message });
    }

    @JRubyMethod(name = "exception", meta = true)
    public static RubyException exception(ThreadContext context, IRubyObject recv, IRubyObject message, IRubyObject name) {
        return newNameError(recv, message, name, false);
    }

    private static RubyException newNameError(IRubyObject recv, IRubyObject[] args) {
        final RubyClass klass = (RubyClass) recv;
        RubyException newError = (RubyException) klass.allocate();

        newError.callInit(args, Block.NULL_BLOCK);

        return newError;
    }

    static RubyException newNameError(IRubyObject recv, IRubyObject message, IRubyObject name, boolean privateCall) {
        final RubyClass klass = (RubyClass) recv;
        RubyNameError newError = (RubyNameError) klass.allocate();

        newError.callInit(message, name, Block.NULL_BLOCK);

        newError.privateCall = privateCall;

        return newError;
    }

    @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        RubyHash options = null;

        if (args.length > 0) {
            if ((args[args.length - 1] != null) && (args[args.length - 1] instanceof RubyHash)) {
                options = (RubyHash)args[args.length - 1];
                args = ArraySupport.newCopy(args, args.length - 1);
            }
        }
        
        return initializeOptions(args, options, block);
    }

    public IRubyObject initializeOptions(IRubyObject[] args, RubyHash options, Block block) {
        String [] keywords = {"receiver"};
 
        if (args.length > 0) this.message = args[0];
        if (message instanceof RubyNameErrorMessage) this.receiver = ((RubyNameErrorMessage) message).object;
        this.name = args.length > 1 ? args[1] : getRuntime().getNil();

        if (options != null) {
            IRubyObject [] values = ArgsUtil.extractKeywordArgs(getRuntime().getCurrentContext(), options, keywords);
            if ((values != null) && (values.length == 1) && (values[0] != null)) {
                this.receiver = values[0];
            }
        }
        return super.initialize(NULL_ARRAY, block); // message already set
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s(ThreadContext context) {
        if (message == context.nil) {
            return context.runtime.newString(getMetaClass().getRealClass().getName());
        }
        RubyString str = message.convertToString();
        if (str != message) message = str;
        return message;
    }

    @JRubyMethod
    public IRubyObject name() {
        return name;
    }

    @JRubyMethod
    public IRubyObject receiver(ThreadContext context) {
        if (receiver != null) return receiver;

        throw context.runtime.newArgumentError("no receiver is available");
    }

    @JRubyMethod(name = "private_call?")
    public IRubyObject private_call_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isPrivateCall());
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyNameError exception = (RubyNameError)clone;
        exception.name = name;
    }

    public boolean isPrivateCall() {
        return privateCall;
    }
}
