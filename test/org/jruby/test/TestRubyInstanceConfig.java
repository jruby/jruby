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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.test;

import java.net.URLClassLoader;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.ClassCache;

/**
 * This should be filled up with more tests for RubyInstanceConfig later
 */
public class TestRubyInstanceConfig extends TestRubyBase {
    public TestRubyInstanceConfig(String name) {
        super(name);
    }
    
    private RubyInstanceConfig config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new RubyInstanceConfig();
    }

    public void testRubyInstanceConfigDefaults() throws Exception {
        assertEquals(RubyInstanceConfig.LoadServiceCreator.DEFAULT, config.getLoadServiceCreator());
        assertFalse(config.isInlineScript());
        assertNull(config.getScriptFileName());
        assertEquals(new ArrayList<String>(), config.loadPaths());
        assertEquals(new ArrayList<String>(), config.requiredLibraries());
    }

    protected final static class NullLoadService extends LoadService {
        public NullLoadService(Ruby runtime) {
            super(runtime);
        }
    }

    public void testRubyInstanceConfigOverriding() throws Exception {
        final boolean[] called = new boolean[1];
        config.setLoadServiceCreator(new RubyInstanceConfig.LoadServiceCreator() {
                public LoadService create(Ruby runtime) {
                    called[0] = true;
                    return new NullLoadService(runtime);
                }
            });
        Ruby ruby = Ruby.newInstance(config);
        assertTrue(called[0]);
        assertEquals(NullLoadService.class, ruby.getLoadService().getClass());
    }

    public void testSettingNewLoaderWillCreateNewClassLoader() throws Exception {
        ClassLoader beforeCL = config.getLoader();
        ClassCache beforeCC = config.getClassCache();
        config.setLoader(beforeCL);
        assertTrue("setting a new classloader that is the same, should not create a new classcache", beforeCC == config.getClassCache());

        config.setLoader(new URLClassLoader(new java.net.URL[0], beforeCL));
        assertTrue("setting a new classloader this is different, should create a new classcache", beforeCC != config.getClassCache());
    }
}
