/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.lexer.yacc;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * This class is what feeds the lexer.  It is primarily a wrapper around a
 * Reader that can unread() data back onto the source.  Originally, I thought
 * about using the PushBackReader to handle read/unread, but I realized that
 * some extremely pathological case could overflow the pushback buffer.  Better
 * safe than sorry.  I could have combined this implementation with a 
 * PushbackBuffer, but the added complexity did not seem worth it.
 * 
 * @author enebo
 */
public class LexerSource {
	// Where we get new positions from.
	private ISourcePositionFactory positionFactory;
	
	// Where we get our newest char's
    private final Reader reader;
    
    // Our readback buffer.  Note:  StringBuffer's are thread-safe.
    // We have no requirement in LexerSource.
    private StringBuffer buf = new StringBuffer(100);
    
    // How long is every line we have run across.  This makes it
    // possible for us to unread() past a read() line and still
    // know what column we are at.
    private ArrayList lineWidths = new ArrayList();
    
    // The name of this source (e.g. a filename: foo.rb)
    private final String sourceName;
    
    // Number of newlines read from the reader
    private int line = 0;
    
    // Column of source.  
    private int column = 0;
    
    // How many bytes into the source are we?
    private int offset = 0;

    // Flag to let us now in next read after a newline that we should reset 
    // column
    private boolean nextCharIsOnANewLine = true;
	
    /**
     * Create our food-source for the lexer
     * 
     * @param sourceName is the file we are reading
     * @param reader is what represents the contents of file sourceName
     */
    public LexerSource(String sourceName, Reader reader) {
        this.sourceName = sourceName;
        this.reader = reader;
        this.positionFactory = new SourcePositionFactory(this);
    }
    
    public LexerSource(String sourceName, Reader reader, ISourcePositionFactory factory) {
        this.sourceName = sourceName;
        this.reader = reader;
        this.positionFactory = factory;
    }

    /**
     * Read next character from this source
     * 
     * @return next character to viewed by the source
     */
    public char read() {
    	int length = buf.length();
    	char c;
    	
    	if (length > 0) {
    		c = buf.charAt(length - 1);
    		buf.deleteCharAt(length - 1);
    	} else {
    		c = wrappedRead();
            
            // EOF...Do not advance column...Go straight to jail
            if (c == 0) {
                //offset++;
                return c;
            }
    	}

    	// Reset column back to zero on first read of a line (note it will be-
    	// come '1' by the time it leaves read().
    	if (nextCharIsOnANewLine) {
    		nextCharIsOnANewLine = false;
    		column = 0;
    	}
    	
    	offset++;
    	column++;
    	if (c == '\n') {
    		line++;
    		// Since we are not reading off of unread buffer we must at the
    		// end of a new line for the first time.  Add it.
    		if (length < 1) {
    			lineWidths.add(new Integer(column));
    		}
    		
    		nextCharIsOnANewLine = true;
        } 
            
    	return c; 
    }

    /**
     * Pushes char back onto this source.  Note, this also
     * allows us to push whatever is passes back into the source.
     * 
     * @param c to be put back onto the source
     */
    public void unread(char c) {
        if (c != (char) 0) {
            offset--;
    	
            if (c == '\n') {
                line--;
                column = ((Integer)lineWidths.get(line)).intValue();
                nextCharIsOnANewLine = true;
            } else {
                column--;
            }

            buf.append(c);
        }
    }
    
    public boolean peek(char to) {
        char c = read();
        unread(c);
        return c == to;
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
    
    /**
     * Are we at beggining of line?
     * 
     * @return the column (0..x)
     */
    public int getColumn() {
    	return column;
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
    public ISourcePosition getPosition(ISourcePosition startPosition, boolean inclusive) {
    	return positionFactory.getPosition(startPosition, inclusive);
    }
    
    /**
     * Where is the reader within the source {filename,row}
     * 
     * @return the current position
     */
    public ISourcePosition getPosition() {
    	return positionFactory.getPosition(null, false);
    }
    
    public ISourcePosition getDummyPosition() {
    	return positionFactory.getDummyPosition();
    }
    
    public ISourcePositionFactory getPositionFactory() {
        return positionFactory;
    }

    /**
     * Convenience method to hide exception.  If we do hit an exception
     * we will pretend we EOF'd.
     * 
     * @return the current char or EOF (at EOF or on error)
     */
    private char wrappedRead() {
        try {
            int c = reader.read();
        	
            // If \r\n then just pass along \n (windows)
            // If \r[^\n] then pass along \n (MAC)
            if (c == '\r') {
                if ((c = reader.read()) != '\n') {
                    unread((char)c);
                    c = '\n';
                } else {
                    // Position within source must reflect the actual offset and column.  Since
                	// we ate an extra character here (this accounting is normally done in read
                	// ), we should update position info.
                    offset++;
                    column++;
                }
            }
        	
            return c != -1 ? (char) c : '\0';
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Create a source.
     * 
     * @param name the name of the source (e.g a filename: foo.rb)
     * @param content the data of the source
     * @return the new source
     */
    public static LexerSource getSource(String name, Reader content) {
        return new LexerSource(name, content);
    }

    public String readLine() {
        StringBuffer sb = new StringBuffer(80);
        for (char c = read(); c != '\n' && c != '\0'; c = read()) {
            sb.append(c);
        }
        return sb.toString();
    }

    public void unreadMany(CharSequence buffer) {
    	int length = buffer.length();
        for (int i = length - 1; i >= 0; i--) {
            unread(buffer.charAt(i));
        }
    }

    public boolean matchString(String match, boolean indent) {
        int length = match.length();
        StringBuffer buffer = new StringBuffer(length + 20);
        
        if (indent) {
        	char c;
        	while ((c = read()) != '\0') {
        		if (!Character.isWhitespace(c)) {
        			unread(c);
        			break;
        		}
            	buffer.append(c);
        	}
        }
        
        for (int i = 0; i < length; i++) {
            char c = read();
            buffer.append(c);
            if (match.charAt(i) != c) {
                unreadMany(buffer);
                return false;
            }
        }
        return true;
    }

    public boolean wasBeginOfLine() {
        return getColumn() == 1;
    }

    public char readEscape() {
        char c = read();

        switch (c) {
            case '\\' : // backslash
                return c;
            case 'n' : // newline
                return '\n';
            case 't' : // horizontal tab
                return '\t';
            case 'r' : // carriage return
                return '\r';
            case 'f' : // form feed
                return '\f';
            case 'v' : // vertical tab
                return '\u0013';
            case 'a' : // alarm(bell)
                return '\u0007';
            case 'e' : // escape
                return '\u0033';
            case '0' : case '1' : case '2' : case '3' : // octal constant
            case '4' : case '5' : case '6' : case '7' :
                unread(c);
                return scanOct(3);
            case 'x' : // hex constant
            	int hexOffset = getColumn();
            	char hexValue = scanHex(2);
            	
            	// No hex value after the 'x'.
            	if (hexOffset == getColumn()) {
            	    throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
            	}
                return hexValue;
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = read()) != '-') {
                    throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
                } else if ((c = read()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == '\0') {
                    throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if ((c = read()) != '-') {
                    throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
                }
            case 'c' :
                if ((c = read()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\u0177';
                } else if (c == '\0') {
                    throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
                }
                return (char) (c & 0x9f);
            case '\0' :
                throw new SyntaxException(getPosition(null, false), "Invalid escape character syntax");
            default :
                return c;
        }
    }

    private char scanHex(int count) {
    	char value = '\0';

    	for (int i = 0; i < count; i++) {
    		char c = read();

    		if (!RubyYaccLexer.isHexChar(c)) {
        		unread(c);
    			break;
    		}

    		value <<= 4;
    		value |= Integer.parseInt(""+c, 16) & 15;
    	}

    	return value;
    }

    private char scanOct(int count) {
    	char value = '\0';

    	for (int i = 0; i < count; i++) {
    		char c = read();

    		if (!RubyYaccLexer.isOctChar(c)) {
        		unread(c);
    			break;
    		}

    		value <<= 3;
    		value |= Integer.parseInt(""+c, 8);
    	}

    	return value;
    }

    /**
     * Get character ahead of current position by offset positions.
     * 
     * @param anOffset is location past current position to get char at
     * @return character index positions ahead of source location or EOF
     */
    public char getCharAt(int anOffset) {
    	StringBuffer buffer = new StringBuffer(anOffset);
    
    	// read next offset chars
        for (int i = 0; i < anOffset; i++) {
            buffer.append(read());
        }
        
        int length = buffer.length();
        
        // Whoops not enough chars left EOF!
        if (length == 0){
        	return '\0';
        }
        
        // Push chars back now that we found it
        for (int i = 0; i < length; i++) {
            unread(buffer.charAt(i));
        }
        
        return buffer.charAt(length - 1);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(20);
        for (int i = 0; i < 20; i++) {
            buffer.append(read());
        }
        for (int i = 0; i < 20; i++) {
            unread(buffer.charAt(buffer.length() - i - 1));
        }
        buffer.append(" ...");
        return buffer.toString();
    }
}
