package org.jruby.util;

import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.FileWriter;
import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.exceptions.RaiseException;
import org.jruby.RubyString;
import jnr.posix.util.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ShellLauncherTest extends TestCase {

    private Ruby runtime;
    private ShellLauncher launcher;
    private File testDir;
    private File testFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        if (testDir != null) {
            testFile.delete();
            testDir.delete();
        }
    }

    // Disabled for shell-character fixes; see JRUBY-3097
//    public void testScriptThreadProcessPuts() {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        RubyString cmd = RubyString.newString(runtime, "jruby -e \"puts %{hi}\"");
//        int result = ShellLauncher.runAndWait(runtime, new IRubyObject[]{cmd}, baos);
//        assertEquals(0, result);
//        assertEquals("hi\n", baos.toString());
//    }

//    public void testScriptVerboseOutput() {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        RubyString cmd = RubyString.newString(runtime, "jruby -e \"1.upto(1000) { puts %{hi} }\"");
//        int result = ShellLauncher.runAndWait(runtime, new IRubyObject[]{cmd}, baos);
//        assertEquals(0, result);
//        assertEquals(3000, baos.size());
//    }

//    public void testCanLaunchShellsFromInternallForkedRubyProcess() {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        RubyString cmd = RubyString.newString(runtime,
//                "jruby -e \"system(%Q[ruby -e 'system %q(echo hello) ; puts %q(done)'])\"");
//        int result = ShellLauncher.runAndWait(runtime, new IRubyObject[]{cmd}, baos);
//        assertEquals(0, result);
//        String msg = baos.toString();
//        msg = msg.replaceAll("\r", "");
//        assertEquals("hello\ndone\n", msg);
//    }

    public void testSingleArgumentCommandOnWindowsIsOnlyRunByShellIfCommandContainsSpaces() {
        if (Platform.IS_WINDOWS) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RubyString cmd = RubyString.newString(runtime, "nonexistentcmd");
            try {
                ShellLauncher.runAndWait(runtime, new IRubyObject[]{cmd}, baos);
                fail("should have raised an exception");
            } catch (RaiseException re) {
            }
        }
    }

    // This is the result of a bug that left off all trailing arguments. E.g.:
    //   system "ls", "-1", "dir"
    // would be seen as
    //   system "ls"
    public void testMultipleArgsAreNotSentToShellAsSingleString() throws Exception {
        testDir = new File("sh_l_test");
        testDir.mkdirs();
        testFile = new File(testDir, "hello.txt");
        FileWriter fw = new FileWriter(testFile);
        fw.write("hello there");
        fw.close();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int result = ShellLauncher.runAndWait(runtime, new IRubyObject[] {
                RubyString.newString(runtime, "ls"),
                RubyString.newString(runtime, "-1"),
                RubyString.newString(runtime, testDir.getName()),
            }, baos);

            if (result == 0) {
                String msg = baos.toString();
                msg = msg.replaceAll("\n", "");
                assertEquals("hello.txt", msg);
            }
        } catch (Exception e) {
            // skip this one, probably no 'ls' (windows)
        }
    }
    public void testUsesRubyEnvPathToRunShellPrograms() {
        RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
        RubyString path = runtime.newString("PATH");
        RubyString utilPath = runtime.newString(System.getProperty("jruby.home") + "/core/src/test/java/org/jruby/util");
        ThreadContext context = runtime.getCurrentContext();
        env.op_aset(context, path, 
                env.op_aref(context, path).convertToString()
                .op_plus(context, runtime.newString(File.pathSeparator)).convertToString()
                .op_plus(context, utilPath));

        String cmd = "shell_launcher_test";
        if (Platform.IS_WINDOWS) {
            cmd += ".bat";
        }
        int code = ShellLauncher.runAndWait(runtime, new IRubyObject[] {
           RubyString.newString(runtime, cmd)
        });
        assertEquals(0, code);
    }
}
