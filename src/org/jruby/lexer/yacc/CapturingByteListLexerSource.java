package org.jruby.lexer.yacc;

import java.io.IOException;
import java.util.List;

import org.jruby.util.ByteList;

public class CapturingByteListLexerSource extends ByteListLexerSource {
    public CapturingByteListLexerSource(String sourceName, ByteList bytelist, List<String> list, int line, 
            boolean extraPositionInformation) {
        super(sourceName, bytelist, list, line, extraPositionInformation);
    }
    
    @Override
    public int read() throws IOException {
        int c = super.read();
        captureFeature(c);
        return c;
    }
}
