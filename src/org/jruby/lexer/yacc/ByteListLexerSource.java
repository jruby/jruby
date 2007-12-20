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
    public boolean matchMarker(ByteList marker, boolean indent, boolean checkNewline) throws IOException {
        // Where we started this marker match
        int start = index;
        
        if (indent) {
            int c;
            while ((c = read()) != RubyYaccLexer.EOF) {
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

        if (!checkNewline) return true;
        
        char c = (char) internalRead();
        
        if (c == RubyYaccLexer.EOF || c == '\n') return true;
        
        index = start;
        
        return false; 
    }

    @Override
    public boolean peek(int c) throws IOException {
        if (index >= realSize) return c == RubyYaccLexer.EOF;
        
        return bytes[index] == c;
    }
    
    private byte internalRead() {
        return index >= realSize ? RubyYaccLexer.EOF : bytes[index++]; 
    }

    @Override
    public int read() throws IOException {
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

        return c;
    }

    @Override
    public ByteList readLineBytes() throws IOException {
        int count = 0;

        for (int c = read(); c != '\n' && c != RubyYaccLexer.EOF; c = read()) {
            count++;
        }
        
        return new ByteList(bytes, index - count - 1, count, false);
    }
    
    @Override
    public int skipUntil(int c) throws IOException {
        for (c = read(); c != '\n' && c != RubyYaccLexer.EOF; c = read()) {}
        
        return c;
    }

    @Override
    public void unread(int c) {
        if (c == RubyYaccLexer.EOF) return;
        
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

    @Override
    public boolean lastWasBeginOfLine() {
        // FIXME: Old Mac format strings?
        return index == 0 || bytes[index] == '\n';
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
    public String toString() {
        try {
            ByteList buffer = new ByteList(20);
            buffer.append(bytes[index - 1]);
            buffer.append(bytes[index]);
            buffer.append(new byte[] {'-', '>'});
            for (int i = index + 1; i < index + 20; i++) {
                if (index > realSize) break;

                buffer.append(bytes[i]);
            }
            buffer.append(new byte[] {' ', '.', '.', '.'});
            return buffer.toString();
        } catch(Exception e) {
            return null;
        }
    }
}
