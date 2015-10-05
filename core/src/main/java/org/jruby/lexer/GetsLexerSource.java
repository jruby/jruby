/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.lexer;

import org.jcodings.Encoding;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *  Lexer source from ripper getting a line at a time via 'gets' calls.
 */
public class GetsLexerSource extends LexerSource {
    private IRubyObject io;
    private Encoding encoding;
    private int offset;

    // Main-line Parsing constructor
    public GetsLexerSource(String sourceName, int line, IRubyObject io, RubyArray scriptLines, Encoding encoding) {
        super(sourceName, line, scriptLines);

        this.io = io;
        this.encoding = encoding;
    }

    // FIXME (enebo 21-Apr-15): ripper probably has same problem as main-line parser so this constructor
    // may need to be a mix of frobbing the encoding of an incoming object plus defaultEncoding if not.
    // But main-line parser should not be asking IO for encoding.
    // Ripper constructor
    public GetsLexerSource(String sourceName, int line, IRubyObject io, RubyArray scriptLines) {
        this(sourceName, line, io, scriptLines, frobnicateEncoding(io));
    }

    // FIXME: Should be a hard failure likely if no encoding is possible
    public static final Encoding frobnicateEncoding(IRubyObject io) {
        // Non-ripper IO will not have encoding so we will just use default external
        if (!io.respondsTo("encoding")) return io.getRuntime().getDefaultExternalEncoding();
        
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
        encodeExistingScriptLines(encoding);
    }

    @Override
    public ByteList gets() {
        IRubyObject result = io.callMethod(io.getRuntime().getCurrentContext(), "gets");
        
        if (result.isNil()) return null;
        
        ByteList bytelist = result.convertToString().getByteList();
        offset += bytelist.getRealSize();
        bytelist.setEncoding(encoding);

        if (scriptLines != null) scriptLines.append(RubyString.newString(scriptLines.getRuntime(), bytelist));

        return bytelist;
    }

    @Override
    public int getOffset() {
        return offset;
    }
}
