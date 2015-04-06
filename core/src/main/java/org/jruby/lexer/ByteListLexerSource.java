/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.lexer;

import org.jcodings.Encoding;
import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.util.ByteList;

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
    public ByteListLexerSource(String sourceName, int line, ByteList in, RubyArray list) {
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
        if (offset == length) return null; // At end of source/eof

        int end = offset;
        
        while (end < length) {
            if (completeSource.get(end) == '\n') {
                end++; // include newline
                break;
            }
            end++;
        }

        ByteList line = completeSource.makeShared(offset, end - offset);

        offset = end;

        if (scriptLines != null) scriptLines.append(RubyString.newString(scriptLines.getRuntime(), line));

        return line;
    }

    @Override
    public int getOffset() {
        return offset;
    }
}
