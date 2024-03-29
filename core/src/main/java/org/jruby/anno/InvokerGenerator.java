/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008-2012 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.anno;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jruby.RubyModule.MethodClumper;
import org.jruby.internal.runtime.methods.DumpingInvocationMethodFactory;
import org.jruby.util.ClassDefiningJRubyClassLoader;

public class InvokerGenerator {

    private static final boolean DEBUG = false;

    private static final Comparator<Method> DETERMINISTIC_METHOD_ORDER = Comparator.comparing(Method::getName)
        .thenComparing(Method::toString); // when there's an overload (same name) compare with parameters included

    public static void main(String[] args) throws Exception {
        final FileReader fileReader;
        try {
            fileReader = new FileReader(args[0]);
        }
        catch (FileNotFoundException e) {
            System.err.println(args[0] + " - not found. skip generator." );
            return;
        }
        BufferedReader br = new BufferedReader(fileReader);

        List<String> classNames = new ArrayList<String>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                classNames.add(line);
            }
        } finally {
            br.close();
        }

        DumpingInvocationMethodFactory dumper = new DumpingInvocationMethodFactory(args[1], new ClassDefiningJRubyClassLoader(ClassLoader.getSystemClassLoader()));

        for (String name : classNames) {
            MethodClumper clumper = new MethodClumper();

            try {
                if (DEBUG) System.err.println("generating for class " + name);
                Class klass = Class.forName(name, false, InvokerGenerator.class.getClassLoader());

                Method[] declaredMethods = klass.getDeclaredMethods();
                Arrays.sort(declaredMethods, DETERMINISTIC_METHOD_ORDER);
                clumper.clump(declaredMethods);

                for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                    dumper.getAnnotatedMethodClass(entry.getValue());
                }

                for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                    dumper.getAnnotatedMethodClass(entry.getValue());
                }
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
                throw e;
            }
        }
        // just delete the input file
        new File(args[0]).delete();
    }
}
