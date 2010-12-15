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
public class ProfileData implements IProfileData {

    private Invocation currentInvocation = new Invocation(0);
    public Invocation topInvocation     = currentInvocation;
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
    
    public HashMap<Integer, MethodData> methodData() {
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
}
