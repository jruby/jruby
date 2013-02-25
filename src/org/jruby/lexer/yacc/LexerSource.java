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
package org.jruby.lexer.yacc;

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
public abstract class LexerSource {
	// Where we get new positions from.
	private SimplePositionFactory positionFactory;
	
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

    /**
     * Create our food-source for the lexer
     * 
     * @param sourceName is the file we are reading
     * @param reader is what represents the contents of file sourceName
     * @param line starting line number for source (used by eval)
     * @param extraPositionInformation will gives us extra information that an IDE may want (deprecated)
     */
    protected LexerSource(String sourceName, List<String> list, int lineOffset, 
            boolean extraPositionInformation) {
        this.sourceName = sourceName;
        this.lineOffset = lineOffset;
        positionFactory = new SimplePositionFactory(this, line);
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
    public ISourcePosition getPosition(ISourcePosition startPosition) {
    	return positionFactory.getPosition(startPosition);
    }
    
    /**
     * Where is the reader within the source {filename,row}
     * 
     * @return the current position
     */
    public ISourcePosition getPosition() {
    	return positionFactory.getPosition(null);
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

    public static LexerSource getSource(String name, byte[] content, List<String> list,
            ParserConfiguration configuration) {
        return new ByteArrayLexerSource(name, content, list, configuration.getLineNumber(),
                configuration.hasExtraPositionInformation());
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
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < length; i++) {
            buf.append(' ');
        }
        buf.append('^');

        return buf.toString();
    }

    // Super slow codepoint reader when we detect non-asci chars
    public int readCodepoint(int first, Encoding encoding) throws IOException {
        int count = 0;
        byte[] value = new byte[6];

        // We know this will never be EOF
        value[0] = (byte) first;

        for (count = 1; count < 6; count++) {
            int c = read();
            if (c == RubyYaccLexer.EOF) break; // Maybe we have enough bytes read to mbc at EOF.
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
     * Match marker against input consumering lexer source as it goes...Unless it does not match
     * then it reverts lexer source back to point when this method was invoked.
     * 
     * @param marker to match against
     * @param indent eat any leading whitespace
     * @param withNewline includes a check that marker is followed by newline or EOF
     * @return true if marker matches...false otherwise
     * @throws IOException if an error occurred reading from underlying IO source
     */
    public abstract boolean matchMarker(ByteList marker, boolean indent, boolean withNewline) throws IOException;

    public abstract int read() throws IOException;
    public abstract ByteList readUntil(char c) throws IOException;
    public abstract ByteList readLineBytes() throws IOException;
    public abstract int skipUntil(int c) throws IOException;
    public abstract void unread(int c);
    public abstract void unreadMany(CharSequence line);
    public abstract boolean peek(int c) throws IOException;
    public abstract boolean lastWasBeginOfLine();
    public abstract boolean wasBeginOfLine();
    public abstract InputStream getRemainingAsStream() throws IOException;
}
