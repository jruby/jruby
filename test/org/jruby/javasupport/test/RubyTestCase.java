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
        Ruby ruby = Ruby.getDefaultInstance("JDK");
        initRuby(ruby);
        RubyKernel.require(ruby.getTopSelf(), new RubyString(ruby, filePath));
        f.delete();
        return ruby;
    }

    // Is there something built into JRuby to do this?
    protected void initRuby(Ruby ruby) throws IOException {
        IRubyObject empty =
            JavaUtil.convertJavaToRuby(
                ruby,
                EMPTY_ARRAY,
                EMPTY_ARRAY.getClass());

        ruby.defineReadonlyVariable("$-p", ruby.getNil());
        ruby.defineReadonlyVariable("$-n", ruby.getNil());
        ruby.defineReadonlyVariable("$-a", ruby.getNil());
        ruby.defineReadonlyVariable("$-l", ruby.getNil());
        ruby.defineReadonlyVariable("$\"", empty);
        ruby.defineReadonlyVariable("$*", empty);
        ruby.defineReadonlyVariable("$:", empty);
        ruby.defineGlobalConstant("ARGV", empty);
    }
}

