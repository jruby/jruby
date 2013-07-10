

package org.jruby.lexer.yacc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jruby.util.ByteList;

public class InputStreamLexerSource extends LexerSource {
    private static final int INITIAL_PUSHBACK_SIZE = 100;
    
    public static final int DATA_READ_BUFFER_SIZE = 65536;
    
    // Where we get our newest char's
    private final InputStream in;
    
    // Our readback/pushback buffer.
    private char buf[] = new char[INITIAL_PUSHBACK_SIZE];
    
    // index of last character in pushback buffer
    private int bufLength = -1;
    
    // Character read before previous read
    private int oneAgo = '\n';
    private int twoAgo = 0;
    
    /**
     * Create our food-source for the lexer
     * 
     * @param sourceName is the file we are reading
     * @param reader is what represents the contents of file sourceName
     * @param line starting line number for source (used by eval)
     * @param extraPositionInformation will gives us extra information that an IDE may want
     */
    public InputStreamLexerSource(String sourceName, InputStream in, List<String> list, int line, 
            boolean extraPositionInformation) {
        super(sourceName, list, line, extraPositionInformation);
        
        this.in = in;
    }
    
    /**
     * Read next character from this source
     * 
     * @return next character to viewed by the source
     */
    @Override
    public int read() throws IOException {
        int c;
        
        if (bufLength >= 0) {
            c = buf[bufLength--];
        } else {
            c = wrappedRead();
            
            if (c == -1) return RubyYaccLexer.EOF;
        }
        
        advance(c);
        
        if (c == '\n') line++;
            
        return c; 
    }

    /**
     * Pushes char back onto this source.  Note, this also
     * allows us to push whatever is passes back into the source.
     * 
     * @param  to be put back onto the source
     */
    @Override
    public void unread(int c) {
        if (c == RubyYaccLexer.EOF) return;
        
        retreat();
            
        if (c == '\n') line--;

        buf[++bufLength] = (char) c;
        
        growBuf();
    }
    
    /**
     * Is the next character equal to 'to'
     * @param to character to compare against
     * @return true if the same
     * @throws IOException
     */
    @Override
    public boolean peek(int to) throws IOException {
        // keep value of twoAgo around so we can restore after we unread
        int captureTwoAgo = twoAgo;
        int c = read();
        unread(c);
        twoAgo = captureTwoAgo;
        return c == to;
    }

    private void advance(int c) {

        twoAgo = oneAgo;
        oneAgo = c;
        offset++;
    }

    private int carriageReturn(int c) throws IOException {
        if ((c = in.read()) != '\n') {
            unread((char) c);
        } else {
            // Position within source must reflect the actual offset and column.  Since
            // we ate an extra character here (this accounting is normally done in read
            // ), we should update position info.
            offset++;
        }
        return c;
    }

    private void growBuf() {
        // If we outgrow our pushback stack then grow it (this should only happen in pretty 
        // pathological cases).
        if (bufLength + 1 == buf.length) {
            char[] newBuf = new char[buf.length + INITIAL_PUSHBACK_SIZE];

            System.arraycopy(buf, 0, newBuf, 0, buf.length);

            buf = newBuf;
        }
    }

    private void retreat() {

        offset--;
        oneAgo = twoAgo;
        twoAgo = 0;
    }
    
    /**
     * Convenience method to hide exception.  If we do hit an exception
     * we will pretend we EOF'd.
     * 
     * @return the current char or EOF (at EOF or on error)
     */
    private int wrappedRead() throws IOException {
        int c = in.read();
        
        // If \r\n then just pass along \n (windows). 
        if (c == '\r') { 
            c = carriageReturn(c);
        }

        captureFeature(c);

        return c;
    }

    @Override
    public ByteList readLineBytes() throws IOException {
        ByteList bytelist = new ByteList(80);

        for (int c = read(); c != '\n' && c != RubyYaccLexer.EOF; c = read()) {
            bytelist.append(c);
        }
        
        return bytelist;
    }
    
    @Override
    public int skipUntil(int marker) throws IOException {
        int c;
        for (c = read(); c != marker && c != RubyYaccLexer.EOF; c = read()) {}
        return c;
    }

    @Override
    public void unreadMany(CharSequence buffer) {
        int length = buffer.length();
        for (int i = length - 1; i >= 0; i--) {
            unread(buffer.charAt(i));
        }
    }

    @Override
    public boolean matchMarker(ByteList match, boolean indent, boolean checkNewline) throws IOException {
        int length = match.length();
        ByteList buffer = new ByteList(length + 1);
        
        if (indent) {
            indentLoop(buffer);
        }
        
        if (!matches(match, buffer, length)) return false;
        
        return finishMarker(checkNewline, buffer); 
    }

    private void indentLoop(ByteList buffer) throws IOException {
        int c;
        while ((c = read()) != RubyYaccLexer.EOF) {
            if (!Character.isWhitespace(c) || c == '\n') {
                unread(c);
                break;
            }
            buffer.append(c);
        }
    }
    
    private boolean matches(ByteList match, ByteList buffer, int length) throws IOException {
        int c;
        for (int i = 0; i < length; i++) {
            c = read();
            buffer.append(c);
            if (match.charAt(i) != c) {
                unreadMany(buffer);
                return false;
            }
        }
        return true;
    }

    private boolean finishMarker(boolean checkNewline, ByteList buffer) throws IOException {

        if (!checkNewline) {
            return true;
        }
        int c = read();

        if (c == RubyYaccLexer.EOF || c == '\n') {
            return true;
        }
        buffer.append(c);
        unreadMany(buffer);

        return false;
    }
    
    /**
     * Was the last character read from the stream the first character on a line
     * 
     * @return true if so
     */
    @Override
    public boolean wasBeginOfLine() {
        return twoAgo == '\n';
    }

    @Override
    public boolean lastWasBeginOfLine() {
        return oneAgo == '\n';
    }
    
    static final ByteList EOF_LABEL = new ByteList(new byte[] {'{', 'e', 'o', 'f', '}'});

    @Override
    public String toString() {
        try {
            ByteList buffer = new ByteList(20);
            ByteList unreadBuffer = new ByteList(20);

            if (twoAgo != -1 && twoAgo != 0) buffer.append(twoAgo);
            if (oneAgo != -1 && oneAgo != 0) buffer.append(oneAgo);

            buffer.append('<');

            int i = 0;
            int c = read();
            unreadBuffer.append(c);
            
            if (c == -1) {
                unread(unreadBuffer.charAt(0));
                buffer.append(EOF_LABEL);
                buffer.append('>');
                
                return buffer.toString();
            } else {
                buffer.append(c).append('>');
            }
            i = 1;
            
            for (; i < 20; i++) {
                c = read();
                unreadBuffer.append(c);
                if (c == -1) {
                    buffer.append(EOF_LABEL);
                    i--;
                    break;
                }
                buffer.append(c);
            }
            for (; i >= 0; i--) {
                unread(unreadBuffer.charAt(i));
            }
            buffer.append(new byte[] {' ', '.', '.', '.'});
            return buffer.toString();
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public ByteList readUntil(char marker) throws IOException {
        ByteList list = new ByteList(20);
        int c;
        
        for (c = read(); c != marker && c != RubyYaccLexer.EOF; c = read()) {
            list.append(c);
        }
        
        if (c == RubyYaccLexer.EOF) return null;
        
        unread(c);
        
        return list;
    }

    @Override
    public InputStream getRemainingAsStream() throws IOException {
        return in;
    }
}
