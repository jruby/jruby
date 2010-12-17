package org.jruby.runtime.profile;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

import org.jruby.internal.runtime.methods.DynamicMethod;

public class GraphProfilePrinter extends AbstractProfilePrinter {
    public static MethodData currentData;
    private ProfileData profileData;
    
    public GraphProfilePrinter(IProfileData iProfileData) {
        profileData = (ProfileData) iProfileData;
    }

    public void printProfile(PrintStream out) {
        profileData.topInvocation.duration = profileData.totalTime();
        
        out.println(" %total   %self    total        self    children                 calls  name");
        
        HashMap<Integer, MethodData> methods = profileData.methodData();
        MethodData[] sortedMethods = methods.values().toArray(new MethodData[0]);
        Arrays.sort(sortedMethods, new Comparator<MethodData>() {
            public int compare(MethodData md1, MethodData md2) {
                long time1 = md1.totalTime();
                long time2 = md2.totalTime();
                return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
            }
        });
            
        for (MethodData data : sortedMethods) {
            GraphProfilePrinter.currentData = data;
            
            out.println("---------------------------------------------------------------------------------------------------------");
            int serial = data.serialNumber;
            
            Integer[] parentSerials = data.parents();
            
            Arrays.sort(parentSerials, new Comparator<Integer>() {
                public int compare(Integer parent1, Integer parent2) {
                    long time1 = GraphProfilePrinter.currentData.rootInvocationsFromParent(parent1).totalTime();
                    long time2 = GraphProfilePrinter.currentData.rootInvocationsFromParent(parent2).totalTime();
                    return time1 == time2 ? 0 : (time1 < time2 ? -1 : 1);
                }
            });
            if (parentSerials.length > 0) {
                for (int parentSerial : parentSerials) {
                    String callerName = methodName(parentSerial);
                    InvocationSet invs = data.rootInvocationsFromParent(parentSerial);
                    out.print("              ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 20, Integer.toString(data.invocationsFromParent(parentSerial).totalCalls()) + "/" + Integer.toString(data.totalCalls()));
                    out.print("  ");
                    out.print(callerName);
                    out.println("");
                }
            }

            String displayName = methodName(serial);
            pad(out, 4, Long.toString(data.totalTime()*100/profileData.totalTime()));
            out.print("%  ");
            pad(out, 4, Long.toString(data.selfTime()*100/profileData.totalTime()));
            out.print("%  ");
            pad(out, 10, nanoString(data.totalTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.selfTime()));
            out.print("  ");
            pad(out, 10, nanoString(data.childTime()));
            out.print("  ");
            pad(out, 20, Integer.toString(data.totalCalls()));
            out.print("  ");
            out.print(displayName);
            out.println("");
            
            Integer[] childSerials = data.children();
            Arrays.sort(childSerials, new Comparator<Integer>() {
                public int compare(Integer child1, Integer child2) {
                    long time1 = GraphProfilePrinter.currentData.rootInvocationsOfChild(child1).totalTime();
                    long time2 = GraphProfilePrinter.currentData.rootInvocationsOfChild(child2).totalTime();
                    return time1 == time2 ? 0 : (time1 < time2 ? 1 : -1);
                }
            });
            
            if (childSerials.length > 0) {
                for (int childSerial : childSerials) {
                    String callerName = methodName(childSerial);
                    InvocationSet invs = data.rootInvocationsOfChild(childSerial);
                    out.print("              ");
                    pad(out, 10, nanoString(invs.totalTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.selfTime()));
                    out.print("  ");
                    pad(out, 10, nanoString(invs.childTime()));
                    out.print("  ");
                    pad(out, 20, Integer.toString(data.invocationsOfChild(childSerial).totalCalls()) + "/" + Integer.toString(methods.get(childSerial).totalCalls()));
                    out.print("  ");
                    out.print(callerName);
                    out.println("");
                }
            }
        }
    }
    
}
