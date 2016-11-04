/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.truffle.parser.lexer;

import org.jcodings.Encoding;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.ChannelHelper;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channel;
import java.util.List;

/**
 *  Lexer source for ripper when we have all bytes available to us.
 */
public class ByteListLexerSource extends LexerSource {
    private ByteList completeSource; // The entire source of the file
    private int offset = 0; // Offset into source overall (mri: lex_gets_ptr)

    /**
     * Create our food-source for the lexer.
     * 
     * @param sourceName is the file we are reading
     * @param line starting line number for source (used by eval)
     * @param in the ByteList backing the source we want to lex
     */
    public ByteListLexerSource(String sourceName, int line, ByteList in, List<ByteList> list) {
        super(sourceName, line, list);
        this.completeSource = in;
    }
    
    @Override
    public Encoding getEncoding() {
        return completeSource.getEncoding();
    }
    
    @Override
    public void setEncoding(Encoding encoding) {
        completeSource.setEncoding(encoding);
        encodeExistingScriptLines(encoding);
    }
    
    
    @Override
    public ByteList gets() {
        int length = completeSource.length();
        if (offset >= length) return null; // At end of source/eof

        int end = completeSource.indexOf('\n', offset) + 1;
        if (end == 0) end = length;

        ByteList line = completeSource.makeShared(offset, end - offset);
        offset = end;

        if (scriptLines != null) scriptLines.add(line);

        return line;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public Channel getRemainingAsChannel() {
        ByteArrayInputStream bais = new ByteArrayInputStream(completeSource.unsafeBytes(), completeSource.begin(), completeSource.realSize());
        bais.skip(offset);
        return ChannelHelper.readableChannel(bais);
    }

    @Override
    public IRubyObject getRemainingAsIO() {
        throw new UnsupportedOperationException();
        //if (scriptLines == null) return null;
        //return new RubyIO(scriptLines.getRuntime(), getRemainingAsChannel());
    }
}
