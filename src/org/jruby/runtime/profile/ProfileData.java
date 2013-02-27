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
package org.jruby.runtime.profile;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.collections.IntHashMap;

import static org.jruby.runtime.profile.ProfilePrinter.PROFILER_PROFILE_METHOD;
import static org.jruby.runtime.profile.ProfilePrinter.PROFILER_PROFILED_CODE_METHOD;

/**
 * Encapsulates the logic of recording and reporting profiled timings of
 * method invocations. This keeps track of aggregate values for callers and
 * callees of each method.
 *
 * @see org.jruby.internal.runtime.methods.ProfilingDynamicMethod
 */
public class ProfileData {
    
    private static final int MAX_PROFILE_METHODS = 100000;
    
    private final IntHashMap<ProfiledMethod> profiledMethods = new IntHashMap<ProfiledMethod>(500);
    
    private Invocation currentInvocation;
    private Invocation topInvocation;
    private int[] methodRecursion;
    
    private final ThreadContext threadContext;
    
    public ProfileData(ThreadContext tc) {
        threadContext = tc;
        clear();
    }
    
    public IntHashMap<ProfiledMethod> getProfiledMethods() {
        return profiledMethods;
    }
    
    public void addProfiledMethod(String name, DynamicMethod method) {
        if (method.isUndefined()) return;
        final long serial = method.getSerialNumber();
        if (serial > MAX_PROFILE_METHODS) return;
        
        if (profiledMethods.get((int) serial) == null) {
            profiledMethods.put((int) serial, new ProfiledMethod(name, method));
        }
    }
    
    protected ProfiledMethod getProfiledMethod(final int serial) {
        ProfiledMethod profiledMethod = getProfiledMethods().get(serial);
        if (profiledMethod == null) { // check for the method in the runtime :
            ProfiledMethod[] runtimeMethods = threadContext.getRuntime().getProfiledMethods();
            if (serial < runtimeMethods.length) {
                profiledMethod = runtimeMethods[serial];
            }
        }
        return profiledMethod;
    }
    
    /**
     * Begin profiling a new method, aggregating the current time diff in the previous
     * method's profile slot.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileEnter(int calledMethod) {
        Invocation parentInvocation = currentInvocation;

        Invocation childInvocation = parentInvocation.childInvocationFor(calledMethod);
        childInvocation.incrementCount();

        currentInvocation = childInvocation;
        return parentInvocation.getMethodSerialNumber();
    }
    
    /**
     * Fall back to previously profiled method after current method has returned.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileExit(int callingMethod, long startTime) {
        long now = System.nanoTime();
        long duration = now - startTime;
        int oldSerial = currentInvocation.getMethodSerialNumber();
        currentInvocation.addDuration(duration);
        
        if (currentInvocation == topInvocation) { 
            Invocation newTopInvocation = new Invocation(0);
            Invocation newCurrentInvocation = 
                currentInvocation.copyWithNewSerialAndParent(callingMethod, newTopInvocation);
            
            newTopInvocation.addChild(newCurrentInvocation);
            newCurrentInvocation.incrementCount();
            
            topInvocation     = newTopInvocation;
            currentInvocation = newCurrentInvocation;
            
            return oldSerial;
        } else if (currentInvocation.getParent() == topInvocation && callingMethod != 0) {
            Invocation newTopInvocation = new Invocation(0);
            Invocation newCurrentInvocation = newTopInvocation.childInvocationFor(callingMethod);
            Invocation newChildInvocation = 
                currentInvocation.copyWithNewSerialAndParent(currentInvocation.getMethodSerialNumber(), newCurrentInvocation);
            
            newCurrentInvocation.addChild(newChildInvocation);
            newCurrentInvocation.incrementCount();

            topInvocation = newTopInvocation;
            currentInvocation = newCurrentInvocation;
            return oldSerial;
        }
        else {
            currentInvocation = currentInvocation.getParent();
            
            return oldSerial;
        }
    }

    /**
     * Clear the gathered profiling (invocation) data.
     */
    public void clear() {
        methodRecursion = new int[1000];
        currentInvocation = new Invocation(0);
        topInvocation = currentInvocation;
        
        profiledMethods.clear();
    }

    public long totalTime() {
        return topInvocation.childTime();
    }
    
    /**
     * @return the topInvocation
     */
    public Invocation getTopInvocation() {
        return topInvocation;
    }
    
    /**
     * @return the currentInvocation
     */
    public Invocation getCurrentInvocation() {
        return currentInvocation;
    }

    /**
     * @return the threadContext
     */
    public ThreadContext getThreadContext() {
        return threadContext;
    }
    
    /**
     * Compute the profiling results from gathered data.
     * @return the top invocation
     */
    public Invocation computeResults() {
        setRecursiveDepths();
        
        if (topInvocation.getChildren().size() != 1) {
            return setDuration(topInvocation);
        }
        if (topInvocation.getChildren().size() == 1) {
            Invocation singleTopChild = topInvocation.getChildren().values().iterator().next();
            int serial = singleTopChild.getMethodSerialNumber();
            if ( PROFILER_PROFILE_METHOD.equals( methodName(serial) ) ) {
                for ( Invocation inv : singleTopChild.getChildren().values() ) {
                    serial = inv.getMethodSerialNumber();
                    if ( PROFILER_PROFILED_CODE_METHOD.equals( methodName(serial) ) ) {
                        return setDuration(inv.copyWithNewSerialAndParent(0, null));
                    }
                }
            }
        }
        return setDuration(topInvocation);
    }
    
    private static Invocation setDuration(Invocation inv) {
        inv.setDuration(inv.childTime());
        return inv;
    }
    
    protected void decRecursionFor(int serial) {
        ensureRecursionSize(serial);
        int[] mr = methodRecursion;
        mr[serial] = mr[serial] - 1;
    }

    protected int incRecursionFor(int serial) {
        ensureRecursionSize(serial);
        int[] mr = methodRecursion;
        int inc = mr[serial] + 1;
        mr[serial] = inc;
        return inc;
    }

    private void ensureRecursionSize(int index) {
        int[] mr = methodRecursion;
        int length = mr.length;
        if (length <= index) {
            int[] newRecursion = new int[(int)(index * 1.5 + 1)];
            System.arraycopy(mr, 0, newRecursion, 0, length);
            methodRecursion = newRecursion;
        }
    }
    
    private void setRecursiveDepths() {
        int topSerial = topInvocation.getMethodSerialNumber();
        int depth = incRecursionFor(topSerial);
        topInvocation.setRecursiveDepth(depth);
        setRecursiveDepths1(topInvocation);
    }
    
    private void setRecursiveDepths1(Invocation inv) {
        int depth;
        int childSerial;
        for (Invocation child : inv.getChildren().values()) {
            childSerial = child.getMethodSerialNumber();
            depth = incRecursionFor(childSerial);
            child.setRecursiveDepth(depth);
            setRecursiveDepths1(child);
            
            decRecursionFor(childSerial);
        }
    }
    
    String methodName(final int serial) {
        if (serial == 0) return "(top)";
        return ProfilePrinter.methodName( getProfiledMethod(serial) );
    }
    
}
