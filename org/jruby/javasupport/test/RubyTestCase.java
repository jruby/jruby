package org.jruby.javasupport.test;

import junit.framework.TestCase;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import org.jruby.*;
import org.jruby.util.*;
import org.jruby.javasupport.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.regexp.JDKRegexpAdapter;

public class RubyTestCase extends TestCase {
    private static final IRubyObject[] EMPTY_ARRAY = new IRubyObject[0];

    public RubyTestCase(String name) {
        super(name);
    }

    protected Ruby createRuby(String fileName) throws IOException {
        return createRuby(new File(fileName));
    }

    protected Ruby createRuby(URL url) throws IOException {
        if (url.getProtocol().equalsIgnoreCase("file"))
            return createRuby(new File(url.getPath()));
        else
            return createRuby(url.openStream());
    }

    protected Ruby createRuby(InputStream in) throws IOException {
        File f = File.createTempFile("rtc", ".rb");
        FileOutputStream out = new FileOutputStream(f);

        int l;
        byte[] buf = new byte[8096];
        while ((l = in.read(buf, 0, buf.length)) >= 0) {
            out.write(buf, 0, l);
        }

        in.close();
        out.close();

        return createRuby(f);
    }

    protected Ruby createRuby(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        Ruby ruby = Ruby.getDefaultInstance("JDK");

        initRuby(ruby);

        KernelModule.require(ruby.getTopSelf(), new RubyString(ruby, filePath));

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

