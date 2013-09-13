/*
 ***** BEGIN LICENSE BLOCK *****
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
package org.jruby.embed.osgi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.File;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.osgi.OSGiScriptingContainer;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author ajuckel
 */
@RunWith(PaxExam.class)
public class JRubyOsgiEmbedTest {
    private static final String SCRIPT_RESULT = "Foo!!!!!!!";

    @Configuration
    public Option[] config() {
        File f = new File("target/jruby-complete.jar");
        return options(junitBundles(), bundle(f.toURI().toString()));
    }

    @Test
    public void testJRubyCreate() throws InterruptedException {
        Bundle b = FrameworkUtil.getBundle(JRubyOsgiEmbedTest.class);
        assertNotNull(b);
        OSGiScriptingContainer scriptingContainer = new OSGiScriptingContainer(b, LocalContextScope.CONCURRENT,
                LocalVariableBehavior.TRANSIENT);
        // Ensure that we can load this class in the created ScriptingContainer.
        String result = (String) scriptingContainer.runScriptlet(String.format(
                "require 'java'; java_import '%s'; %s.new.result.to_java :string", JRubyOsgiEmbedTest.class.getName(),
                JRubyOsgiEmbedTest.class.getSimpleName()));
        assertEquals(getResult(), result);
        // OK, this is super ugly. Pax Exam is sometimes unregistering bundles
        // before they've been fully registered with a quick test like this.
        Thread.sleep(500);
    }

    public String getResult() {
        return SCRIPT_RESULT;
    }
}
