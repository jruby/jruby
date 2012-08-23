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


import java.io.PrintStream;
import java.util.Iterator;

import org.jruby.util.collections.IntHashMap;


public class JsonProfilePrinter extends ProfilePrinter {

    public JsonProfilePrinter(ProfileData profileData) {
        super(profileData);
    }
    
    JsonProfilePrinter(ProfileData profileData, Invocation topInvocation) {
        super(profileData, topInvocation);
    }

    public void printProfile(PrintStream out) {
        Invocation topInvocation = getTopInvocation();
        IntHashMap<MethodData> methods = methodData(topInvocation);
        String threadName = getProfileData().getThreadContext().getThread().getNativeThread().getName();

        out.printf("{\n\t\"total_time\":%d,\n\t\"thread_name\":\"%s\",\n\t\"methods\":[\n", topInvocation.getDuration(), threadName);

        Iterator<MethodData> i = methods.values().iterator();
        while (i.hasNext()) {
            MethodData method = i.next();
            out.print("\t\t");
            out.print(methodToJson(method));
            if (i.hasNext()) {
                out.print(",");
            }
            out.println();
        }

        out.print("\t]\n}\n");
    }

    private String methodToJson(MethodData method) {
        return toJsonObject(
            "id", quote(method.serialNumber),
            "name", quote(methodName(method.serialNumber)),
            "total_calls", String.valueOf(method.totalCalls()),
            "total_time", String.valueOf(method.totalTime()),
            "self_time", String.valueOf(method.selfTime()),
            "child_time", String.valueOf(method.childTime()),
            "parents", parentCallsToJson(method),
            "children", childCallsToJson(method)
        );
    }

    private String parentCallsToJson(MethodData method) {
        if (method.serialNumber == 0) {
            return toJsonArray(new String[] { });
        } else {
            int[] parentSerials = method.parents();
            String[] parentCalls = new String[parentSerials.length];
            for (int i = 0; i < parentSerials.length; i++) {
                parentCalls[i] = callToJson(
                    parentSerials[i],
                    method.invocationsFromParent(parentSerials[i]).totalCalls(),
                    method.rootInvocationsFromParent(parentSerials[i])
                );
            }
            return toJsonArray(parentCalls);
        }
    }

    private String childCallsToJson(MethodData method) {
        int[] childSerials = method.children();
        String[] childCalls = new String[childSerials.length];
        for (int i = 0; i < childSerials.length; i++) {
            childCalls[i] = callToJson(
                childSerials[i],
                method.invocationsOfChild(childSerials[i]).totalCalls(),
                method.rootInvocationsOfChild(childSerials[i])
            );
        }
        return toJsonArray(childCalls);
    }

    private String callToJson(int serial, int calls, InvocationSet invocations) {
        return toJsonObject(
            "id", quote(serial),
            "total_calls", String.valueOf(calls),
            "total_time", String.valueOf(invocations.totalTime()),
            "self_time", String.valueOf(invocations.selfTime()),
            "child_time", String.valueOf(invocations.childTime())
        );
    }

    private String quote(String str) {
        return String.format("\"%s\"", str);
    }

    private String quote(int num) {
        return String.format("\"%d\"", num);
    }

    private String quote(long num) {
        return String.format("\"%d\"", num);
    }

    private String toJsonArray(String... values) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (String v : values) {
            buffer.append(v);
            if (v != values[values.length - 1]) {
                buffer.append(",");
            }
        }
        buffer.append("]");
        return buffer.toString();
    }

    private String toJsonObject(String... keysAndValues) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            buffer.append(quote(keysAndValues[i]));
            buffer.append(":");
            buffer.append(keysAndValues[i + 1]);
            if (i < keysAndValues.length - 3) {
                buffer.append(",");
            }
        }
        buffer.append("}");
        return buffer.toString();
    }
}