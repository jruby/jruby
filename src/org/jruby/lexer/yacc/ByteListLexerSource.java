package org.jruby.lexer.yacc;

import java.io.IOException;
import java.util.List;

import org.jruby.parser.ParserConfiguration;
import org.jruby.util.ByteList;

public class ByteListLexerSource extends LexerSource {
    private int begin;
    private int realSize;
    private int index;
    private byte[] bytes;

    public ByteListLexerSource(String sourceName, ByteList bytelist, List<String> list, int line, 
            boolean extraPositionInformation) {
        super(sourceName, list, line, extraPositionInformation);

        /* Consider Thread aspects of this */
        bytes = bytelist.bytes;
        index = bytelist.begin();
        begin = index;
        realSize = bytelist.realSize;
    }

    @Override
    public boolean matchMarker(ByteList marker, boolean indent) throws IOException {
        // Where we started this marker match
        int start = index;
        
        if (indent) {
            char c;
            while ((c = read()) != '\0') {
                if (!Character.isWhitespace(c) || c == '\n') {
                    index--;
                    break;
                }
            }
        }
        
        for (int i = 0; i < marker.length(); i++) {
            if (marker.charAt(i) != read()) {
                index = start;
                return false;
            }
        }

        char c = (char) internalRead();
        if (c != '\0' && c != '\n') return false;

        return true;
    }

    @Override
    public boolean peek(char c) throws IOException {
        // PEEK(EOF)?
        if (index >= realSize) return false;
        
        return bytes[index] == c;
    }
    
    private byte internalRead() {
        return index >= realSize ? 0 : bytes[index++]; 
    }

    @Override
    public char read() throws IOException {
        byte c = internalRead();
            
        // If \r\n then just pass along \n (windows). 
        // If \r[^\n] then pass along \n (MAC).
        switch (c) {
        case '\r':
            if ((c = internalRead()) != '\n') {
                unread((char) c);
                c = '\n';
                line++;
            } else {
                // Position within source must reflect the actual offset and column.  Since
                // we ate an extra character here (this accounting is normally done in read
                // ), we should update position info.
                offset++;
                // We use offset here to keep track of extra newlines for windows
            }
            break;
        case '\n':
            line++;
            break;
        }

        return (char) c;
    }

    @Override
    public ByteList readLineBytes() throws IOException {
        int count = 0;

        for (char c = read(); c != '\n' && c != '\0'; c = read()) {
            count++;
        }
        
        return new ByteList(bytes, index - count, count, false);
    }
    
    @Override
    public char skipUntil(char c) throws IOException {
        for (c = read(); c != '\n' && c != '\0'; c = read()) {}
        
        return c;
    }

    @Override
    public void unread(char c) {
        if (c == 0) return;
        
        index--;
            
        if (c == '\n') line--;
        // FIXME: \r logic needed
    }

    @Override
    public void unreadMany(CharSequence line) {
        int length = line.length();

        index -= length;
    }

    @Override
    public boolean wasBeginOfLine() {
        // FIXME: Old Mac format strings?
        return index == 0 || bytes[index - 1] == '\n';
    }

    // FIXME: Make faster
    @Override
    public String matchMarkerNoCase(ByteList match) throws IOException {
       ByteList buf = new ByteList(match.length());
        
        for (int i = 0; i < match.length(); i++) {
            char c = match.charAt(i);
            char r = read();
            buf.append(r);
            
            if (Character.toLowerCase(c) != r && Character.toUpperCase(c) != r) {
                unreadMany(buf);
                return null;
            }
        }

        return buf.toString();
    }

    public static LexerSource getSource(String file, ByteList content, List<String> list,
            ParserConfiguration configuration) {
        if (list != null) {
            return new CapturingByteListLexerSource(file, content, list, configuration.getLineNumber(), 
                    configuration.hasExtraPositionInformation());
        }
        return new ByteListLexerSource(file, content, list, configuration.getLineNumber(), 
                configuration.hasExtraPositionInformation());
    }

    @Override
    public int getOffset() {
        return index - begin + offset;
    }

    @Override
    public ByteList readUntil(char marker) throws IOException {
        int begin = index;
        int i = begin;

        for ( ; i < realSize && bytes[i] != marker; i++ ) {
        }
        
        if (i >= realSize) return null;
        
        index = i;
        
        return new ByteList(bytes, begin, i - begin, false);
    }

    @Override
    public ByteList readIdentifer() throws IOException {
        int begin = index;
        int i = begin;

        for ( ; i < realSize && RubyYaccLexer.isIdentifierChar((char) bytes[i]); i++ ) {
        }
        
        if (bytes[i] == '!' || bytes[i] == '?') {
            if ((i + 1) < realSize && bytes[i+1] != '=') {
                i++;
            }
        }
        
        index = i;
        return new ByteList(bytes, begin, i - begin, false);
    }

}
