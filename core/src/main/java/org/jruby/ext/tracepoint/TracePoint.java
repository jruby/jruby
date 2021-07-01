package org.jruby.ext.tracepoint;

import java.util.ArrayList;
import java.util.EnumSet;
import org.jruby.Ruby;
import org.jruby.RubyBinding;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static org.jruby.util.RubyStringBuilder.str;

public class TracePoint extends RubyObject {
    public static void createTracePointClass(Ruby runtime) {
        RubyClass tracePoint = runtime.defineClass("TracePoint", runtime.getObject(), TracePoint::new);
        
        tracePoint.defineAnnotatedMethods(TracePoint.class);
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
        final Ruby runtime = context.runtime;
        
        ArrayList<RubyEvent> events = new ArrayList<RubyEvent>(_events.length);
        for (int i = 0; i < _events.length; i++) {
            RubySymbol _event = (RubySymbol) TypeConverter.convertToType(context, _events[i], runtime.getSymbol(), sites(context).to_sym);

            String eventName = _event.asJavaString().toUpperCase();
            RubyEvent event = null;
            try {
                event = RubyEvent.valueOf(eventName);
            } catch (IllegalArgumentException iae) {}

            if (event == null) throw runtime.newArgumentError(str(runtime, "unknown event: ", _event));

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
        
        final EnumSet<RubyEvent> eventSet;
        if (events.size() > 0) {
            eventSet = EnumSet.copyOf(events);
        } else {
            eventSet = RubyEvent.ALL_EVENTS_ENUMSET;
        }

        if (!block.isGiven()) throw runtime.newArgumentError("must be called with a block");

        hook = new EventHook() {
            @Override
            public void event(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type) {
                if (!enabled || context.isWithinTrace()) return;

                synchronized (this) {
                    inside = true;

                    if (file == null) file = "(ruby)";
                    if (type == null) type = context.fals;

                    IRubyObject binding;
                    if (event == RubyEvent.THREAD_BEGIN || event == RubyEvent.THREAD_END) {
                        binding = context.nil;
                    } else {
                        binding = RubyBinding.newBinding(context.runtime, context.currentBinding());
                    }

                    context.preTrace();

                    // FIXME: get return value
                    update(event.getName(), file, line, name, type, context.getErrorInfo(), context.nil, binding);

                    try {
                        block.yieldSpecific(context, TracePoint.this);
                    } finally {
                        update(null, null, line, null, context.nil, context.nil, context.nil, context.nil);
                        context.postTrace();
                        inside = false;
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
        };
                
        return context.nil;
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
        return doToggle(context, block, false);
    }
    
    @JRubyMethod
    public IRubyObject enable(ThreadContext context, Block block) {
        return doToggle(context, block, true);
    }
    
    @JRubyMethod(name = "enabled?")
    public IRubyObject enabled_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, enabled);
    }
    
    @JRubyMethod
    public IRubyObject event(ThreadContext context) {
        checkInside(context);
        
        return eventName == null ? context.nil : context.runtime.newSymbol(eventName);
    }
    
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        if (inside) {
            // TODO: event-specific inspect output
            return context.runtime.newString("#<TracePoint:" + eventName + ">");
        }
        
        return context.runtime.newString("#<TracePoint:" + (enabled ? "enabled" : "disabled") + ">");
    }
    
    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        checkInside(context);
        
        return context.runtime.newFixnum(line);
    }
    
    @JRubyMethod
    public IRubyObject method_id(ThreadContext context) {
        checkInside(context);
        
        return name == null ? context.nil : context.runtime.newSymbol(name);
    }

    @JRubyMethod
    public IRubyObject callee_id(ThreadContext context) {
        checkInside(context);

        // TODO: actually get called name, requires modifying trace handling in bindings

        return name == null ? context.nil : context.runtime.newSymbol(name);
    }
    
    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        checkInside(context);
        
        return file == null ? context.nil : context.runtime.newString(file);
    }
    
    @JRubyMethod
    public IRubyObject raised_exception(ThreadContext context) {
        checkInside(context);
        
        return exception;
    }
    
    @JRubyMethod
    public IRubyObject return_value(ThreadContext context) {
        checkInside(context);
        
        // FIXME: get return value
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
    
    private void update(String eventName, String file, int line, String name, IRubyObject type, IRubyObject exception, IRubyObject returnValue, IRubyObject binding) {
        // TODO: missing exception, self, return value
        this.eventName = eventName;
        this.file = file;
        this.line = line;
        this.name = name;
        this.type = type;
        this.exception = exception;
        this.returnValue = returnValue;
        this.binding = binding;
    }

    private synchronized IRubyObject doToggle(ThreadContext context, Block block, boolean toggle) {
        if (block.isGiven()) {
            boolean old = enabled;
            try {
                updateEnabled(context, toggle);
                
                return block.yieldSpecific(context);
            } finally {
                updateEnabled(context, old);
            }
        }
        
        IRubyObject old = RubyBoolean.newBoolean(context, enabled);
        updateEnabled(context, toggle);
        
        return old;
    }

    public void updateEnabled(ThreadContext context, boolean toggle) {
        if (toggle == enabled) return; // don't re-add or re-remove hook
        
        enabled = toggle;
        
        if (toggle) {
            context.runtime.addEventHook(hook);
        } else {
            context.runtime.removeEventHook(hook);
        }
    }

    private void checkInside(ThreadContext context) throws RaiseException {
        if (!inside) throw context.runtime.newRuntimeError("access from outside");
    }

    private static JavaSites.TracePointSites sites(ThreadContext context) {
        return context.sites.TracePoint;
    }
    
    private EventHook hook;
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
