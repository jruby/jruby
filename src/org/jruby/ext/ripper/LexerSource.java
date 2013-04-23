/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Zach Dennis <zdennis@mktec.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.ripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jcodings.Encoding;

import org.jruby.parser.ParserConfiguration;
import org.jruby.util.ByteList;

/**
 * This class is what feeds the lexer.  It is primarily a wrapper around a
 * Reader that can unread() data back onto the source.  Originally, I thought
 * about using the PushBackReader to handle read/unread, but I realized that
 * some extremely pathological case could overflow the pushback buffer.  Better
 * safe than sorry.  I could have combined this implementation with a 
 * PushbackBuffer, but the added complexity did not seem worth it.
 * 
 */
public class LexerSource {
	// Last position we gave out
    private Position lastPosition;
	
    // The name of this source (e.g. a filename: foo.rb)
    private final String sourceName;
    
    // Number of newlines read from the reader
    protected int line = 0;
    
    // Virtual line as specified by eval, etc...
    protected int lineOffset = 0;
    
    // How many bytes into the source are we?
    protected int offset = 0;
    
    // Store each line into this list if not null.
    private List<String> list;
    
    // For 'list' and only populated if list is not null.
    private StringBuilder lineBuffer;

    // Last full line read.
    private StringBuilder sourceLine;
    
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
     * @param extraPositionInformation will gives us extra information that an IDE may want (deprecated)
     */
    protected LexerSource(String sourceName, InputStream in, List<String> list, int lineOffset) {
        this.in = in;        
        this.sourceName = sourceName;
        this.lineOffset = lineOffset;
        lastPosition = new Position(sourceName, line, line, offset, offset);
        this.list = list;
        lineBuffer = new StringBuilder(160);
        sourceLine = new StringBuilder(160);
    }

    /**
     * What file are we lexing?
     * @return the files name
     */
    public String getFilename() {
    	return sourceName;
    }
    
    /**
     * What line are we at?
     * @return the line number 0...line_size-1
     */
    public int getLine() {
        return line;
    }
    
    public int getVirtualLine() {
        return line + lineOffset;
    }
    
    /**
     * The location of the last byte we read from the source.
     * 
     * @return current location of source
     */
    public int getOffset() {
        return (offset <= 0 ? 0 : offset);
    }
    
    /**
     * Where is the reader within the source {filename,row}
     * 
     * @return the current position
     */
    public Position getPosition() {
        return new Position(getFilename(), lastPosition.getEndLine(),
                    getLine(), lastPosition.getEndOffset(), getOffset());
    }
    
    /**
     * Where is the reader within the source {filename,row}
     * 
     * @return the current position
     */
    public Position getPosition(Position startPosition, boolean inclusive) {
        if (startPosition == null) {
            lastPosition = new Position(getFilename(), lastPosition.getEndLine(),
                    getLine(), lastPosition.getEndOffset(), getOffset());
        } else if (inclusive) {
            lastPosition = new Position(getFilename(), startPosition.getStartLine(),
                    getLine(), startPosition.getStartOffset(), getOffset());
        } else {
            lastPosition = new Position(getFilename(), startPosition.getEndLine(),
                    getLine(), startPosition.getEndOffset(), getOffset());
        }

        return lastPosition;
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
        return new LexerSource(name, content, list, configuration.getLineNumber());
    }

    private void captureFeatureNewline() {
        StringBuilder temp = sourceLine;
        // Save sourceLine for error reporting to display line where error occurred
        sourceLine = lineBuffer;


        // Ruby's OMG capture all source in a Hash feature
        // Add each line to buffer when encountering newline or EOF for first time.
        if (list != null && lineBuffer.length() > 0) list.add(sourceLine.toString());

        temp.setLength(0);
        lineBuffer = temp;
    }

    protected void captureFeature(int c) {
        switch(c) {
            case '\n':
                lineBuffer.append((char) c);
            case -1:
                captureFeatureNewline();
                break;
            default:
                lineBuffer.append((char) c);
                break;
        }
    }

    protected void uncaptureFeature(int c) {
        int end = lineBuffer.length() - 1;
        if (end >= 0 && lineBuffer.charAt(end) == c) {
            lineBuffer.deleteCharAt(end);
        } else if (c == '\n' && list != null && !list.isEmpty()) {
            lineBuffer = new StringBuilder(list.remove(list.size() - 1));
            end = lineBuffer.length() - 1;
            if (lineBuffer.charAt(end) == '\n') {
                lineBuffer.deleteCharAt(end);
            }
        }
    }

    public String getCurrentLine() {
        int errorLocation = lineBuffer.length() - 1;

        // Get rest of line. lineBuffer filled as side-effect...
        try { readLineBytes(); } catch (IOException e) {}


        return sourceLine.toString() + makePointer(errorLocation);
    }

    protected String makePointer(int length) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < length; i++) {
            buffer.append(' ');
        }
        buffer.append('^');

        return buffer.toString();
    }

    // Super slow codepoint reader when we detect non-asci chars
    public int readCodepoint(int first, Encoding encoding) throws IOException {
        int count;
        byte[] value = new byte[6];

        // We know this will never be EOF
        value[0] = (byte) first;

        for (count = 1; count < 6; count++) {
            int c = read();
            if (c == RipperLexer.EOF) break; // Maybe we have enough bytes read to mbc at EOF.
            value[count] = (byte) c;
        }

        int length = encoding.length(value, 0, count);
        if (length < 0) {
            return -2; // TODO: Hack
        }

        int codepoint = encoding.mbcToCode(value, 0, length);
        for (int i = count - 1; i >= length; i--) {
            unread(value[i]);
        }

        return codepoint;
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
            
            if (c == -1) return RipperLexer.EOF;
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
        if (c == RipperLexer.EOF) return;
        
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

    public ByteList readLineBytes() throws IOException {
        ByteList bytelist = new ByteList(80);

        for (int c = read(); c != '\n' && c != RipperLexer.EOF; c = read()) {
            bytelist.append(c);
        }

        return bytelist;
    }
    
    public ByteList readLineBytesPlusNewline() throws IOException {
        ByteList bytelist = new ByteList(80);

        int c = read();
        for (; c != '\n' && c != RipperLexer.EOF; c = read()) {
            bytelist.append(c);
        }
        if (c != RipperLexer.EOF) bytelist.append(c);

        return bytelist;        
    }
    
    public int skipUntil(int marker) throws IOException {
        int c;
        for (c = read(); c != marker && c != RipperLexer.EOF; c = read()) {}
        return c;
    }

    public void unreadMany(CharSequence buffer) {
        int length = buffer.length();
        for (int i = length - 1; i >= 0; i--) {
            unread(buffer.charAt(i));
        }
    }

    /**
     * Match marker against input consumering lexer source as it goes...Unless it does not match
     * then it reverts lexer source back to point when this method was invoked.
     * 
     * @param marker to match against
     * @param indent eat any leading whitespace
     * @param withNewline includes a check that marker is followed by newline or EOF
     * @return 0 if no match -1 is EOF and '\n' if newline (only if withNewline is true).
     * @throws IOException if an error occurred reading from underlying IO source
     */
    public int matchMarker(ByteList match, boolean indent, boolean checkNewline) throws IOException {
        int length = match.length();
        ByteList buffer = new ByteList(length + 1);
        
        if (indent) {
            indentLoop(buffer);
        }
        
        if (!matches(match, buffer, length)) return 0;
        
        return finishMarker(checkNewline, buffer); 
    }

    private void indentLoop(ByteList buffer) throws IOException {
        int c;
        while ((c = read()) != RipperLexer.EOF) {
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

    private int finishMarker(boolean checkNewline, ByteList buffer) throws IOException {

        if (!checkNewline) return -1;

        int c = read();

        if (c == RipperLexer.EOF) return -1;
        if (c == '\n') return '\n';

        buffer.append(c);
        unreadMany(buffer);

        return 0;
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
    
    static final ByteList EOF_LABEL = new ByteList(new byte[] {'{', 'e', 'o', 'f', '}'});

    @Override
    public String toString() {
        try {
            ByteList buffer = new ByteList(20);
            ByteList unreadBuffer = new ByteList(20);

            if (twoAgo != -1 && twoAgo != 0) buffer.append(twoAgo);
            if (oneAgo != -1 && oneAgo != 0) buffer.append(oneAgo);

            buffer.append('<');

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
            int i = 1;
            
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

    public ByteList readUntil(char marker) throws IOException {
        ByteList buffer = new ByteList(20);
        int c;
        
        for (c = read(); c != marker && c != RipperLexer.EOF; c = read()) {
            buffer.append(c);
        }
        
        if (c == RipperLexer.EOF) return null;
        
        unread(c);
        
        return buffer;
    }

    public InputStream getRemainingAsStream() throws IOException {
        return bufferEntireStream(in);
    }

    private InputStream bufferEntireStream(InputStream stream) throws IOException {
        byte[] allBytes = new byte[0];
        byte[] b = new byte[1024];
        int bytesRead;
        while ((bytesRead = stream.read(b)) != -1) {
            byte[] newbuf = new byte[allBytes.length + bytesRead];
            System.arraycopy(allBytes, 0, newbuf, 0, allBytes.length);
            System.arraycopy(b, 0, newbuf, allBytes.length, bytesRead);
            allBytes = newbuf;
        }

        return new ByteArrayInputStream(allBytes);
    }    
}
