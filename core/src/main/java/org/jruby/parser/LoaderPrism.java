package org.jruby.parser;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.util.ByteList;
import org.prism.Loader;
import org.prism.Nodes;
import org.prism.ParseResult;

public class LoaderPrism extends Loader {
    private Ruby runtime;

    private Encoding encoding = null;

    public static ParseResult load(Ruby runtime, byte[] serialized, Nodes.Source source) {
        return new LoaderPrism(runtime, serialized, source).load();
    }

    // FIXME: could not override impl (made constructor protected)
    // FIXME: could not access encodingName so made protected
    // FIXME: extra work done for encodingCharset which we do not use (but TR probably does)
    // FIXME: consider abstract methods for Loader
    private LoaderPrism(Ruby runtime, byte[] serialized, Nodes.Source source) {
        super(serialized, source);

        this.runtime = runtime;
    }

    public org.jruby.RubySymbol bytesToName(byte[] bytes) {
        if (encoding == null) {
            encoding = runtime.getEncodingService().findEncodingOrAliasEntry(encodingName.getBytes()).getEncoding();
        }
        return runtime.newSymbol(new ByteList(bytes, encoding));
    }
}
