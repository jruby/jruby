/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ripper;

import org.jcodings.Encoding;
import org.jruby.util.ByteList;

/**
 *  Lexer source for ripper when we have all bytes available to us.
 */
public class ByteListLexerSource extends LexerSource {
    private ByteList completeSource; // The entire source of the file
    private int offset = 0; // Offset into source overall (mri: lex_gets_ptr)
    
    /**
     * Create our food-source for the lexer
     * 
     * @param sourceName is the file we are reading
     * @param reader is what represents the contents of file sourceName
     * @param line starting line number for source (used by eval)
     * @param extraPositionInformation will gives us extra information that an IDE may want (deprecated)
     */
    public ByteListLexerSource(String sourceName, ByteList in, int lineOffset) {
        super(sourceName, lineOffset);
        this.completeSource = in;
    }
    
    @Override
    public Encoding getEncoding() {
        return completeSource.getEncoding();
    }
    
    @Override
    public void setEncoding(Encoding encoding) {
        completeSource.setEncoding(encoding);
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
        
        return line;
    }    
}
