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

        int recursiveDepth = incRecursionFor(calledMethod);
        Invocation childInvocation = parentInvocation.childInvocationFor(calledMethod, recursiveDepth);
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
        if (currentInvocation.getMethodSerialNumber() != 0) {
            long now = System.nanoTime();
            long duration = now - startTime;
            Invocation current = currentInvocation;
            
            current.addDuration(duration);
            
            int previousMethod = current.getMethodSerialNumber();
            
            decRecursionFor(previousMethod);
            
            currentInvocation = current.getParent();
            
            return previousMethod;
        }
        else {
            return 0;
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

    public long totalTime() {
        return topInvocation.childTime();
    }

    public Map<Integer, MethodData> methodData() {
        Map<Integer, MethodData> methods = new HashMap();
        MethodData data = new MethodData(0);
        methods.put(0, data);
        data.invocations.add(topInvocation);
        methodData1(methods, topInvocation);
        return methods;
    }

    private static void methodData1(Map<Integer, MethodData> methods, Invocation inv) {
        for (Entry<Invocation> entry : inv.getChildren().entrySet()) {
            Invocation child = entry.getValue();
            int serial = child.getMethodSerialNumber();
            MethodData data = methods.get(serial);
            if (data == null) {
                data = new MethodData(serial);
                methods.put(serial, data);
            }
            data.invocations.add(child);
            methodData1(methods, child);
        }
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
}
