package org.jruby.ext.date;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.load.Library;

import java.io.IOException;

public class DateLibrary implements Library {

    public static void load(Ruby runtime) {
        RubyClass Date = RubyDate.createDateClass(runtime);
        RubyDateTime.createDateTimeClass(runtime, Date);
        TimeExt.load(runtime);
    }

    public void load(final Ruby runtime, boolean wrap) throws IOException {
        DateLibrary.load(runtime);
    }

}
