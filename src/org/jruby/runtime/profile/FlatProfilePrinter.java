package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

/**
 * Encapsulates the logic of recording and reporting profiled timings of
 * method invocations.
 *
 * Profile data is stored in a set of arrays, indexed by the serial number of
 * the method in question. This helps keep the cost of profiling to a minimum,
 * since only primitive int/long array read/writes are required to update the tally
 * for a given method.
 *
 * See ProfilingDynamicMethod for the "hook" end of profiling.
 */
public class FlatProfilePrinter extends AbstractProfilePrinter {
     private static final int SERIAL_OFFSET = 0;
     private static final int SELFTIME_OFFSET = 1;
     private static final int COUNT_OFFSET = 2;
     private static final int AGGREGATETIME_OFFSET = 3;

    /**
     * Process the profile data for a given thread (context).
     *
     * @param context the thread (context) for which to dump profile data
     */
    public void printProfile(IProfileData iProfileData, ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out) {
        ProfileData profileData = (ProfileData) iProfileData;
        profileData.topInvocation.duration = profileData.totalTime();
        
        out.printf("Total time: %s\n\n", nanoString(profileData.totalTime()));
        
        HashMap<Integer, MethodData> serialsToMethods = profileData.methodData();
        
        long[][] tuples = new long[serialsToMethods.size()][];
        
        int j = 0;
        for (int serial : serialsToMethods.keySet()) {
            MethodData method = serialsToMethods.get(serial);
            tuples[j] = new long[]{serial, method.selfTime(), method.totalCalls(), method.totalTime()};
            j++;
        }
        
        Arrays.sort(tuples, new Comparator<long[]>() {
            public int compare(long[] o1, long[] o2) {
                return ((Long) o2[AGGREGATETIME_OFFSET]).compareTo(o1[AGGREGATETIME_OFFSET]);
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
                String name = methodName(profiledNames, profiledMethods, index);
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
