

package org.jruby.lexer.yacc;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jruby.parser.ParserConfiguration;
import org.jruby.util.ByteList;

public class InputStreamLexerSource extends LexerSource {
    private static final int INITIAL_PUSHBACK_SIZE = 100;
    
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
            c = '\n';
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
        // If \r[^\n] then pass along \n (MAC).
        if (c == '\r') { 
            c = carriageReturn(c);
        }

        captureFeature(c);

        return c;
    }
    
    /**
     * Create a source.
     * 
     * @param name the name of the source (e.g a filename: foo.rb)
     * @param content the data of the source
     * @return the new source
     */
    public static LexerSource getSource(String name, InputStream content, List<String> list,
            ParserConfiguration configuration) {
        return new InputStreamLexerSource(name, content, list, configuration.getLineNumber(), 
                configuration.hasExtraPositionInformation());
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
    public int skipUntil(int c) throws IOException {
        for (c = read(); c != '\n' && c != RubyYaccLexer.EOF; c = read()) {}
        
        return c;
    }

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
    public boolean wasBeginOfLine() {
        return twoAgo == '\n';
    }

    public boolean lastWasBeginOfLine() {
        return oneAgo == '\n';
    }

    public String toString() {
        try {
            ByteList buffer = new ByteList(20);
            buffer.append(twoAgo);
            buffer.append(oneAgo);
            buffer.append(new byte[] {'-', '>'});
            int i = 0;
            for (; i < 20; i++) {
                int c = read();
                if (c == 0) {
                    i--;
                    break;
                }
                buffer.append(c);
            }
            for (; i >= 0; i++) {
                unread(buffer.charAt(i));
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
}
