package org.jruby.test;

import jnr.posix.util.Platform;
import org.jruby.RubyArray;
import org.jruby.exceptions.RaiseException;

public class TestProcessSpawn extends Base {

    public void testSpawnAndDetach() throws RaiseException {
        if (!Platform.IS_WINDOWS) {
            context.runtime.evalScriptlet("require 'open3'; require 'jruby'");
            Object result = context.runtime.evalScriptlet("Kernel.spawn('non-existent-cmd') rescue nil");
            int errno = context.runtime.getPosix().errno();
            assertTrue("expected > 0 got: " + errno, errno > 0);
            String cmd = "echo foo";
            // should not raise ENOENT: No such file or directory
            result = context.runtime.evalScriptlet("out_err, status = Open3.capture2e('" + cmd + "'); [out_err, status.to_i]");
            // ["foo\n", #<Process::Status: pid 3839850 exit 0>]
            assertEquals(0, ((Number) ((RubyArray) result).get(1)).intValue());
        }
    }

}
