package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.text.DecimalFormat;
import org.jruby.RubyClass;
import org.jruby.MetaClass;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyObject;
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
    private HashMap<Integer, Integer> methodRecursion = new HashMap<Integer, Integer>();
    private long sinceTime = 0;
    
    public static MethodData currentData;
    
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
        
        int recursiveDepth = incRecursionFor(calledMethod);
        Invocation childInvocation = currentInvocation.childInvocationFor(calledMethod, recursiveDepth);
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
        
        decRecursionFor(previousMethod);
        
        currentInvocation = currentInvocation.parent;
        sinceTime     = now;

        return previousMethod;
    }
    
    public void decRecursionFor(int serial) {
        methodRecursion.put(serial, methodRecursion.get(serial) - 1);
    }
    
    public int incRecursionFor(int serial) {
        Integer prev;
        if ((prev = methodRecursion.get(serial)) == null) {
            prev = 0;
        }
        methodRecursion.put(serial, prev + 1);
        return prev + 1;
    }
    
    public long totalTime() {
        return topInvocation.childTime();
    }
    
    public void printProfile(ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out) {
        topInvocation.duration = totalTime();
        out.println(" %total   %self    total        self    children                 calls  name");
        
        HashMap<Integer, MethodData> methods = methodData();
        MethodData[] sortedMethods = methods.values().toArray(new MethodData[0]);
        Arrays.sort(sortedMethods, new Comparator<MethodData>() {
            public int compare(MethodData md1, MethodData md2) {
                long time1 = md1.totalTime();
                long time2 = md2.totalTime();
                return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
            }
        });
        int longestName = 0;
        for (int i = 0; i < profiledNames.length; i++) {
            String name = profiledNames[i];
            if (name == null) {
                continue;
            }
            DynamicMethod method = profiledMethods[i];
            String displayName = moduleHashMethod(method.getImplementationClass(), name);
            longestName = Math.max(longestName, displayName.length());
        }
        for (MethodData data : sortedMethods) {
            GraphProfileData.currentData = data;
            
            out.println("---------------------------------------------------------------------------------------------------------");
            int serial = data.serialNumber;
            
            Integer[] parentSerials = data.parents();
            
            Arrays.sort(parentSerials, new Comparator<Integer>() {
                public int compare(Integer parent1, Integer parent2) {
                    long time1 = GraphProfileData.currentData.rootInvocationsFromParent(parent1).totalTime();
                    long time2 = GraphProfileData.currentData.rootInvocationsFromParent(parent2).totalTime();
                    return time1 == time2 ? 0 : (time1 < time2 ? -1 : 1);
                }
            });
            if (parentSerials.length > 0) {
                for (int parentSerial : parentSerials) {
                    String callerName = methodName(profiledNames, profiledMethods, parentSerial);
                    InvocationSet invs = data.rootInvocationsFromParent(parentSerial);
                    out.print("              ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 20, Integer.toString(data.invocationsFromParent(parentSerial).totalCalls()) + "/" + Integer.toString(data.totalCalls()));
                    out.print("  ");
                    out.print(callerName);
                    out.println("");
                }
            }

            String displayName = methodName(profiledNames, profiledMethods, serial);
            pad(out, 4, Long.toString(data.totalTime()*100/totalTime()));
            out.print("%  ");
            pad(out, 4, Long.toString(data.selfTime()*100/totalTime()));
            out.print("%  ");
            pad(out, 10, nanoString(data.totalTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.selfTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.childTime()));
            out.print("  ");
            pad(out, 20, Integer.toString(data.totalCalls()));
            out.print("  ");
            out.print(displayName);
            out.println("");
            
            Integer[] childSerials = data.children();
            Arrays.sort(childSerials, new Comparator<Integer>() {
                public int compare(Integer child1, Integer child2) {
                    long time1 = GraphProfileData.currentData.rootInvocationsOfChild(child1).totalTime();
                    long time2 = GraphProfileData.currentData.rootInvocationsOfChild(child2).totalTime();
                    return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
                }
            });
            
            if (childSerials.length > 0) {
                for (int childSerial : childSerials) {
                    String callerName = methodName(profiledNames, profiledMethods, childSerial);
                    InvocationSet invs = data.rootInvocationsOfChild(childSerial);
                    out.print("              ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 20, Integer.toString(data.invocationsOfChild(childSerial).totalCalls()) + "/" + Integer.toString(methods.get(childSerial).totalCalls()));
                    out.print("  ");
                    out.print(callerName);
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
        DecimalFormat formatter = new DecimalFormat("##0.00");
        return formatter.format((double) nanoTime / 1.0E9);
    }

    private String methodName(String[] profiledNames, DynamicMethod[] profiledMethods, int serial) {
        if (serial == 0) {
            return "(top)";
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
        if (module instanceof MetaClass) {
            IRubyObject obj = ((MetaClass) module).getAttached();
            if (obj instanceof RubyModule) {
                module = (RubyModule) obj;
                return module.getName() + "." + name;
            }
            else if (obj instanceof RubyObject) {
                return ((RubyObject) obj).getType().getName() + "#" + name;
            }
            else {
                return "unknown#" + name;
            }
        } else if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        } else {
            return module.getName() + "#" + name;
        }
    }
    
}
