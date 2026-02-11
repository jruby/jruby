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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sf.net>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.exceptions.Exception;
import org.jruby.exceptions.JumpException.FlowControlException;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.runtime.*;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.CommonByteLists;
import org.jruby.util.Sprintf;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.jruby.api.Access.*;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.RubyStringBuilder.str;

@JRubyClass(name="Exception")
public class RubyException extends RubyObject {

    private static class Backtrace {
        private BacktraceData backtraceData;
        private IRubyObject backtraceObject;
        private IRubyObject backtraceLocations;

        public void copy(Backtrace clone) {
            this.backtraceData = clone.backtraceData;
            this.backtraceObject = clone.backtraceObject;
            this.backtraceLocations = clone.backtraceLocations;
        }

        /**
         * Generate a new backtrace (or returns nil if no backtrace data exists).
         * @param runtime
         * @return the generated Ruby backtrace
         */
        public IRubyObject generateBacktrace(Ruby runtime) {
            final BacktraceData backtraceData = this.backtraceData;
            if (backtraceData == null || backtraceData == BacktraceData.EMPTY) return runtime.getNil();
            return TraceType.generateMRIBacktrace(runtime, backtraceData.getBacktrace(runtime));
        }

        /**
         * Generate an array of backtrace location objects for this backtrace.
         *
         * @param context the current thread context
         * @return the array of backtrace locations
         */
        public IRubyObject generateBacktraceLocations(ThreadContext context) {
            final BacktraceData backtraceData = this.backtraceData;
            if (backtraceData == null) return context.nil;

            final Ruby runtime = context.runtime;
            return RubyThread.Location.newLocationArray(runtime, backtraceData.getBacktrace(runtime));
        }
    }

    public static final int TRACE_HEAD = 8;
    public static final int TRACE_TAIL = 4;
    public static final int TRACE_MAX = RubyException.TRACE_HEAD + RubyException.TRACE_TAIL + 6;

    private final Backtrace backtrace = new Backtrace();

    IRubyObject message;
    private IRubyObject cause = null;
    private RaiseException throwable;

    protected RubyException(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public RubyException(Ruby runtime, RubyClass rubyClass, String message) {
        super(runtime, rubyClass);

        this.setMessage(message == null ? runtime.getNil() : runtime.newString(message));
    }

    @JRubyMethod(name = "exception", rest = true, meta = true)
    public static IRubyObject exception(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return ((RubyClass) recv).newInstance(context, args, block);
    }

    @JRubyMethod(name = "to_tty?", meta = true)
    public static IRubyObject to_tty_p(ThreadContext context, IRubyObject recv) {
        IRubyObject stderr = globalVariables(context).get("$stderr");
        IRubyObject STDERR = objectClass(context).getConstant(context, "STDERR");
        return equalInternal(context, stderr, STDERR) ? ((RubyIO) STDERR).tty_p(context) : context.fals;
    }

    @JRubyMethod(name = "===", meta = true)
    public static IRubyObject op_eqq(ThreadContext context, IRubyObject recv, IRubyObject other) {
        // special case non-FlowControlException Java exceptions so they'll be caught by rescue Exception
        if (other instanceof ConcreteJavaProxy proxy && (recv == exceptionClass(context) || recv == standardErrorClass(context))) {
            Object object = proxy.getObject();
            if (object instanceof Throwable && !(object instanceof FlowControlException)) {
                if (recv == exceptionClass(context) || object instanceof java.lang.Exception) return context.tru;
            }
        }
        // fall back on default logic
        return ((RubyClass) recv).op_eqq(context, other);
    }

    protected RaiseException constructThrowable(String message) {
        return new Exception(message, this);
    }

    public static RubyClass createExceptionClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "Exception", Object, EXCEPTION_ALLOCATOR).
                reifiedClass(RubyException.class).
                marshalWith(EXCEPTION_MARSHAL).
                classIndex(ClassIndex.EXCEPTION).
                defineMethods(context, RubyException.class);
    }

    public static final ObjectAllocator EXCEPTION_ALLOCATOR = RubyException::new;

    private static final ObjectMarshal<RubyException> EXCEPTION_MARSHAL = new ObjectMarshal<RubyException>() {
        @Override
        @Deprecated(since = "10.0.0.0", forRemoval = true)
        @SuppressWarnings("removal")
        public void marshalTo(Ruby runtime, RubyException exc, RubyClass type,
                              org.jruby.runtime.marshal.MarshalStream marshalStream) throws IOException {
            var context = runtime.getCurrentContext();
            marshalStream.registerLinkTarget(context, exc);
            List<Variable<Object>> attrs = exc.getMarshalVariableList();
            attrs.add(new VariableEntry<>("mesg", exc.getMessage()));
            attrs.add(new VariableEntry<>("bt", exc.getBacktrace()));
            marshalStream.dumpVariables(attrs);
        }

        @Override
        public void marshalTo(ThreadContext context, RubyOutputStream out, RubyException exc, RubyClass type,
                              MarshalDumper marshalStream) {
            marshalStream.registerObject(exc);
            marshalStream.dumpVariables(context, out, exc, 2, (marshal, c, o, v, receiver) -> {
                receiver.receive(marshal, c, o, "mesg", v.getMessage());
                receiver.receive(marshal, c, o, "bt", v.getBacktrace());
            });
        }

        @Override
        @Deprecated(since = "10.0.0.0", forRemoval = true)
        @SuppressWarnings("removal")
        public RubyException unmarshalFrom(Ruby runtime, RubyClass type, org.jruby.runtime.marshal.UnmarshalStream input) throws IOException {
            var context = runtime.getCurrentContext();
            RubyException exc = (RubyException) input.entry(type.allocate(context));

            input.ivar(null, exc, null);

            exc.setMessage((IRubyObject) exc.removeInternalVariable("mesg"));
            exc.set_backtrace(context, (IRubyObject) exc.removeInternalVariable("bt"));

            return exc;
        }

        @Override
        public RubyException unmarshalFrom(ThreadContext context, RubyInputStream in, RubyClass type, MarshalLoader input) {
            RubyException exc = (RubyException) input.entry(type.allocate(context));

            input.ivar(context, in, null, exc, null);

            exc.setMessage((IRubyObject) exc.removeInternalVariable("mesg"));
            exc.set_backtrace(context, (IRubyObject) exc.removeInternalVariable("bt"));

            return exc;
        }
    };

    public static RubyException newException(Ruby runtime, RubyClass exceptionClass, String msg) {
        if (msg == null) {
            msg = "No message available";
        }

        return (RubyException) exceptionClass.newInstance(runtime.getCurrentContext(), RubyString.newString(runtime, msg), Block.NULL_BLOCK);
    }

    /**
     * Construct a new RubyException object from the given exception class and message.
     *
     * @param context the current thread context
     * @param exceptionClass the exception class
     * @param message the message for the exception
     * @return the new exception object
     */
    public static RubyException newException(ThreadContext context, RubyClass exceptionClass, RubyString message) {
        return (RubyException) exceptionClass.newInstance(context, message, Block.NULL_BLOCK);
    }

    /**
     * Construct a new RubyException object from the given exception class and message.
     *
     * @param context the current thread context
     * @param exceptionClass the exception class
     * @param args the arguments for the exception's constructor
     * @return the new exception object
     */
    public static RubyException newException(ThreadContext context, RubyClass exceptionClass, IRubyObject... args) {
        return (RubyException) exceptionClass.newInstance(context, args, Block.NULL_BLOCK);
    }

    @JRubyMethod
    public IRubyObject full_message(ThreadContext context) {
        return full_message(context, null);
    }

    @JRubyMethod
    public IRubyObject full_message(ThreadContext context, IRubyObject opts) {
        return TraceType.printFullMessage(context, this, opts);
    }

    @JRubyMethod
    public IRubyObject detailed_message(ThreadContext context) {
        return detailed_message(context, (IRubyObject) null);
    }

    @JRubyMethod
    public IRubyObject detailed_message(ThreadContext context, IRubyObject opts) {
        return TraceType.printDetailedMessage(context, this, opts);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject detailed_message(ThreadContext context, IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> detailed_message(context);
            case 1 -> detailed_message(context, args[0]);
            default -> throw argumentError(context, args.length, 0, 1);
        };
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        // cause filled in at RubyKernel#raise ... Exception.new does not fill-in cause!
        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        setMessage(arg0);
        // cause filled in at RubyKernel#raise ... Exception.new does not fill-in cause!
        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        // cause filled in at RubyKernel#raise ... Exception.new does not fill-in cause!
        return this;
    }

    public IRubyObject initialize(IRubyObject[] args, Block block) {
        var context = getRuntime().getCurrentContext();
        return switch (args.length) {
            case 0 -> initialize(context);
            case 1 -> initialize(context, args[0]);
            case 2 -> initialize(context, args[0], args[1]);
            default -> throw argumentError(context, args.length, 0, 2);
        };
    }

    @JRubyMethod
    public IRubyObject backtrace() {
        return getBacktrace();
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject set_backtrace(IRubyObject obj) {
        return set_backtrace(getCurrentContext(), obj);
    }

    @JRubyMethod
    public IRubyObject set_backtrace(ThreadContext context, IRubyObject obj) {
        setBacktrace(context, obj);
        return backtrace();
    }

    public void setBacktrace(ThreadContext context, IRubyObject obj) {
        BacktraceData backtraceData = locationArrayToBacktrace(obj);
        if (backtraceData != null) {
            this.backtrace.backtraceData = backtraceData;
            this.backtrace.backtraceObject = null;
            this.backtrace.backtraceLocations = obj;
        } else {
            this.backtrace.backtraceObject = checkBacktrace(context, obj);
        }
    }

    // MRI: rb_check_backtrace
    private static IRubyObject checkBacktrace(ThreadContext context, IRubyObject obj) {
        if (obj.isNil() || isArrayOfStrings(obj)) {
            return obj;
        } else if (obj instanceof RubyString) {
            return RubyArray.newArray(context.runtime, obj);
        } else {
            throw typeError(context,  "backtrace must be an Array of String or an Array of Thread::Backtrace::Location");
        }
    }

    // MRI: rb_location_ary_to_backtrace
    private BacktraceData locationArrayToBacktrace(IRubyObject ary) {
        if (!(ary instanceof RubyArray array) || array.size() == 0 || asBacktraceLocation(array.eltOk(0)) == null) {
            return null;
        }

        int num_frames = array.size();
        RubyStackTraceElement[] backtrace = new RubyStackTraceElement[num_frames];

        for (int index = 0; index < num_frames; index++) {
            RubyThread.Location locobj = asBacktraceLocation(array.eltOk(index));

            if (locobj == null) {
                return null;
            }

            backtrace[index] = locobj.getElement();
        }

        return new BacktraceData(backtrace);
    }

    // MRI: rb_frame_info_p
    private static RubyThread.Location asBacktraceLocation(IRubyObject object) {
        if (object instanceof RubyThread.Location) {
            return (RubyThread.Location) object;
        }

        return null;
    }

    @JRubyMethod(omit = true)
    public IRubyObject backtrace_locations(ThreadContext context) {
        IRubyObject backtraceLocations = backtrace.backtraceLocations;
        if (backtraceLocations != null) return backtraceLocations;
        return backtrace.backtraceLocations = backtrace.generateBacktraceLocations(context);
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public RubyException exception(IRubyObject[] args) {
        switch (args.length) {
            case 0 :
                return this;
            case 1 :
                if (args[0] == this) return this;
                RubyException ret = (RubyException) rbClone();
                ret.initialize(args, Block.NULL_BLOCK); // This looks wrong, but it's the way MRI does it.
                return ret;
            default :
                throw argumentError(getRuntime().getCurrentContext(), "Wrong argument count");
        }
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        final IRubyObject msg = getMessage();
        return !msg.isNil() ? msg.asString() : newString(context, getMetaClass().getRealClass().getName(context));
    }

    @JRubyMethod(name = "message")
    public IRubyObject message(ThreadContext context) {
        return callMethod(context, "to_s");
    }

    /** inspects an object and return a kind of debug information
     *
     *@return A RubyString containing the debug information.
     */
    @JRubyMethod(name = "inspect")
    public RubyString inspect(ThreadContext context) {
        RubyClass klass = getMetaClass();

        RubyString exc = asString();
        if (exc.size() == 0) {
            return klass.rubyName(context);
        }

        RubyString str = Create.newString(context, "#<");
        str.append(klass.rubyName(context));

        if (exc.include_p(context, RubyString.newStringShared(context.runtime, CommonByteLists.NEWLINE)).isTrue()) {
            str.cat(Sprintf.sprintf(exc.getEncoding(), ":%p", exc));
        }
        else {
            str.catString(": ");
            str.append(exc);
        }

        str.cat('>');

        return str;
    }

    @Override
    @JRubyMethod(name = "==")
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;

        return asBoolean(context, exceptionClass(context).isInstance(other) &&
                getMetaClass().getRealClass() == other.getMetaClass().getRealClass() &&
                callMethod(context, "message").equals(other.callMethod(context, "message")) &&
                callMethod(context, "backtrace").equals(other.callMethod(context, "backtrace")));
    }

    @JRubyMethod(name = "cause")
    public IRubyObject cause(ThreadContext context) {
        return cause == null ? context.nil : cause;
    }

    /**
     * Coerce this Ruby exception to the requested Java type, if possible.
     *
     * If the requested type is a supertype of RaiseException, the attached throwable will be returned.
     *
     * Otherwise, it will fall back on RubyBasicObject toJava logic.
     *
     * @param target the target type to which this object should be converted
     * @return the converted result
     */
    @Override
    public <T> T toJava(Class<T> target) {
        if (target.isAssignableFrom(RaiseException.class)) {
            return target.cast(toThrowable());
        }
        return super.toJava(target);
    }

    /**
     * Get a throwable suitable for throwing in Java.
     *
     * The throwable here will be constructed lazily by calling constructThrowable and then cached for future calls.
     *
     * All throwables returned by Ruby exception objects will descend from RaiseException and follow the Throwable
     * hierarchy below {@link Exception}
     * @return
     */
    public RaiseException toThrowable() {
        if (throwable == null) {
            return throwable = constructThrowable(getMessageAsJavaString());
        }
        return throwable;
    }

    public void setCause(IRubyObject cause) {
        setCause(getRuntime().getCurrentContext(), cause);
    }

    public void setCause(ThreadContext context, IRubyObject cause) {
        if (cause == this) {
            this.cause = context.nil;
            return;
        }

        Object c = cause;
        while (c instanceof RubyException causeException) {
            if (c == this) {
                throw argumentError(context, "circular causes");
            }
            c = causeException.getCause();
        }

        this.cause = cause;

        // don't do anything to throwable for null/nil cause to avoid forcing backtrace
        // * if we have no cause yet, it's a no-op
        // * if we have a cause, we can't change it
        if (cause == null || cause.isNil()) return;

        Throwable t = toThrowable();
        Object javaCause;

        if (t.getCause() == null) {
            if (cause instanceof RubyException) {
                t.initCause(((RubyException) cause).toThrowable());
            } else if (cause instanceof ConcreteJavaProxy && (javaCause = ((ConcreteJavaProxy) cause).getObject()) instanceof Throwable) {
                t.initCause((Throwable) javaCause);
            } else {
                Ruby runtime = getRuntime();
                throw typeError(runtime.getCurrentContext(), "exception object expected");
            }
        }
    }

    // NOTE: can not have IRubyObject as NativeException has getCause() returning Throwable
    public Object getCause() {
        return cause == this ? null : cause;
    }

    public RubyStackTraceElement[] getBacktraceElements() {
        if (backtrace.backtraceData == null) {
            return RubyStackTraceElement.EMPTY_ARRAY;
        }
        return backtrace.backtraceData.getBacktrace(getRuntime());
    }

    public void captureBacktrace(ThreadContext context) {
        backtrace.backtraceData = instanceConfig(context).getTraceType().getBacktrace(context);
    }

    public IRubyObject getBacktrace() {
        IRubyObject backtraceObject = backtrace.backtraceObject;
        if (backtraceObject != null) return backtraceObject;

        return backtrace.backtraceObject = backtrace.generateBacktrace(getRuntime());
    }

    /**
     * Retrieve the current backtrace object for a given exception.
     * @param exception
     * @return set (or already generated) backtrace, null otherwise
     * <p>Note: Internal API.</p>
     */
    public static IRubyObject retrieveBacktrace(RubyException exception) {
        return exception.backtrace.backtraceObject;
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyException exception = (RubyException)clone;
        exception.backtrace.copy(backtrace);
        exception.message = message;
    }

    /**
     * Print the Ruby exception's backtrace to the given PrintStream.
     *
     * @param errorStream the PrintStream to which backtrace should be printed
     */
    public void printBacktrace(PrintStream errorStream) {
        printBacktrace(errorStream, 0);
    }

    /**
     * Print the Ruby exception's backtrace to the given PrintStream. This
     * version accepts a number of lines to skip and is primarily used
     * internally for exception printing where the first line is treated specially.
     *
     * @param errorStream the PrintStream to which backtrace should be printed
     */
    public void printBacktrace(PrintStream errorStream, int skip) {
        ThreadContext context = getRuntime().getCurrentContext();
        IRubyObject trace = callMethod(context, "backtrace");
        RubyString string = newEmptyString(context);
        TraceType.printBacktraceToStream(context, trace, string, skip);
        errorStream.print(string);
    }

    private static boolean isArrayOfStrings(IRubyObject backtrace) {
        if (!(backtrace instanceof RubyArray)) return false;

        final RubyArray rTrace = ((RubyArray) backtrace);

        for (int i = 0 ; i < rTrace.getLength() ; i++) {
            if (!(rTrace.eltInternal(i) instanceof RubyString)) return false;
        }

        return true;
    }

     /**
     * @return error message if provided or nil
     */
    public IRubyObject getMessage() {
        return message == null ? getRuntime().getNil() : message;
    }

    /**
     * Set the message for this NameError.
     * @param message the message
     */
    public void setMessage(IRubyObject message) {
        this.message = message;
    }

    public String getMessageAsJavaString() {
        final IRubyObject msg = getMessage();
        return msg.isNil() ? null : msg.toString();
    }

    // Note: we override this because we do not use traditional internal variables (concurrency complications)
    // for a few fields but MRI does.  Both marshal and oj (native extensions) expect to see these.
    @Override
    public List<Variable<Object>> getVariableList() {
        List<Variable<Object>> attrs = super.getVariableList();

        attrs.add(new VariableEntry<>("mesg", getMessage()));
        IRubyObject backtrace = getBacktrace();
        attrs.add(new VariableEntry<>("bt", backtrace));

        return attrs;
    }

    @Override
    public List<String> getVariableNameList() {
        List<String> names = super.getVariableNameList();

        names.add("mesg");
        names.add("bt");

        return names;
    }

    @Deprecated(since = "9.2.7.0")
    public void prepareIntegratedBacktrace(ThreadContext context, StackTraceElement[] javaTrace) {
        // if it's null, build a backtrace
        if (backtrace.backtraceData == null) {
            backtrace.backtraceData = instanceConfig(context).getTraceType().getIntegratedBacktrace(context, javaTrace);
        }
    }

    @Deprecated(since = "9.3.0.0")
    public static IRubyObject newException(ThreadContext context, RubyClass exceptionClass, IRubyObject message) {
        return newException(context, exceptionClass, message.convertToString());
    }
}
