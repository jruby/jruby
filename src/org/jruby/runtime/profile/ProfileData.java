package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
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
public class ProfileData {

    private static final int SERIAL_OFFSET = 0;
    private static final int SELFTIME_OFFSET = 1;
    private static final int COUNT_OFFSET = 2;
    private static final int AGGREGATETIME_OFFSET = 3;

    /**
     * Begin profiling a new method, aggregating the current time diff in the previous
     * method's profile slot.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileEnter(int nextMethod) {
        ensureProfileSize(Math.max(current, nextMethod));
        profileCounts[nextMethod]++;
        return aggregateProfileTime(nextMethod, true);
    }

    /**
     * Fall back to previously profiled method after current method has returned.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
    public int profileExit(int nextMethod) {
        ensureProfileSize(Math.max(current, nextMethod));
        return aggregateProfileTime(nextMethod, false);
    }

    private int aggregateProfileTime(int newMethod, boolean entry) {
        long now = System.nanoTime();
        if (entry) {
            profileRecursions[newMethod]++;
            if (profileRecursions[newMethod] == 1) {
                profileAggregateStarts[newMethod] = now;
            }
        } else {
            profileRecursions[current]--;
            if (profileRecursions[current] == 0) {
                profileAggregateTimes[current] += (now - profileAggregateStarts[current]);
            }
        }
        if (current != 0) {
            profileSelfTimes[current] += now - lastTime;
        }
        lastTime = now;
        int oldCurrent = current;
        current = newMethod;
        return oldCurrent;
    }

    /**
     * Ensure the profile times array is large enough to support the given method's
     * serial number.
     *
     * @param method the profiled method's serial number
     */
    private void ensureProfileSize(int method) {
        if (profileSelfTimes.length <= method) {
            long[] newProfileSelfTimes = new long[method * 2 + 1];
            System.arraycopy(profileSelfTimes, 0, newProfileSelfTimes, 0, profileSelfTimes.length);
            profileSelfTimes = newProfileSelfTimes;
            long[] newProfileAggregateTimes = new long[method * 2 + 1];
            System.arraycopy(profileAggregateTimes, 0, newProfileAggregateTimes, 0, profileAggregateTimes.length);
            profileAggregateTimes = newProfileAggregateTimes;
            long[] newProfileAggregateStarts = new long[method * 2 + 1];
            System.arraycopy(profileAggregateStarts, 0, newProfileAggregateStarts, 0, profileAggregateStarts.length);
            profileAggregateStarts = newProfileAggregateStarts;
            int[] newProfileCounts = new int[method * 2 + 1];
            System.arraycopy(profileCounts, 0, newProfileCounts, 0, profileCounts.length);
            profileCounts = newProfileCounts;
            int[] newProfileRecursions = new int[method * 2 + 1];
            System.arraycopy(profileRecursions, 0, newProfileRecursions, 0, profileRecursions.length);
            profileRecursions = newProfileRecursions;
        }
    }

    /**
     * Process the profile data for a given thread (context).
     *
     * @param context the thread (context) for which to dump profile data
     */
    public void printProfile(ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out) {
        long[][] tuples = new long[profileSelfTimes.length][];
        for (int i = 0; i < profileSelfTimes.length; i++) {
            tuples[i] = new long[]{i, profileSelfTimes[i], profileCounts[i], profileAggregateTimes[i]};
        }
        Arrays.sort(tuples, new Comparator<long[]>() {

            public int compare(long[] o1, long[] o2) {
                return ((Long) o2[SELFTIME_OFFSET]).compareTo(o1[SELFTIME_OFFSET]);
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
        out.println("    #            calls             self        aggregate  method");
        out.println("----------------------------------------------------------------");
        int lines = 0;
        for (long[] tuple : tuples) {
            if (tuple[SELFTIME_OFFSET] == 0) {
                break; // if we start hitting zeros, bail out
            }
            lines++;
            int index = (int) tuple[SERIAL_OFFSET];
            String name = profiledNames[index];
            DynamicMethod method = profiledMethods[index];
            String displayName = moduleHashMethod(method.getImplementationClass(), name);
            pad(out, 5, Integer.toString(lines));
            out.print("  ");
            pad(out, 15, Long.toString(tuple[COUNT_OFFSET]));
            out.print("  ");
            pad(out, 15, nanoString(tuple[SELFTIME_OFFSET]));
            out.print("  ");
            pad(out, 15, nanoString(tuple[AGGREGATETIME_OFFSET]));
            out.print("  ");
            out.println(displayName);
            if (lines == 50) {
                break;
            }
        }
    }

    private void pad(PrintStream out, int size, String body) {
        pad(out, size, body, true);
    }

    private void pad(PrintStream out, int size, String body, boolean front) {
        if (front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
        out.print(body);
        if (!front) {
            for (int i = 0; i < size - body.length(); i++) {
                out.print(' ');
            }
        }
    }

    private String nanoString(long nanoTime) {
        return Double.toString((double) nanoTime / 1.0E9) + 's';
    }

    private String moduleHashMethod(RubyModule module, String name) {
        if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        } else {
            return module.getName() + "#" + name;
        }
    }
    private long[] profileSelfTimes = new long[0];
    private long[] profileAggregateTimes = new long[0];
    private long[] profileAggregateStarts = new long[0];
    private int[] profileCounts = new int[0];
    private int[] profileRecursions = new int[0];
    private int current;
    private long lastTime = 0;
}
