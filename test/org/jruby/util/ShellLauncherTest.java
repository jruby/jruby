package org.jruby.util;

import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

public class ShellLauncherTest extends TestCase {
    private Ruby runtime;
    private ShellLauncher launcher;
    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.getDefaultInstance();
        launcher = new ShellLauncher(runtime);
    }

    public void testScriptThreadProcessPuts() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RubyString cmd = RubyString.newString(runtime, "jruby -e \"puts %{hi}\"");
        int result = launcher.runAndWait(new IRubyObject[] {cmd}, baos);
        assertEquals(0, result);
        assertEquals("hi\n", baos.toString());
    }
    
    public void testCanLaunchShellsFromInternallForkedRubyProcess() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RubyString cmd = RubyString.newString(runtime, 
          "jruby -e \"system(%Q[ruby -e \"system 'echo hello' ; puts 'done'\"])\"");
        int result = launcher.runAndWait(new IRubyObject[] {cmd}, baos);
        assertEquals(0, result);
        String msg = baos.toString();
        msg = msg.replaceAll("\r", "");
        assertEquals("hello\ndone\n", msg);
    }
}