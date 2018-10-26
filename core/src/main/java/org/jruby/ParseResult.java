package org.jruby;

import org.jruby.parser.StaticScope;

public interface ParseResult {
    StaticScope getStaticScope();
    int getLine();
    String getFile();
}
