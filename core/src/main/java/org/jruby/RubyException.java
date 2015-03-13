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
import org.jruby.exceptions.JumpException.FlowControlException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.runtime.*;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.jruby.runtime.Visibility.PRIVATE;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Exception")
public class RubyException extends RubyObject {
    protected RubyException(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, null);
    }

    public RubyException(Ruby runtime, RubyClass rubyClass, String message) {
        super(runtime, rubyClass);
        
        this.message = message == null ? runtime.getNil() : runtime.newString(message);
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length == 1) message = args[0];

        IRubyObject errinfo = getRuntime().getCurrentContext().getErrorInfo();
        if (!errinfo.isNil()) cause = errinfo;
        return this;
    }

    @JRubyMethod
    public IRubyObject backtrace() {
        IRubyObject bt = getBacktrace();
        return bt;
    }

    @JRubyMethod(required = 1)
    public IRubyObject set_backtrace(IRubyObject obj) {
        if (obj.isNil()) {
            backtrace = null;
        } else if (isArrayOfStrings(obj)) {
            backtrace = obj;
        } else if (obj instanceof RubyString) {
            backtrace = RubyArray.newArray(getRuntime(), obj);
        } else {
            throw getRuntime().newTypeError("backtrace must be Array of String or a single String");
        }

        return backtrace();
    }
    
    @JRubyMethod(omit = true)
    public IRubyObject backtrace_locations(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyStackTraceElement[] elements = backtraceData.getBacktrace(runtime);
        
        return RubyThread.Location.newLocationArray(runtime, elements);
    }

    @JRubyMethod(name = "exception", optional = 1, rest = true, meta = true)
    public static IRubyObject exception(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return ((RubyClass) recv).newInstance(context, args, block);
    }

    @JRubyMethod(optional = 1)
    public RubyException exception(IRubyObject[] args) {
        switch (args.length) {
            case 0 :
                return this;
            case 1 :
                if(args[0] == this) {
                    return this;
                }
                RubyException ret = (RubyException)rbClone();
                ret.initialize(args, Block.NULL_BLOCK); // This looks wrong, but it's the way MRI does it.
                return ret;
            default :
                throw getRuntime().newArgumentError("Wrong argument count");
        }
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        if (message.isNil()) return context.runtime.newString(getMetaClass().getRealClass().getName());

        message.setTaint(isTaint());
        return message.asString();
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
    public IRubyObject inspect(ThreadContext context) {
        // rb_class_name skips intermediate classes (JRUBY-6786)
        RubyModule rubyClass = getMetaClass().getRealClass();
        RubyString exception = RubyString.objAsString(context, this);

        if (exception.isEmpty()) return getRuntime().newString(rubyClass.getName());
        StringBuilder sb = new StringBuilder("#<");
        sb.append(rubyClass.getName()).append(": ").append(exception.getByteList()).append(">");
        return getRuntime().newString(sb.toString());
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.runtime.getTrue();

        boolean equal = context.runtime.getException().isInstance(other) &&
                callMethod(context, "message").equals(other.callMethod(context, "message")) &&
                callMethod(context, "backtrace").equals(other.callMethod(context, "backtrace")) &&
                getMetaClass().getRealClass() == other.getMetaClass().getRealClass();
        return context.runtime.newBoolean(equal);
    }

    @JRubyMethod(name = "===", meta = true)
    public static IRubyObject op_eqq(ThreadContext context, IRubyObject recv, IRubyObject other) {
        Ruby runtime = context.runtime;
        // special case non-FlowControlException Java exceptions so they'll be caught by rescue Exception
        if (other instanceof ConcreteJavaProxy &&
                (recv == runtime.getException() || recv == runtime.getStandardError())) {

            Object object = ((ConcreteJavaProxy)other).getObject();
            if (object instanceof Throwable && !(object instanceof FlowControlException)) {
                if (recv == runtime.getException() || object instanceof Exception) {
                    return context.runtime.getTrue();
                }
            }
        }
        // fall back on default logic
        return ((RubyClass)recv).op_eqq(context, other);
    }

    @JRubyMethod(name = "cause")
    public IRubyObject cause(ThreadContext context) {
        IRubyObject nil = context.nil;
        if (cause != nil) return cause;
        return nil;
    }

    public void setCause(IRubyObject cause) {
        this.cause = cause;
    }

    public void setBacktraceData(BacktraceData backtraceData) {
        this.backtraceData = backtraceData;
    }

    public BacktraceData getBacktraceData() {
        return backtraceData;
    }

    public RubyStackTraceElement[] getBacktraceElements() {
        if (backtraceData == null) {
            return RubyStackTraceElement.EMPTY_ARRAY;
        }
        return backtraceData.getBacktrace(getRuntime());
    }

    public void prepareBacktrace(ThreadContext context, boolean nativeException) {
        // if it's null, build a backtrace
        if (backtraceData == null) {
            backtraceData = context.runtime.getInstanceConfig().getTraceType().getBacktrace(context, nativeException);
        }
    }

    /**
     * Prepare an "integrated" backtrace that includes the normal Ruby trace plus non-filtered Java frames. Used by
     * Java integration to show the Java frames for a JI-called method.
     *
     * @param context
     * @param javaTrace
     */
    public void prepareIntegratedBacktrace(ThreadContext context, StackTraceElement[] javaTrace) {
        // if it's null, build a backtrace
        if (backtraceData == null) {
            backtraceData = context.runtime.getInstanceConfig().getTraceType().getIntegratedBacktrace(context, javaTrace);
        }
    }

    public void forceBacktrace(IRubyObject backtrace) {
        backtraceData = BacktraceData.EMPTY;
        set_backtrace(backtrace);
    }
    
    public IRubyObject getBacktrace() {
        if (backtrace == null) {
            initBacktrace();
        }
        return backtrace;
    }
    
    public void initBacktrace() {
        Ruby runtime = getRuntime();
        if (backtraceData == null) {
            backtrace = runtime.getNil();
        } else {
            backtrace = TraceType.generateMRIBacktrace(runtime, backtraceData.getBacktrace(runtime));
        }
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        RubyException exception = (RubyException)clone;
        exception.backtraceData = backtraceData;
        exception.backtrace = backtrace;
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
        IRubyObject trace = callMethod(getRuntime().getCurrentContext(), "backtrace");
        if (!trace.isNil() && trace instanceof RubyArray) {
            IRubyObject[] elements = trace.convertToArray().toJavaArray();

            for (int i = skip; i < elements.length; i++) {
                IRubyObject stackTraceLine = elements[i];
                if (stackTraceLine instanceof RubyString) {
                    printStackTraceLine(errorStream, stackTraceLine);
                }
            }
        }
    }

    private void printStackTraceLine(PrintStream errorStream, IRubyObject stackTraceLine) {
            errorStream.print("\tfrom " + stackTraceLine + '\n');
    }
	
    private boolean isArrayOfStrings(IRubyObject backtrace) {
        if (!(backtrace instanceof RubyArray)) return false; 
            
        IRubyObject[] elements = ((RubyArray) backtrace).toJavaArray();
        
        for (int i = 0 ; i < elements.length ; i++) {
            if (!(elements[i] instanceof RubyString)) return false;
        }
            
        return true;
    }

    public static ObjectAllocator EXCEPTION_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyException instance = new RubyException(runtime, klass);

            // for future compatibility as constructors move toward not accepting metaclass?
            instance.setMetaClass(klass);

            return instance;
        }
    };

    private static final ObjectMarshal EXCEPTION_MARSHAL = new ObjectMarshal() {
        @Override
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            RubyException exc = (RubyException)obj;

            marshalStream.registerLinkTarget(exc);
            List<Variable<Object>> attrs = exc.getVariableList();
            attrs.add(new VariableEntry<Object>(
                    "mesg", exc.message == null ? runtime.getNil() : exc.message));
            attrs.add(new VariableEntry<Object>("bt", exc.getBacktrace()));
            marshalStream.dumpVariables(attrs);
        }

        @Override
        public Object unmarshalFrom(Ruby runtime, RubyClass type,
                                    UnmarshalStream unmarshalStream) throws IOException {
            RubyException exc = (RubyException)type.allocate();

            unmarshalStream.registerLinkTarget(exc);
            // FIXME: Can't just pull these off the wire directly? Or maybe we should
            // just use real vars all the time for these?
            unmarshalStream.defaultVariablesUnmarshal(exc);

            exc.message = (IRubyObject)exc.removeInternalVariable("mesg");
            exc.set_backtrace((IRubyObject)exc.removeInternalVariable("bt"));

            return exc;
        }
    };

    public static RubyClass createExceptionClass(Ruby runtime) {
        RubyClass exceptionClass = runtime.defineClass("Exception", runtime.getObject(), EXCEPTION_ALLOCATOR);
        runtime.setException(exceptionClass);

        exceptionClass.setClassIndex(ClassIndex.EXCEPTION);
        exceptionClass.setReifiedClass(RubyException.class);

        exceptionClass.setMarshal(EXCEPTION_MARSHAL);
        exceptionClass.defineAnnotatedMethods(RubyException.class);

        return exceptionClass;
    }

    public static RubyException newException(Ruby runtime, RubyClass excptnClass, String msg) {
        return new RubyException(runtime, excptnClass, msg);
    }

    // rb_exc_new3
    public static IRubyObject newException(ThreadContext context, RubyClass exceptionClass, IRubyObject message) {
        return exceptionClass.callMethod(context, "new", message.convertToString());
    }

    @Deprecated
    public IRubyObject to_s19(ThreadContext context) {
        if (message.isNil()) return context.runtime.newString(getMetaClass().getRealClass().getName());

        message.setTaint(isTaint());
        return message.asString();
    }

    public IRubyObject getMessage() {
        return message;
    }

    private BacktraceData backtraceData;
    private IRubyObject backtrace;
    public IRubyObject message;
    private IRubyObject cause = getRuntime().getNil();

    public static final int TRACE_HEAD = 8;
    public static final int TRACE_TAIL = 4;
    public static final int TRACE_MAX = TRACE_HEAD + TRACE_TAIL + 6;

}
