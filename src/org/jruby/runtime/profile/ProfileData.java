/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.profile;

import java.util.HashMap;
import java.util.Map;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.collections.IntHashMap.Entry;

/**
 * Encapsulates the logic of recording and reporting profiled timings of
 * method invocations. This keeps track of aggregate values for callers and
 * callees of each method.
 *
 * See ProfilingDynamicMethod for the "hook" end of profiling.
 */
public class ProfileData implements IProfileData {
    private Invocation currentInvocation = new Invocation(0);
    private Invocation topInvocation = currentInvocation;
    private int[] methodRecursion = new int[1000];
    private ThreadContext threadContext;
    
    public ProfileData(ThreadContext tc) {
        threadContext = tc;
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

    public void clear() {
        methodRecursion = new int[1000];
        currentInvocation = new Invocation(0);
        topInvocation = currentInvocation;
    }
    
    public void decRecursionFor(int serial) {
        ensureRecursionSize(serial);
        int[] mr = methodRecursion;
        mr[serial] = mr[serial] - 1;
    }

    public int incRecursionFor(int serial) {
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

    public long totalTime() {
        return topInvocation.childTime();
    }

    /**
     * @return the topInvocation
     */
    public Invocation getTopInvocation() {
        return topInvocation;
    }

    public Invocation getResults() {
        setRecursiveDepths();
        
        if (topInvocation.getChildren().size() != 1) {
            return addDuration(topInvocation);
        }
        if (topInvocation.getChildren().size() == 1) {
            Invocation singleTopChild = null;
            for (Invocation inv : topInvocation.getChildren().values() ) {
                singleTopChild = inv;
            }
            String singleTopChildName = AbstractProfilePrinter.getMethodName(singleTopChild.getMethodSerialNumber());
            if (singleTopChildName.equals("JRuby::Profiler.profile")) {
                Invocation profiledCodeInvocation = null;
                for (Invocation inv : singleTopChild.getChildren().values() ) {
                    if (AbstractProfilePrinter.getMethodName(inv.getMethodSerialNumber()).equals("JRuby::Profiler.profiled_code")) {
                        return addDuration(inv.copyWithNewSerialAndParent(0, null));
                    }
                }
            }
        }
        return addDuration(topInvocation);
    }
    
    public Invocation addDuration(Invocation inv) {
        inv.setDuration(inv.childTime());
        return inv;
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
}
