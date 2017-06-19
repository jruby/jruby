package org.jruby.ext.date;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.load.Library;

import java.io.IOException;

/**
 * Assumes kernel will lo
 */
public class DateLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        RubyClass dateClass = runtime.getClass("Date");
        dateClass.defineAnnotatedMethods(RubyDate.class);
    }
}
