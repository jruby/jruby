package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;

public class FlatProfilePrinter extends AbstractProfilePrinter {
    private static final int SERIAL_OFFSET = 0;
    private static final int SELFTIME_OFFSET = 1;
    private static final int COUNT_OFFSET = 2;
    private static final int AGGREGATETIME_OFFSET = 3;

    private ProfileData profileData;
    
    public FlatProfilePrinter(IProfileData iProfileData) {
        profileData = (ProfileData) iProfileData;
    }
    
    public void printProfile(PrintStream out) {
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
