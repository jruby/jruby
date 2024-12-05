package org.jruby.ext.date;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.load.Library;

import java.io.IOException;

public class DateLibrary implements Library {

    public static void load(Ruby runtime) {
        var context = runtime.getCurrentContext();
        RubyClass Date = RubyDate.createDateClass(context);
        RubyDateTime.createDateTimeClass(context, Date);
        TimeExt.load(context);
    }

    public void load(final Ruby runtime, boolean wrap) throws IOException {
        DateLibrary.load(runtime);
    }

}
