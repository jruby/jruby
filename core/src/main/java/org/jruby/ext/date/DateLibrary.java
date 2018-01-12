package org.jruby.ext.date;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.load.Library;

import java.io.IOException;

public class DateLibrary implements Library {

    public static void load(Ruby runtime) {
        // assuming date.rb is loading first, for now :
        RubyClass Date = runtime.getClass("Date");
        Date.defineAnnotatedMethods(RubyDate.class);
    }

    public void load(final Ruby runtime, boolean wrap) throws IOException {
        DateLibrary.load(runtime);
    }

}
