package org.jruby.truffle.parser.ast;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.parser.lexer.SimpleSourcePosition;

/**
 * Base class for all D (e.g. Dynamic) node types like DStrParseNode, DSymbolParseNode, etc...
 */
public abstract class DParseNode extends ListParseNode {
    protected Encoding encoding;

    public DParseNode(SimpleSourcePosition position) {
        // FIXME: I believe this possibly should be default parsed encoding but this is
        // what we currently default to if we happen to receive a null encoding.  This is
        // an attempt to at least always have a valid encoding set to something.
        this(position, ASCIIEncoding.INSTANCE);
    }

    public DParseNode(SimpleSourcePosition position, Encoding encoding) {
        super(position);

        assert encoding != null: getClass().getName() + " passed in a null encoding";

        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }
}
