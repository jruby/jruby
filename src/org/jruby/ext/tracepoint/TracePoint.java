package org.jruby.ext.tracepoint;

import java.util.Arrays;
import java.util.EnumSet;
import org.jruby.Ruby;
import org.jruby.RubyBinding;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class TracePoint extends RubyObject {
    public static void createTracePointClass(Ruby runtime) {
        RubyClass tracePoint = runtime.defineClass("TracePoint", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new TracePoint(runtime, klazz);
            }
        });
        
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
    
    @JRubyMethod(rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] _events, final Block block) {
        final Ruby runtime = context.runtime;
        
        if (!block.isGiven()) throw runtime.newThreadError("must be called with a block");
        
        RubyEvent[] events = new RubyEvent[_events.length];
        for (int i = 0; i < _events.length; i++) {
            IRubyObject _event = _events[i];
            if (_event instanceof RubySymbol || _event instanceof RubyString) {
                String eventName = _event.asJavaString();
                RubyEvent event = RubyEvent.valueOf(eventName);
                
                if (event == null) throw runtime.newArgumentError("unknown event: " + eventName);
                
                events[i] = event;
            }
        }
        
        EnumSet<RubyEvent> _eventSet;
        if (events.length > 0) {
            _eventSet = EnumSet.copyOf(Arrays.asList(events));
        } else {
            _eventSet = EnumSet.allOf(RubyEvent.class);
        }
        
        final EnumSet<RubyEvent> eventSet = _eventSet;
        hook = new EventHook() {
            @Override
            public synchronized void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
                if (!enabled || context.isWithinTrace()) return;
                
                inside = true;
                try {
                    if (file == null) file = "(ruby)";
                    if (type == null) type = context.runtime.getFalse();

                    RubyBinding binding = RubyBinding.newBinding(context.runtime, context.currentBinding());

                    context.preTrace();
                    
                    // FIXME: get return value
                    update(eventName, file, line, name, type, context.getErrorInfo(), context.nil, binding);
                
                    block.yieldSpecific(context, TracePoint.this);
                } finally {
                    update(null, null, line, null, context.nil, context.nil, context.nil, context.nil);
                    context.postTrace();
                    inside = false;
                }
            }

            @Override
            public boolean isInterestedInEvent(RubyEvent event) {
                return eventSet.contains(event);
            }
        };
                
        return context.nil;
    }
    
    @JRubyMethod
    public IRubyObject binding(ThreadContext context) {
        checkInside(context);
        
        return context.nil;
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
        return context.runtime.newBoolean(enabled);
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
        
        return name == null ? context.nil : context.runtime.newString(name);
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
        
        IRubyObject old = context.runtime.newBoolean(enabled);
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
