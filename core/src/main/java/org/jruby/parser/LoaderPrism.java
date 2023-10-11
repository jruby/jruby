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

    // FIXME: could not override impl (made constructor protected)
    // FIXME: could not access encodingName so made protected
    // FIXME: extra work done for encodingCharset which we do not use (but TR probably does)
    // FIXME: consider abstract methods for Loader
    LoaderPrism(Ruby runtime, byte[] serialized, Nodes.Source source) {
        super(serialized, source);

        this.runtime = runtime;
    }

    public ParseResult load() {
        ParseResult result = super.load();

        resolveEncoding();

        return result;
    }

    public org.jruby.RubySymbol bytesToName(byte[] bytes) {
        resolveEncoding();
        return runtime.newSymbol(new ByteList(bytes, encoding));
    }

    private void resolveEncoding() {
        if (encoding == null) {
            encoding = runtime.getEncodingService().findEncodingOrAliasEntry(encodingName.getBytes()).getEncoding();
        }
    }

    public Encoding getEncoding() {
        return encoding;
    }
}
