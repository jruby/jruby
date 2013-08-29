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
    
    public GetsLexerSource(String sourceName, int line, IRubyObject io) {
        super(sourceName, line);
        
        this.io = io;
    }

    // FIXME: Should be a hard failure likely if no encoding is possible
    @Override
    public Encoding getEncoding() {
        if (!io.respondsTo("encoding")) return null;
        
        IRubyObject encodingObject = io.callMethod(io.getRuntime().getCurrentContext(), "encoding");

        return encodingObject instanceof RubyEncoding ? ((RubyEncoding) encodingObject).getEncoding() : null;
    }

    @Override
    public void setEncoding(Encoding encoding) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ByteList gets() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
