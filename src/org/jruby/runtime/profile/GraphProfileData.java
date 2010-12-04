package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

/**
 * Encapsulates the logic of recording and reporting profiled timings of
 * method invocations. This keeps track of aggregate values for callers and
 * callees of each method.
 *
 * See ProfilingDynamicMethod for the "hook" end of profiling.
 */
public class GraphProfileData implements IProfileData {

    private Invocation currentInvocation = new Invocation(0);
    private Invocation topInvocation     = currentInvocation;
    private long sinceTime = 0;
    
    /**
     * Begin profiling a new method, aggregating the current time diff in the previous
     * method's profile slot.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileEnter(int calledMethod) {
        Invocation parentInvocation = currentInvocation;
        long now                    = System.nanoTime();
        
        Invocation childInvocation = currentInvocation.childInvocationFor(calledMethod);
        
        childInvocation.count++;
        
        currentInvocation = childInvocation;
        sinceTime         = now;
        return parentInvocation.methodSerialNumber;
    }
    
    /**
     * Fall back to previously profiled method after current method has returned.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileExit(int callingMethod, long startTime) {
        long now         = System.nanoTime();

        long duration = now - startTime;
        
        currentInvocation.duration += duration;
        
        int previousMethod = currentInvocation.methodSerialNumber;
        currentInvocation = currentInvocation.parent;
        sinceTime     = now;
        return previousMethod;
    }
    
    public long _totalTime = -1;
    public long totalTime() {
        if (_totalTime == -1)
            _totalTime = topInvocation.childTime();
        return _totalTime;
    }
    
    public void printProfile(ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out) {
        topInvocation.duration = totalTime();
        out.println(" %total   %self     total      self    children               calls   Name");
        
        HashMap<Integer, MethodData> methods = methodData();
        
        for (MethodData data : methods.values()) {
        out.println("--------------------------------------------------------------------------");
            int serial = data.serialNumber;
            
            int[] parentSerials = data.parents();
            if (parentSerials.length > 0) {
                for (int parentSerial : parentSerials) {
                    String callerName = methodName(profiledNames, profiledMethods, parentSerial);
                    InvocationSet invs = data.invocationsFromParent(parentSerial);
                    out.print("                 ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 10, Integer.toString(invs.totalCount()));
                    out.print("  ");
                    pad(out, 10, callerName);
                    out.println("");
                }
            }
            int[] childSerials = data.children();

            String displayName = methodName(profiledNames, profiledMethods, serial);
            pad(out, 4, Long.toString(data.totalTime()*100/totalTime()));
            out.print("%  ");
            pad(out, 4, Long.toString(data.selfTime()*100/totalTime()));
            out.print("%     ");
            pad(out, 10, nanoString(data.totalTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.selfTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.childTime()));
            out.print("  ");
            pad(out, 10, Integer.toString(data.calls()));
            out.print("  ");
            pad(out, 10, displayName);
            out.println("");
            
            if (childSerials.length > 0) {
                for (int childSerial : childSerials) {
                    String callerName = methodName(profiledNames, profiledMethods, childSerial);
                    InvocationSet invs = data.invocationsOfChild(childSerial);
                    out.print("                 ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 10, Integer.toString(invs.totalCount()));
                    out.print("  ");
                    pad(out, 10, callerName);
                    out.println("");
                }
            }
        }
    }
    
    private void pad(PrintStream out, int size, String body) {
        pad(out, size, body, true);
    }

    private void pad(PrintStream out, int size, String body, boolean front) {
        if (front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
        out.print(body);
        if (!front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
    }

    private String nanoString(long nanoTime) {
        return Double.toString((double) nanoTime / 1.0E9) + 's';
    }

    private String methodName(String[] profiledNames, DynamicMethod[] profiledMethods, int serial) {
        if (serial == 0) {
            return "#top";
        }
        String name = profiledNames[serial];
        DynamicMethod method = profiledMethods[serial];
        return moduleHashMethod(method.getImplementationClass(), name);
    }
    
    private HashMap<Integer, MethodData> methodData() {
        HashMap<Integer, MethodData> methods = new HashMap<Integer, MethodData>();
        MethodData data = new MethodData(0);
        methods.put(0, data);
        data.invocations.add(topInvocation);
        methodData1(methods, topInvocation);
        return methods;
    }
        
    private void methodData1(HashMap<Integer, MethodData> methods, Invocation inv) {
        for (int serial : inv.children.keySet()) {
            Invocation child = inv.children.get(serial);
            MethodData data = methods.get(child.methodSerialNumber);
            if (data == null) {
                data = new MethodData(child.methodSerialNumber);
                methods.put(child.methodSerialNumber, data);
            }
            data.invocations.add(child);
            methodData1(methods, child);
        }
    }
    
    private String moduleHashMethod(RubyModule module, String name) {
        if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        } else {
            return module.getName() + "#" + name;
        }
    }
    
}
