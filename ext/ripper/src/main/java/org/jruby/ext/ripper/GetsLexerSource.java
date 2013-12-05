/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ripper;

import org.jcodings.Encoding;
import org.jruby.RubyEncoding;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *  Lexer source from ripper getting a line at a time via 'gets' calls.
 */
public class GetsLexerSource extends LexerSource {
    private IRubyObject io;
    private Encoding encoding;
    
    public GetsLexerSource(String sourceName, int line, IRubyObject io) {
        super(sourceName, line);
        
        this.io = io;
        encoding = frobnicateEncoding();
    }

    // FIXME: Should be a hard failure likely if no encoding is possible
    public final Encoding frobnicateEncoding() {
        if (!io.respondsTo("encoding")) return null;
        
        IRubyObject encodingObject = io.callMethod(io.getRuntime().getCurrentContext(), "encoding");

        return encodingObject instanceof RubyEncoding ? 
                ((RubyEncoding) encodingObject).getEncoding() : io.getRuntime().getDefaultExternalEncoding();
    }
    
    @Override
    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    @Override
    public ByteList gets() {
        IRubyObject result = io.callMethod(io.getRuntime().getCurrentContext(), "gets");
        
        if (result.isNil()) return null;
        
        ByteList bytelist = result.convertToString().getByteList();
        bytelist.setEncoding(encoding);
        return bytelist;
    }
    
}
