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

import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
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

        static ObjectAllocator NAMEERRORMESSAGE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                IRubyObject dummy = new RubyObject(runtime, runtime.getObject());
                return new RubyNameErrorMessage(runtime, dummy, dummy, Visibility.PRIVATE, CallType.VARIABLE);
            }
        };        

        private final IRubyObject object;
        private final IRubyObject method;
        private final Visibility visibility;
        private final CallType callType;

        RubyNameErrorMessage(Ruby runtime, IRubyObject object, IRubyObject method, Visibility visibility, CallType callType) {
            super(runtime, runtime.getNameErrorMessage(), false);
            this.object = object;
            this.method = method;
            this.visibility = visibility;
            this.callType = callType;
        }

        @JRubyMethod(name = "_load", meta = true)
        public static IRubyObject load(IRubyObject recv, IRubyObject arg) {
            return arg;
        }

        @JRubyMethod(name = "_dump")
        public IRubyObject dump(ThreadContext context, IRubyObject arg) {
            return to_str(context);
        }

        @JRubyMethod(name = "to_str")
        public IRubyObject to_str(ThreadContext context) {
            String format = null;

            if (visibility == PRIVATE) {
                format = "private method `%s' called for %s";
            } else if (visibility == PROTECTED) {
                format = "protected method `%s' called for %s";
            } else if (callType == CallType.VARIABLE) {
                format = "undefined local variable or method `%s' for %s";
            } else if (callType == CallType.SUPER) {
                format = "super: no superclass method `%s'";
            }

            if (format == null) format = "undefined method `%s' for %s";

            String description = null;

            if (object.isNil()) {
                description = "nil";
            } else if (object instanceof RubyBoolean && object.isTrue()) {
                description = "true";
            } else if (object instanceof RubyBoolean && !object.isTrue()) {
                description = "false";
            } else {
                try {
                    description = RubyObject.inspect(context, object).toString();
                } catch(JumpException e) {}

                if (description == null || description.length() > 65) description = object.anyToString().toString();
            }

            if (description.length() == 0 || (description.length() > 0 && description.charAt(0) != '#')) {
                description = description + ":" + object.getMetaClass().getRealClass().getName();            
            }

            Ruby runtime = getRuntime();
            RubyArray arr = runtime.newArray(method, runtime.newString(description));
            ByteList msgBytes = new ByteList(format.length() + description.length() + method.toString().length());
            Sprintf.sprintf(msgBytes, format, arr);
            return runtime.newString(msgBytes).infectBy(object);
        }
    }

    private static ObjectAllocator NAMEERROR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNameError(runtime, klass);
        }
    };

    public static RubyClass createNameErrorClass(Ruby runtime, RubyClass standardErrorClass) {
        RubyClass nameErrorClass = runtime.defineClass("NameError", standardErrorClass, NAMEERROR_ALLOCATOR);
        nameErrorClass.defineAnnotatedMethods(RubyNameError.class);
        return nameErrorClass;
    }

    public static RubyClass createNameErrorMessageClass(Ruby runtime, RubyClass nameErrorClass) {
        RubyClass messageClass = nameErrorClass.defineClassUnder("Message", runtime.getClass("Data"), RubyNameErrorMessage.NAMEERRORMESSAGE_ALLOCATOR);
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

    @JRubyMethod(name = "exception", rest = true, meta = true)
    public static RubyException newRubyNameError(IRubyObject recv, IRubyObject[] args) {
        RubyClass klass = (RubyClass)recv;
        
        RubyException newError = (RubyException) klass.allocate();
        
        newError.callInit(args, Block.NULL_BLOCK);
        
        return newError;
    }

    @JRubyMethod(name = "initialize", optional = 2)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length > 1) {
            name = args[args.length - 1];
            int newLength = args.length > 2 ? args.length - 2 : args.length - 1;

            IRubyObject []tmpArgs = new IRubyObject[newLength];
            System.arraycopy(args, 0, tmpArgs, 0, newLength);
            args = tmpArgs;
        } else {
            name = getRuntime().getNil();
        }

        super.initialize(args, block);
        return this;
    }

    @JRubyMethod(name = "to_s")
    @Override
    public IRubyObject to_s() {
        if (message.isNil()) return getRuntime().newString(message.getMetaClass().getName());
        RubyString str = message.convertToString();
        if (str != message) message = str;
        if (isTaint()) message.setTaint(true);
        return message;
    }

    @JRubyMethod(name = "name")
    public IRubyObject name() {
        return name;
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyNameError exception = (RubyNameError)clone;
        exception.name = name;
    }
}
