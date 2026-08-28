package org.jruby.parser;

public enum ParserType {
    MAIN,   // The main file to run using Ruby (this is only one which uses DATA/__END__) also TOPLEVEL_BINDING scope.
    EVAL,   // An evaluation parse (almost always has a passed in existingScope -- except evalScriptlet)
    INLINE, // -e but also passes in TOPLEVEL_BINDING scope.
    NORMAL  // Any other file.
}
