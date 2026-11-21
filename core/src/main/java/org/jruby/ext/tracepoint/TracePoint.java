package org.jruby.ext.tracepoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.AbstractRubyMethod;
import org.jruby.Ruby;
import org.jruby.RubyBinding;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubySymbol;
import org.jruby.RubyThread;
import org.jruby.anno.CRuby;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.TraceInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static org.jruby.RubyHash.newHash;
import static org.jruby.api.Access.getModule;
import static org.jruby.api.Access.symbolClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.checkUnsignedInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.util.RubyStringBuilder.str;

public class TracePoint extends RubyObject {
    public static void createTracePointClass(ThreadContext context, RubyClass Object) {
        defineClass(context, "TracePoint", Object, TracePoint::new).defineMethods(context, TracePoint.class);
    }
    
    public TracePoint(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        
        this.eventName = "";
        this.file = "";
        this.line = -1;
        this.name = "";
        this.type = runtime.getNil();
        this.exception = runtime.getNil();
        this.returnValue = runtime.getNil();
    }
    
    @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] _events, final Block block) {
        ArrayList<RubyEvent> events = new ArrayList<RubyEvent>(_events.length);
        for (int i = 0; i < _events.length; i++) {
            RubySymbol _event = (RubySymbol) TypeConverter.convertToType(context, _events[i], symbolClass(context), sites(context).to_sym);

            String eventName = _event.asJavaString().toUpperCase();
            RubyEvent event = null;
            try {
                event = RubyEvent.valueOf(eventName);
            } catch (IllegalArgumentException iae) {}

            if (event == null) throw argumentError(context, str(context.runtime, "unknown event: ", _event));

            // a_call is call | b_call | c_call, and same as a_return.
            if (event == RubyEvent.A_CALL) {
                events.add(RubyEvent.CALL);
                events.add(RubyEvent.B_CALL);
                events.add(RubyEvent.C_CALL);
            } else if (event == RubyEvent.A_RETURN) {
                events.add(RubyEvent.RETURN);
                events.add(RubyEvent.B_RETURN);
                events.add(RubyEvent.C_RETURN);
            } else {
                events.add(event);
            }
        }
        
        final EnumSet<RubyEvent> eventSet = !events.isEmpty() ?
                EnumSet.copyOf(events) : RubyEvent.ALL_EVENTS_ENUMSET;

        if (!block.isGiven()) throw argumentError(context, "must be called with a block");

        hook = new TracePointEventHook(eventSet, block);
                
        return context.nil;
    }

    private class TracePointEventHook extends EventHook {
        protected ThreadContext threadToTrace;
        private final Block block;
        private final EnumSet<RubyEvent> eventSet;

        public TracePointEventHook(EnumSet<RubyEvent> eventSet, Block block) {
            this.block = block;
            this.eventSet = eventSet;
        }

        @Override
        public void event(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type) {
            event(context, event, file, line, name, type, context.nil);
        }

        @Override
        public void event(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type, IRubyObject value) {
            // Read target before and after enabled. Enabling sets target before enabled and disabling clears it
            // after, so if target changed in between, a targeted enable or disable is running: skip the event
            // instead of firing it without the right filter.
            Target target = TracePoint.this.target;
            if (!enabled || (threadToTrace != null && threadToTrace != context) || context.isWithinTrace()) return;
            if (target != TracePoint.this.target) return;
            if (target != null && !target.covers(event, file, line, name)) return;

            int savedCallInfo = ThreadContext.resetCallInfo(context);

            synchronized (this) {
                inside = true;

                if (type == null) type = context.fals;

                IRubyObject binding;
                if (event == RubyEvent.THREAD_BEGIN || event == RubyEvent.THREAD_END) {
                    binding = context.nil;
                } else {
                    binding = RubyBinding.newBinding(context.runtime, context.currentBinding());
                }

                context.preTrace();

                final IRubyObject errorInfo = context.getErrorInfo();
                update(event.getName(), file, line, name, type, errorInfo, value == null ? context.nil : value, binding);

                // MRI clears $! while a hook runs and restores it after (unless the hook raises)
                context.setErrorInfo(context.nil);
                try {
                    block.yieldSpecific(context, TracePoint.this);
                    context.setErrorInfo(errorInfo);
                } finally {
                    update(null, null, line, null, context.nil, context.nil, context.nil, context.nil);
                    context.postTrace();
                    inside = false;
                    context.callInfo = savedCallInfo;
                }
            }
        }

        @Override
        public void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
            event(context, RubyEvent.fromName(eventName), file, line, name, type);
        }

        @Override
        public boolean isInterestedInEvent(RubyEvent event) {
            return eventSet.contains(event);
        }

        @Override
        public EnumSet<RubyEvent> eventSet() {
            return eventSet;
        }
    }
    
    @JRubyMethod
    public IRubyObject binding(ThreadContext context) {
        checkInside(context);
        
        return binding == null ? context.nil : binding;
    }
    
    @JRubyMethod
    public IRubyObject defined_class(ThreadContext context) {
        checkInside(context);
        
        return type;
    }
    
    @JRubyMethod
    public IRubyObject disable(ThreadContext context, Block block) {
        if (block.isGiven() && target != null) throw argumentError(context, "can't disable a targeting TracePoint in a block");

        return doToggle(context, null, block, false);
    }

    @JRubyMethod(keywords = true)
    public IRubyObject enable(ThreadContext context, Block block) {
        ThreadContext.resetCallInfo(context);
        if (target != null) throw argumentError(context, "can't nest-enable a targeting TracePoint");

        return doToggle(context, null, block, true);
    }

    @CRuby(value = "tracepoint_enable_m", file = "vm_trace.c",
            note = "target_thread: :default does not limit a block form enable to the current thread")
    @JRubyMethod(keywords = true)
    public IRubyObject enable(ThreadContext context, IRubyObject opts, Block block) {
        // A ruby2_keywords Hash at the end of a splat does not set CALL_KEYWORD, but is passed as keywords
        int callInfo = ThreadContext.resetCallInfo(context);
        boolean keywords = (callInfo & ThreadContext.CALL_KEYWORD) != 0 ||
                (callInfo & ThreadContext.CALL_SPLATS) != 0 && opts instanceof RubyHash hash && hash.isRuby2KeywordHash();
        IRubyObject[] args = keywords ?
                ArgsUtil.extractKeywordArgs(context, opts, "target", "target_line", "target_thread") : null;
        if (args == null) throw argumentError(context, 1, 0);

        IRubyObject target = args[0] == null ? context.nil : args[0];
        IRubyObject targetLine = args[1] == null ? context.nil : args[1];

        RubyThread targetThread = null;
        IRubyObject thread = args[2];
        if (thread != null && !thread.isNil() && thread != asSymbol(context, "default")) {
            if (!(thread instanceof RubyThread th)) throw argumentError(context, "target must be a Thread");
            targetThread = th;
        }

        if (target.isNil()) {
            if (!targetLine.isNil()) throw argumentError(context, "only target_line is specified");
            if (this.target != null) throw argumentError(context, "can't nest-enable a targeting TracePoint");

            return doToggle(context, targetThread, block, true);
        }

        // CRuby ignores target_thread for a targeted TracePoint, which fires on every thread
        return enableForTarget(context, target, targetLine, block);
    }

    @CRuby(value = "rb_tracepoint_enable_for_target", file = "vm_trace.c",
            note = "only raises \"can not enable any hooks\" with --debug")
    private synchronized IRubyObject enableForTarget(ThreadContext context, IRubyObject target, IRubyObject targetLine, Block block) {
        IRScope scope = targetScope(context, target);

        if (enabled) throw argumentError(context, "can't nest-enable a targeting TracePoint");

        int line = 0;
        if (!targetLine.isNil()) {
            if (!hook.eventSet().contains(RubyEvent.LINE)) {
                throw argumentError(context, "target_line is specified, but line event is not specified");
            }
            // NUM2UINT in CRuby. Equality on the int bits gives the same result as its unsigned compare.
            // 0 means no line filter.
            line = checkUnsignedInt(context, toLong(context, targetLine));
        }

        EnumMap<RubyEvent, Map<String, Set<Integer>>> found = new EnumMap<>(RubyEvent.class);
        collectTargetEvents(scope, hook.eventSet(), line, found);

        // Without --debug no trace instructions are built and nothing can be found. Enable anyway instead of
        // raising, so code that targets a method still runs. The hook then fires no events.
        if (found.isEmpty() && RubyInstanceConfig.FULL_TRACE_ENABLED) {
            throw argumentError(context, "can not enable any hooks");
        }

        this.target = Target.of(scope.getFile(), found);
        updateEnabled(context, null, true);
        if (!block.isGiven()) return context.fals;

        try {
            return block.yieldSpecific(context);
        } finally {
            updateEnabled(context, null, false);
        }
    }

    private static IRScope targetScope(ThreadContext context, IRubyObject target) {
        IRScope scope = null;

        if (target instanceof RubyProc proc) {
            scope = blockScope(proc);
        } else if (target instanceof AbstractRubyMethod method) {
            DynamicMethod real = method.getMethod().getRealMethod();
            if (real instanceof AbstractIRMethod irMethod) {
                scope = irMethod.getIRScope();
            } else if (real instanceof ProcMethod procMethod) {
                scope = blockScope(procMethod.getProc());
            }
        }

        if (scope == null) throw argumentError(context, "specified target is not supported");

        return scope;
    }

    // Procs from Method#to_proc or Symbol#to_proc have no Ruby code of their own
    private static IRScope blockScope(RubyProc proc) {
        return proc.getBlock().getBody() instanceof IRBlockBody body ? body.getStaticScope().getIRScope() : null;
    }

    /**
     * Add the event, scope name and line of each trace instruction in scope and the scopes nested in it. With a
     * target line, :line events are only added for that line.
     */
    @CRuby(value = "rb_iseq_add_local_tracepoint_recursively", file = "iseq.c",
            note = "records the events to filter on instead of turning on hooks in the code")
    private static void collectTargetEvents(IRScope scope, EnumSet<RubyEvent> events, int targetLine,
                                            EnumMap<RubyEvent, Map<String, Set<Integer>>> found) {
        InterpreterContext ic = scope instanceof IRMethod method ?
                method.lazilyAcquireInterpreterContext() :
                scope.getInterpreterContext();

        if (ic != null) {
            for (Instr instr : ic.getInstructions()) {
                if (!(instr instanceof TraceInstr trace) || !events.contains(trace.getEvent())) continue;

                int line = trace.getLinenumber();
                if (trace.getEvent() == RubyEvent.LINE && targetLine != 0 && targetLine != line) continue;

                found.computeIfAbsent(trace.getEvent(), e -> new HashMap<>())
                        .computeIfAbsent(scopeName(trace.getName()), n -> new HashSet<>())
                        .add(line);
            }
        }

        for (IRScope child : List.copyOf(scope.getLexicalScopes())) {
            collectTargetEvents(child, events, targetLine, found);
        }
    }
    
    @JRubyMethod(name = "enabled?")
    public IRubyObject enabled_p(ThreadContext context) {
        return asBoolean(context, enabled);
    }
    
    @JRubyMethod
    public IRubyObject event(ThreadContext context) {
        checkInside(context);
        
        return eventName == null ? context.nil : asSymbol(context, eventName);
    }
    
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return inside ? // TODO: event-specific inspect output
                newString(context, "#<TracePoint:" + eventName + ">") :
                newString(context, "#<TracePoint:" + (enabled ? "enabled" : "disabled") + ">");
    }
    
    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        checkInside(context);
        
        return asFixnum(context, line);
    }
    
    @JRubyMethod
    public IRubyObject method_id(ThreadContext context) {
        checkInside(context);
        
        return name == null ? context.nil : asSymbol(context, name);
    }

    @JRubyMethod
    public IRubyObject callee_id(ThreadContext context) {
        checkInside(context);

        // TODO: actually get called name, requires modifying trace handling in bindings
        return name == null ? context.nil : asSymbol(context, name);
    }
    
    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        checkInside(context);
        
        return file == null ? context.nil : newString(context, file);
    }
    
    @JRubyMethod
    public IRubyObject raised_exception(ThreadContext context) {
        checkInside(context);
        
        return exception;
    }
    
    @JRubyMethod
    public IRubyObject return_value(ThreadContext context) {
        checkInside(context);

        // FIXME: :c_return events do not pass the return value through yet, so it is always nil for them
        switch (eventName) {
            case "return", "b_return", "c_return" -> {}
            default -> throw runtimeError(context, "not supported by this event");
        }

        return returnValue;
    }
    
    @JRubyMethod
    public IRubyObject self(ThreadContext context) {
        return binding.isNil() ? context.nil : ((RubyBinding)binding).getBinding().getSelf();
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject trace(ThreadContext context, IRubyObject self, IRubyObject[] events, Block block) {
        TracePoint tp = (TracePoint) self.callMethod(context, "new", events, block);
        tp.enable(context, Block.NULL_BLOCK);
        return tp;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject stat(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        var stats = runtime.getTraceEvents().eventHookStats();
        return newHash(runtime,
                getModule(context, "JRuby"),
                newArray(context, asFixnum(context, stats[0]), asFixnum(context, stats[1])));
    }
    
    private void update(String eventName, String file, int line, String name, IRubyObject type, IRubyObject exception, IRubyObject returnValue, IRubyObject binding) {
        this.eventName = eventName;
        this.file = file;
        this.line = line;
        this.name = name;
        this.type = type;
        this.exception = exception;
        this.returnValue = returnValue;
        this.binding = binding;
    }

    private synchronized IRubyObject doToggle(ThreadContext context, RubyThread _targetThread, Block block, boolean toggle) {
        if (block.isGiven()) {
            boolean old = enabled;
            try {
                updateEnabled(context, _targetThread, toggle);
                
                return block.yieldSpecific(context);
            } finally {
                updateEnabled(context, _targetThread, old);
            }
        }
        
        IRubyObject old = asBoolean(context, enabled);
        updateEnabled(context, _targetThread, toggle);
        
        return old;
    }

    public void updateEnabled(ThreadContext context, RubyThread targetThread, boolean toggle) {
        if (toggle == enabled) return; // don't re-add or re-remove hook
        
        enabled = toggle;
        
        if (toggle) {
            if (targetThread != null && !targetThread.isNil()) {
                hook.threadToTrace = targetThread.getContext();
            }
            context.traceEvents.addEventHook(context, hook);
        } else {
            hook.threadToTrace = null;
            target = null;
            context.traceEvents.removeEventHook(hook);
        }
    }

    private void checkInside(ThreadContext context) throws RaiseException {
        if (!inside) throw runtimeError(context, "access from outside");
    }

    private static JavaSites.TracePointSites sites(ThreadContext context) {
        return context.sites.TracePoint;
    }
    
    // The JIT passes a missing name as "null"
    private static String scopeName(String name) {
        return name == null ? "null" : name;
    }

    /**
     * Where a TracePoint enabled with target: fires: for each event, the names of the methods and blocks in the
     * target's code with the sorted lines of their trace instructions.
     *
     * Events are matched by file, event, name and line, not by the code they come from. A method or block with the
     * same name on the same line also matches, such as a method of another class. A :line event in a block has the
     * name of the enclosing method, so it matches other :line events of that method on the same line.
     */
    private record Target(String file, EnumMap<RubyEvent, Map<String, int[]>> lines) {
        static Target of(String file, EnumMap<RubyEvent, Map<String, Set<Integer>>> found) {
            EnumMap<RubyEvent, Map<String, int[]>> lines = new EnumMap<>(RubyEvent.class);
            found.forEach((event, names) -> {
                Map<String, int[]> byName = new HashMap<>();
                names.forEach((name, set) -> byName.put(name, set.stream().mapToInt(Integer::intValue).sorted().toArray()));
                lines.put(event, byName);
            });

            return new Target(file, lines);
        }

        boolean covers(RubyEvent event, String file, int line, String name) {
            Map<String, int[]> byName = lines.get(event);
            if (byName == null || !this.file.equals(file)) return false;

            int[] nameLines = byName.get(scopeName(name));
            return nameLines != null && Arrays.binarySearch(nameLines, line) >= 0;
        }
    }

    private TracePointEventHook hook;
    private volatile Target target;
    private volatile boolean enabled = false;
    private String eventName;
    private String file;
    private int line;
    private String name;
    private IRubyObject type;
    private IRubyObject exception;
    private IRubyObject returnValue;
    private IRubyObject binding;
    private volatile boolean inside = false;
}
