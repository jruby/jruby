package org.jruby.javasupport.test;

import junit.framework.TestCase;
import org.jruby.RubyKernel;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RubyTestCase extends TestCase {
    private static final IRubyObject[] EMPTY_ARRAY = IRubyObject.NULL_ARRAY;

    public RubyTestCase(String name) {
        super(name);
    }

    protected Ruby createRuby(URL url) throws IOException {
        if (url == null) {
            throw new NullPointerException("url was null");
        }
        InputStream in = url.openStream();
        File f = File.createTempFile("rtc", ".rb");
        FileOutputStream out = new FileOutputStream(f);

        int length;
        byte[] buf = new byte[8096];
        while ((length = in.read(buf, 0, buf.length)) >= 0) {
            out.write(buf, 0, length);
        }
        in.close();
        out.close();

        String filePath = f.getAbsolutePath();
        Ruby runtime = Ruby.getDefaultInstance();
        initRuby(runtime);
        RubyKernel.require(runtime.getTopSelf(), new RubyString(runtime, filePath));
        f.delete();
        return runtime;
    }

    // Is there something built into JRuby to do this?
    protected void initRuby(Ruby runtime) {
        IRubyObject empty =
            JavaUtil.convertJavaToRuby(
                runtime,
                EMPTY_ARRAY,
                EMPTY_ARRAY.getClass());

        runtime.defineReadonlyVariable("$-p", runtime.getNil());
        runtime.defineReadonlyVariable("$-n", runtime.getNil());
        runtime.defineReadonlyVariable("$-a", runtime.getNil());
        runtime.defineReadonlyVariable("$-l", runtime.getNil());
        runtime.defineReadonlyVariable("$\"", empty);
        runtime.defineReadonlyVariable("$*", empty);
        runtime.defineReadonlyVariable("$:", empty);
        runtime.defineGlobalConstant("ARGV", empty);
    }
}

