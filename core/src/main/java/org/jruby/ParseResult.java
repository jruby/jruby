package org.jruby;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public interface ParseResult {
    StaticScope getStaticScope();
    DynamicScope getDynamicScope();
    int getLine();
    String getFile();
    int getCoverageMode();
}
