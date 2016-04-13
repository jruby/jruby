/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;

/**
 * @author Anders Bengtsson
 */
@JRubyClass(name="NameError", parent="StandardError")
public class RubyNameError extends RubyException {
    private IRubyObject name;

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

        private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyNameErrorMessage(runtime);
            }
        };

        private String message;
        private IRubyObject object;
        private IRubyObject name;

        RubyNameErrorMessage(Ruby runtime) {
            super(runtime, runtime.getNameErrorMessage());
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

        @JRubyMethod(name = "_dump")
        public IRubyObject dump(ThreadContext context, IRubyObject arg) {
            return to_str(context);
        }

        @JRubyMethod
        public IRubyObject to_str(ThreadContext context) {
            final Ruby runtime = context.runtime;

            if (message == null) {
                return context.nil;
            } else {
                String description = null;
                boolean singleton = false;

                if (object.isNil()) {
                    description = "nil";
                } else if (object instanceof RubyBoolean && object.isTrue()) {
                    description = "true";
                } else if (object instanceof RubyBoolean && !object.isTrue()) {
                    description = "false";
                } else {
                    try {
                        description = RubyObject.inspect(context, object).toString();
                    } catch (JumpException e) {
                        context.setErrorInfo(context.nil);
                    }

                    if (description == null || description.length() > 65) {
                        description = object.anyToString().toString();
                    }

                    singleton = description.length() > 0 && description.charAt(0) == '#';
                }

                if (!singleton) {
                    description = description + ':' + object.getMetaClass().getRealClass().getName();
                }
                //IRubyObject[] args = new IRubyObject[] { name, runtime.newString(description) };

                RubyArray arr = runtime.newArray(name, runtime.newString(description));
                ByteList msgBytes = new ByteList(this.message.length() + description.length() + name.toString().length());
                Sprintf.sprintf(msgBytes, this.message, arr);

                return runtime.newString(msgBytes);
            }
        }
    }

    private static final ObjectAllocator NAMEERROR_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNameError(runtime, klass);
        }
    };

    static RubyClass createNameErrorClass(Ruby runtime, RubyClass standardErrorClass) {
        RubyClass nameErrorClass = runtime.defineClass("NameError", standardErrorClass, NAMEERROR_ALLOCATOR);
        nameErrorClass.defineAnnotatedMethods(RubyNameError.class);
        return nameErrorClass;
    }

    static RubyClass createNameErrorMessageClass(Ruby runtime, RubyClass nameErrorClass) {
        RubyClass messageClass = nameErrorClass.defineClassUnder("Message", runtime.getClass("Data"), RubyNameErrorMessage.ALLOCATOR);
        messageClass.defineAnnotatedMethods(RubyNameErrorMessage.class);
        return messageClass;
    }

    protected RubyNameError(Ruby runtime, RubyClass exceptionClass) {
        this(runtime, exceptionClass, exceptionClass.getName());
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message) {
        this(runtime, exceptionClass, message, (String) null);
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message, String name) {
        super(runtime, exceptionClass, message);
        this.name = name == null ? runtime.getNil() : runtime.newString(name);
    }

    public RubyNameError(Ruby runtime, RubyClass exceptionClass, String message, IRubyObject name) {
        super(runtime, exceptionClass, message);
        this.name = name;
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
        return newNameError(recv, message, name);
    }

    private static RubyException newNameError(IRubyObject recv, IRubyObject[] args) {
        final RubyClass klass = (RubyClass) recv;
        RubyException newError = (RubyException) klass.allocate();

        newError.callInit(args, Block.NULL_BLOCK);

        return newError;
    }

    static RubyException newNameError(IRubyObject recv, IRubyObject message, IRubyObject name) {
        final RubyClass klass = (RubyClass) recv;
        RubyException newError = (RubyException) klass.allocate();

        newError.callInit(message, name, Block.NULL_BLOCK);

        return newError;
    }

    @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if ( args.length > 0 ) this.message = args[0];
        if ( args.length > 1 ) this.name = args[1];
        else this.name = getRuntime().getNil();
        super.initialize(NULL_ARRAY, block); // message already set
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s(ThreadContext context) {
        if (message.isNil()) {
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
        if (message instanceof RubyNameErrorMessage) {
            return ((RubyNameErrorMessage) message).object;
        }

        throw context.runtime.newArgumentError("no receiver is available");
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyNameError exception = (RubyNameError)clone;
        exception.name = name;
    }
}
