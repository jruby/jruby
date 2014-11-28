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
import java.net.URL;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.IsolatedScriptingContainer;
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

    @Configuration
    public Option[] config() {
        File f = new File("target/osgi-test.jar");
        return options(junitBundles(), bundle(f.toURI().toString()));
    }

    @Test
    public void testJRubyCreate() throws InterruptedException {

        System.err.println();
        System.err.println();

        // System.setProperty( "jruby.debug.loadService", "true" );
        IsolatedScriptingContainer jruby = new IsolatedScriptingContainer();

        // run a script from LOAD_PATH
        String hello = (String) jruby.runScriptlet( "require 'hello'; Hello.say" );
        assertEquals( hello, "world" );

        System.err.println();
        System.err.println();

        // ensure we can load rake from the default gems
        boolean loaded = (Boolean) jruby.runScriptlet( "require 'rake'" );
        assertEquals(true, loaded);

        String list = (String) jruby.runScriptlet( "Gem.loaded_specs.keys.inspect" );
        assertEquals(list, "[\"rake\"]");

        // ensure we can load openssl (with its bouncy-castle jars)
        loaded = (Boolean) jruby.runScriptlet( "require 'openssl'" );
        assertEquals(true, loaded);

        // ensure we can load ffi
        loaded = (Boolean) jruby.runScriptlet( "require 'ffi'" );
	// false is good enough, i.e. it is already loaded
        assertEquals(false, loaded);

        String gemPath = (String) jruby.runScriptlet( "Gem::Specification.dirs.inspect" );
        gemPath = gemPath.replaceAll( "bundle[^:]*://[^/]*", "bundle:/" );
        assertEquals( gemPath, "[\"uri:classloader:/specifications\", \"uri:classloader:/META-INF/jruby.home/lib/ruby/gems/shared/specifications\"]" );

        jruby.runScriptlet( "require 'jar-dependencies'; require 'krypt'" );
        list = (String) jruby.runScriptlet( "Gem.loaded_specs.keys.inspect" );
        assertEquals(list, "[\"rake\", \"jruby-openssl\", \"jar-dependencies\", \"ffi\", \"krypt-provider-jdk\", \"krypt-core\", \"krypt\"]");

        // ensure we can load can load embedded gems
        loaded = (Boolean) jruby.runScriptlet( "require 'virtus'" );
        assertEquals(true, loaded);

	list = (String) jruby.runScriptlet( "Gem.loaded_specs.keys.inspect" );
        assertEquals(list, "[\"rake\", \"jruby-openssl\", \"jar-dependencies\", \"ffi\", \"krypt-provider-jdk\", \"krypt-core\", \"krypt\", \"thread_safe\", \"descendants_tracker\", \"equalizer\", \"coercible\", \"ice_nine\", \"axiom-types\", \"virtus\"]");
    }
}
