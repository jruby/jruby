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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class FlatProfilePrinter extends AbstractProfilePrinter {
    private static final int SERIAL_OFFSET = 0;
    private static final int SELFTIME_OFFSET = 1;
    private static final int COUNT_OFFSET = 2;
    private static final int AGGREGATETIME_OFFSET = 3;
    private final Invocation topInvocation;

    public FlatProfilePrinter(Invocation top) {
        topInvocation = top;
    }

    public void printProfile(PrintStream out) {
        
        out.printf("Total time: %s\n\n", nanoString(topInvocation.getDuration()));

        Map<Integer, MethodData> serialsToMethods = methodData(topInvocation);

        long[][] tuples = new long[serialsToMethods.size()][];

        int j = 0;
        for (Entry<Integer, MethodData> entry : serialsToMethods.entrySet()) {
            MethodData method = entry.getValue();
            tuples[j] = new long[]{entry.getKey(), method.selfTime(), method.totalCalls(), method.totalTime()};
            j++;
        }

        Arrays.sort(tuples, new Comparator<long[]>() {
            public int compare(long[] o1, long[] o2) {
                long o1Val = o1[AGGREGATETIME_OFFSET];
                long o2Val = o2[AGGREGATETIME_OFFSET];
                return o2Val > o1Val ? 1 : (o2Val < o1Val ? -1 : 0);
            }
        });

        out.println("     total        self    children       calls  method");
        out.println("----------------------------------------------------------------");
        int lines = 0;
        for (long[] tuple : tuples) {
            if (tuple[AGGREGATETIME_OFFSET] == 0) {
                break; // if we start hitting zeros, bail out
            }
            int index = (int) tuple[SERIAL_OFFSET];
            if (index != 0) {
                lines++;
                String name = methodName(index);
                pad(out, 10, nanoString(tuple[AGGREGATETIME_OFFSET]));
                out.print("  ");
                pad(out, 10, nanoString(tuple[SELFTIME_OFFSET]));
                out.print("  ");
                pad(out, 10, nanoString(tuple[AGGREGATETIME_OFFSET] - tuple[SELFTIME_OFFSET]));
                out.print("  ");
                pad(out, 10, Long.toString(tuple[COUNT_OFFSET]));
                out.print("  ");
                out.println(name);
            }
            if (lines == 50) {
                break;
            }
        }
    }
}
