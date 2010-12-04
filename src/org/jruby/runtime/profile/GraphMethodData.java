package org.jruby.runtime.profile;

import java.util.HashMap;


/**
 * Contains the caller and callee aggregates for a method. Used when profiling in 'graph' mode. 
 *
 */
public class GraphMethodData {
    public HashMap callerMethodCounts = new HashMap<Integer, Integer>();
    public HashMap callerMethodTimes  = new HashMap<Integer, Long>();
    public HashMap calleeMethodCounts = new HashMap<Integer, Integer>();
    public HashMap calleeMethodTimes  = new HashMap<Integer, Long>();
    
    public int  callCount           = 0;
    public long selfTime            = 0;
    public long totalTime           = 0;
    public int  recursiveDepth      = 0;
    
    public void incCaller(int callerMethod) {
        if (callerMethodCounts.get(callerMethod) == null) {
            callerMethodCounts.put(callerMethod, 0);
        }
        callerMethodCounts.put(callerMethod, ((Integer) callerMethodCounts.get(callerMethod)) + 1);
    }
    
    public void incCallee(int calleeMethod) {
        if (calleeMethodCounts.get(calleeMethod) == null) {
            calleeMethodCounts.put(calleeMethod, 0);
        }
        calleeMethodCounts.put(calleeMethod, ((Integer) calleeMethodCounts.get(calleeMethod)) + 1);
    }
    
    public void addCallerTime(int callerMethod, long duration) {
        if (callerMethodTimes.get(callerMethod) == null) {
            callerMethodTimes.put(callerMethod, (long) 0);
        }
        callerMethodTimes.put(callerMethod, ((Long) callerMethodTimes.get(callerMethod)) + duration);
    }
    
    public void addCalleeTime(int calleeMethod, long duration) {
        if (calleeMethodTimes.get(calleeMethod) == null) {
            calleeMethodTimes.put(calleeMethod, (long) 0);
        }
        calleeMethodTimes.put(calleeMethod, ((Long) calleeMethodTimes.get(calleeMethod)) + duration);
    }
    
}