package org.jruby.truffle.parser.ast;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.parser.TempSourceSection;

/**
 * Base class for all D (e.g. Dynamic) node types like DStrParseNode, DSymbolParseNode, etc...
 */
public abstract class DParseNode extends ListParseNode {
    protected Encoding encoding;

    public DParseNode(TempSourceSection position) {
        // FIXME: I believe this possibly should be default parsed encoding but this is
        // what we currently default to if we happen to receive a null encoding.  This is
        // an attempt to at least always have a valid encoding set to something.
        this(position, ASCIIEncoding.INSTANCE);
    }

    public DParseNode(TempSourceSection position, Encoding encoding) {
        super(position);

        assert encoding != null: getClass().getName() + " passed in a null encoding";

        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }
}
