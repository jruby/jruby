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

/**
 * Encapsulates the logic of recording and reporting profiled timings of
 * method invocations. This keeps track of aggregate values for callers and
 * callees of each method.
 *
 * See ProfilingDynamicMethod for the "hook" end of profiling.
 */
public class ProfileData implements IProfileData {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private Invocation currentInvocation = new Invocation(0);
    public Invocation topInvocation = currentInvocation;
    private int[] methodRecursion = EMPTY_INT_ARRAY;
    private long sinceTime = 0;
    public ThreadContext threadContext;
    public static MethodData currentData;

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
        long now = System.nanoTime();

        int recursiveDepth = incRecursionFor(calledMethod);
        Invocation childInvocation = currentInvocation.childInvocationFor(calledMethod, recursiveDepth);
        childInvocation.count++;

        currentInvocation = childInvocation;
        sinceTime = now;
        return parentInvocation.methodSerialNumber;
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

        currentInvocation.duration += duration;

        int previousMethod = currentInvocation.methodSerialNumber;

        decRecursionFor(previousMethod);

        currentInvocation = currentInvocation.parent;
        sinceTime = now;

        return previousMethod;
    }

    public void decRecursionFor(int serial) {
        ensureRecursionSize(serial);
        methodRecursion[serial] = methodRecursion[serial] - 1;
    }

    public int incRecursionFor(int serial) {
        ensureRecursionSize(serial);
        Integer prev;
        if ((prev = methodRecursion[serial]) == null) {
            prev = 0;
        }
        methodRecursion[serial] = prev + 1;
        return prev + 1;
    }

    private void ensureRecursionSize(int index) {
        if (methodRecursion.length <= index) {
            int[] newRecursion = new int[index * 2];
            System.arraycopy(methodRecursion, 0, newRecursion, 0, methodRecursion.length);
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
