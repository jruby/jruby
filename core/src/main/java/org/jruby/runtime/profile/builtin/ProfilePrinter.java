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
package org.jruby.runtime.profile.builtin;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig.ProfilingMode;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.collections.IntHashMap.Entry;

import java.io.PrintStream;
import java.text.DecimalFormat;

public abstract class ProfilePrinter {
    
    /**
     * Printer implementation factory for supported profiling modes.
     * @param mode the profiling mode
     * @param profileData
     * @param runtime
     * @see Ruby#printProfileData(ProfileData)
     */
    public static ProfilePrinter newPrinter(ProfilingMode mode, ProfileData profileData) {
        return newPrinter(mode, profileData, null);
    }

    static ProfilePrinter newPrinter(ProfilingMode mode, ProfileData profileData, Invocation topInvocation) {
        final ProfilePrinter printer;
        if (topInvocation == null) topInvocation = profileData.computeResults();
        if (mode == ProfilingMode.FLAT) {
            printer = new FlatProfilePrinter(profileData, topInvocation);
        }
        else if (mode == ProfilingMode.GRAPH) {
            printer = new GraphProfilePrinter(profileData, topInvocation);
        }
        else if (mode == ProfilingMode.HTML) {
            printer = new HtmlProfilePrinter(profileData, topInvocation);
        }
        else if (mode == ProfilingMode.JSON) {
            printer = new JsonProfilePrinter(profileData, topInvocation);
        }
        else {
            printer = null;
        }
        return printer;
    }
    
    private final ProfileData profileData;
    private final Invocation topInvocation;
    
    protected ProfilePrinter(ProfileData profileData) {
        this(profileData, profileData.computeResults());
    }

    protected ProfilePrinter(ProfileData profileData, Invocation topInvocation) {
        this.profileData = profileData;
        this.topInvocation = topInvocation;
    }

    public ProfileData getProfileData() {
        return profileData;
    }
    
    protected Invocation getTopInvocation() {
        return topInvocation;
    }

    public void printHeader(PrintStream out) { }
    public void printFooter(PrintStream out) { }
    
    public void printProfile(PrintStream out) {
        printProfile(out, true);
    }

    public abstract void printProfile(PrintStream out, boolean first) ;

    public void printProfile(RubyIO out) {
        printProfile(new PrintStream(out.getOutStream()));
    }

    boolean isProfilerInvocation(Invocation inv) {
        return isThisProfilerInvocation(inv.getMethodSerialNumber()) || 
                (inv.getParent() != null && isProfilerInvocation(inv.getParent()));
    }
    
    boolean isThisProfilerInvocation(int serial) {
        final String start = PROFILER_START_METHOD;
        final String stop = PROFILER_STOP_METHOD;
        
        final String name = methodName(serial);
        return ( name.hashCode() == start.hashCode() && name.equals(start) ) ||
               ( name.hashCode() == stop.hashCode() && name.equals(stop) );
    }

    public String getThreadName() {
        if (getProfileData().getThreadContext().getThread() == null) {
            return Thread.currentThread().getName();
        } else {
            return getProfileData().getThreadContext().getThread().getNativeThread().getName();
        }
    }

    public String methodName(int serial) {
        return profileData.methodName(serial);
    }

    static String methodName(ProfiledMethod profileMethod) {
        final String displayName;
        if (profileMethod != null) {
            String name = profileMethod.getName();
            DynamicMethod method = profileMethod.getMethod();
            if (name == null) name = method.getName();
            displayName = moduleHashMethod(method.getImplementationClass(), name);
        } else {
            displayName = "<unknown>";
        }
        // System.out.printf("%d - %s\n", serial, displayName);
        return displayName;
    }
    
    protected static IntHashMap<MethodData> methodData(Invocation top) {
        IntHashMap<MethodData> methods = new IntHashMap<MethodData>();
        MethodData data = new MethodData(0);
        methods.put(0, data);
        data.invocations.add(top);
        methodData1(methods, top);
        return methods;
    }

    private static void methodData1(final IntHashMap<MethodData> methods, Invocation inv) {
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

    private static final String PROFILER_START_METHOD = "JRuby::Profiler.start";
    private static final String PROFILER_STOP_METHOD = "JRuby::Profiler.stop";
    
    /*
     * Here to keep these in one place if the hash format gets updated
     * @see ProfileData#computeResults()
     */
    static final String PROFILER_PROFILE_METHOD = "JRuby::Profiler.profile";
    static final String PROFILER_PROFILED_CODE_METHOD = "JRuby::Profiler.profiled_code";
    
    private static String moduleHashMethod(RubyModule module, String name) {
        if (module instanceof MetaClass) {
            IRubyObject obj = ((MetaClass) module).getAttached();
            if (obj instanceof RubyModule) {
                return ((RubyModule) obj).getName() + "." + name;
            } 
            if (obj instanceof RubyObject) {
                return ((RubyObject) obj).getType().getName() + "(singleton)#" + name;
            }
            return "unknown#" + name;
        }
        if (module.isSingleton()) {
            return ((RubyClass) module).getRealClass().getName() + "(singleton)#" + name;
        }
        if (module instanceof RubyClass) {
            return module.getName() + "#" + name; // instance method
        }
        return module.getName() + "." + name; // module method
    }
    
    protected static void pad(PrintStream out, int size, String body) {
        pad(out, size, body, true);
    }
    
    protected static void pad(PrintStream out, int size, String body, boolean front) {
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

    protected static String nanoString(long nanoTime) {
        DecimalFormat formatter = new DecimalFormat("##0.00");
        return formatter.format((double) nanoTime / 1.0E9);
    }
    
}
