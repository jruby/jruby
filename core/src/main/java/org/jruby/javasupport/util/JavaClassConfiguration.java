/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

package org.jruby.javasupport.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for Reified classes, both normal, and java concrete extension
 */
public class JavaClassConfiguration implements Cloneable {
    private static final Set<String> DEFAULT_EXCLUDES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("class", "finalize", "initialize", "java_class", "java_object",
                    "__jcreate!", "java_interfaces", "java_proxy_class", "java_proxy_class=")));

    // general
    public Map<String, List<Map<Class<?>, Map<String, Object>>>> parameterAnnotations;
    public Map<String, Map<Class<?>, Map<String, Object>>> methodAnnotations;
    public Map<String, Map<Class<?>, Map<String, Object>>> fieldAnnotations;
    public Map<Class<?>, Map<String, Object>> classAnnotations;
    public Map<String, List<Class<?>[]>> methodSignatures;
    public Map<String, Class<?>> fieldSignatures;

    public boolean callInitialize = true;
    public boolean allMethods = true;
    public boolean allClassMethods = true; // TODO: ensure defaults are sane
    public boolean javaConstructable = true;
    public List<Class<?>[]> extraCtors = new ArrayList<>();

    // for java proxies
    public boolean allCtors = false;
    public boolean rubyConstructable = true; //
    public boolean IroCtors = true;

    public Map<String, String> renamedMethods = new HashMap<>();
    public String javaCtorMethodName = "initialize";
    private Set<String> excluded = null;
    private Set<String> included = null;

    public JavaClassConfiguration clone() {
        JavaClassConfiguration other = new JavaClassConfiguration();
        if (excluded != null) other.excluded = new HashSet<>(excluded);
        if (included != null) other.included = new HashSet<>(included);
        other.javaCtorMethodName = javaCtorMethodName;

        other.IroCtors = IroCtors;
        other.rubyConstructable = rubyConstructable;
        other.allCtors = allCtors;

        other.javaConstructable = javaConstructable;
        other.allClassMethods = allClassMethods;
        other.allMethods = allMethods;
        other.callInitialize = callInitialize;

        other.renamedMethods = new HashMap<>(renamedMethods);
        other.extraCtors = new ArrayList<>(extraCtors); // NOTE: doesn't separate the arrays, is that fine?

        if (parameterAnnotations != null) other.parameterAnnotations = new HashMap<>(parameterAnnotations); // TOOD:
                                                                                                            // deep
                                                                                                            // clone
        if (methodAnnotations != null) other.methodAnnotations = new HashMap<>(methodAnnotations); // TOOD: deep clone
        if (fieldAnnotations != null) other.fieldAnnotations = new HashMap<>(fieldAnnotations); // TOOD: deep clone
        if (classAnnotations != null) other.classAnnotations = new HashMap<>(classAnnotations); // TOOD: deep clone
        if (methodSignatures != null) other.methodSignatures = new HashMap<>(methodSignatures); // TOOD: deep clone
        if (fieldSignatures != null) other.fieldSignatures = new HashMap<>(fieldSignatures); // TOOD: deep clone

        return other;
    }

    public synchronized Set<String> getExcluded() {
        if (excluded == null) return DEFAULT_EXCLUDES;

        return excluded;
    }

    public synchronized void exclude(String name) {
        if (included == null) included = new HashSet<>();
        if (excluded == null) excluded = new HashSet<>(DEFAULT_EXCLUDES);

        excluded.add(name);
        included.remove(name);
    }

    public synchronized Set<String> getIncluded() {
        if (included == null) return Collections.EMPTY_SET;

        return included;
    }

    public synchronized void include(String name) {
        if (included == null) included = new HashSet<>();
        if (excluded == null) excluded = new HashSet<>(DEFAULT_EXCLUDES);

        included.add(name);
        excluded.remove(name);
    }
}
