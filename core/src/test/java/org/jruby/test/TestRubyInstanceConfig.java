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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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

package org.jruby.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jruby.exceptions.MainExitException;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.platform.Platform;
import org.jruby.runtime.load.LoadService;

/**
 * This should be filled up with more tests for RubyInstanceConfig later
 */
public class TestRubyInstanceConfig extends Base {
    public TestRubyInstanceConfig(String name) {
        super(name);
    }

    private RubyInstanceConfig config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new RubyInstanceConfig();
    }

    public void testRubyInstanceConfigDefaults() {
        assertEquals(RubyInstanceConfig.LoadServiceCreator.DEFAULT, config.getLoadServiceCreator());
        assertFalse(config.isInlineScript());
        assertNull(config.getScriptFileName());
        assertTrue(config.getLoadPaths().isEmpty());
        assertTrue(config.getRequiredLibraries().isEmpty());
        assertTrue(config.isUpdateNativeENVEnabled());
    }

    protected final static class NullLoadService extends LoadService {
        public NullLoadService(Ruby runtime) {
            super(runtime);
        }
    }

    public void testRubyInstanceConfigOverriding() {
        final boolean[] called = new boolean[1];
        config.setLoadServiceCreator(runtime -> {
            called[0] = true;
            return new NullLoadService(runtime);
        });
        Ruby ruby = Ruby.newInstance(config);
        assertTrue(called[0]);
        assertEquals(NullLoadService.class, ruby.getLoadService().getClass());
    }

    public void testGetScriptSource() throws Exception {
      config.setCurrentDirectory("uri:classloader:/test_dir");
      config.setScriptFileName("test_script.rb");

      String scriptSource = inputStreamToString(config.getScriptSource());

      assertTrue(scriptSource.startsWith("puts \"Hello World\""));
    }

    public void testGetScriptSourceWithSTDIN() {
      config.setScriptFileName(getSTDINPath());

      // FIXME: "CON" is not working here but I am not sure if this should or not?
      if (!Platform.IS_WINDOWS) assertNotNull(config.getScriptSource());
    }

    public void testGetScriptSourceWithDirectory() {
      config.setCurrentDirectory("uri:classloader:/somedir");
      config.setScriptFileName("dir_with_listing");

      try {
        config.getScriptSource();
        fail("Should throw FileNotFoundException");
      } catch (MainExitException ex) {
        assertTrue(ex.getMessage().contains("(Not a file)"));
      }
    }

    public void testGetScriptSourceWithNonexistentFile() {
      config.setCurrentDirectory("uri:classloader:/somedir");
      config.setScriptFileName("non_existing.rb");

      try {
        config.getScriptSource();
        fail("Should throw FileNotFoundException");
      } catch (MainExitException ex) {
        assertTrue(ex.getMessage().contains("(No such file or directory)"));
      }
    }

    private String inputStreamToString(InputStream inputStream) throws Exception {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
          result.write(buffer, 0, length);
      }
      return result.toString(StandardCharsets.UTF_8);
    }

    private String getSTDINPath() {
      String osName = System.getProperty("os.name").toLowerCase(Locale.US);
      if (osName.contains("windows")) return "CON";
      if (osName.contains("openvms")) return "/sys$input";
      if (osName.contains("mac")) return "/dev/fd/0";

      return "/dev/stdin";
    }
}
