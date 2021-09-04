package org.jruby.ast;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;

/**
 * Base class for all D (e.g. Dynamic) node types like DStrNode, DSymbolNode, etc...
 */
public abstract class DNode extends ListNode {
    protected Encoding encoding;

    public DNode(int line) {
        // FIXME: I believe this possibly should be default parsed encoding but this is
        // what we currently default to if we happen to receive a null encoding.  This is
        // an attempt to at least always have a valid encoding set to something.
        this(line, ASCIIEncoding.INSTANCE);
    }

    public DNode(int line, Encoding encoding) {
        super(line);

        assert encoding != null: getClass().getName() + " passed in a null encoding";

        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }
}
