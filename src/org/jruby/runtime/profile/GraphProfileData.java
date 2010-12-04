package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

    private HashMap methods = new HashMap<Integer, GraphMethodData>();

    private int currentMethod = 0;
    private long sinceTime = 0;
    
    /**
     * Begin profiling a new method, aggregating the current time diff in the previous
     * method's profile slot.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileEnter(int calledMethod) {
        int callingMethod = currentMethod;
        long now          = System.nanoTime();
        
        GraphMethodData calledMethodData = getMethodData(calledMethod);
        calledMethodData.recursiveDepth++;
        calledMethodData.callCount++;
        calledMethodData.incCaller(callingMethod);
        
        GraphMethodData callingMethodData = getMethodData(callingMethod);
        callingMethodData.incCallee(calledMethod);
        callingMethodData.selfTime += (now - sinceTime)/1000;

        currentMethod = calledMethod;
        sinceTime     = now;
        return callingMethod;
    }
    
    /**
     * Fall back to previously profiled method after current method has returned.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileExit(int callingMethod, long startTime) {
        int calledMethod = currentMethod;
        
        long now      = System.nanoTime();
        long duration = (now - startTime)/1000;
        
        GraphMethodData callingMethodData = getMethodData(callingMethod);
        callingMethodData.addCalleeTime(calledMethod, duration);
        
        GraphMethodData calledMethodData = getMethodData(calledMethod);
        calledMethodData.recursiveDepth--;
        if (calledMethodData.recursiveDepth == 0) {
            calledMethodData.totalTime += duration;
        }
        calledMethodData.addCallerTime(callingMethod, duration);
        calledMethodData.selfTime += (now - sinceTime)/1000;
        
        currentMethod = callingMethod;
        sinceTime     = now;
        return calledMethod;
    }

    public void printProfile(ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out) {
        out.println("    #            calls             self        aggregate  method");
        out.println("----------------------------------------------------------------");
        
        Set<Integer> methodSerialNumbers = methods.keySet();
        for (int serialNumber : methodSerialNumbers) {
            if (serialNumber != 0) {
                String name                = profiledNames[serialNumber];
                DynamicMethod method       = profiledMethods[serialNumber];
                GraphMethodData methodData = getMethodData(serialNumber);
                String displayName         = moduleHashMethod(method.getImplementationClass(), name);
                
                out.printf("%s, serial: %d, count: %d, time: %d, self: %d\n", displayName, serialNumber, methodData.callCount, methodData.totalTime, methodData.selfTime);
                out.println("  callers:");
                Set<Integer> callerSerials = methodData.callerMethodCounts.keySet();
                for (int i : callerSerials) {
                    if (i != 0) {
                        String n          = profiledNames[i];
                        DynamicMethod m   = profiledMethods[i];
                        String dp         = moduleHashMethod(m.getImplementationClass(), n);
                        int count         = (Integer) methodData.callerMethodCounts.get(i);
                        long time         = (Long) methodData.callerMethodTimes.get(i);
                        out.printf("    %s, count: %d, time: %d\n", dp, count, time);
                    }
                }
                
                out.println("  callees:");
                Set<Integer> calleeSerials = methodData.calleeMethodCounts.keySet();
                for (int i : calleeSerials) {
                    if (i != 0) {
                        String n          = profiledNames[i];
                        DynamicMethod m   = profiledMethods[i];
                        String dp         = moduleHashMethod(m.getImplementationClass(), n);
                        int count         = (Integer) methodData.calleeMethodCounts.get(i);
                        long time         = (Long) methodData.calleeMethodTimes.get(i);
                        out.printf("    %s, count: %d, time: %d\n", dp, count, time);
                    }
                }
            }
        }
    }
    
    private String moduleHashMethod(RubyModule module, String name) {
        if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        } else {
            return module.getName() + "#" + name;
        }
    }
    
    private GraphMethodData getMethodData(int method) {
        GraphMethodData methodData = (GraphMethodData) methods.get(method);
        if (methodData == null) {
            methodData = new GraphMethodData();
            methods.put(method, methodData);
        }
        return methodData;
    }

}
