/*
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
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
    private String sourceName;
    
    // Number of newlines read from the reader
    private int line = 0;
    
    // Column of source.  
    private int column = 0;

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
    	}

    	// Reset column back to zero on first read of a line (note it will be-
    	// come '1' by the time it leaves read().
    	if (nextCharIsOnANewLine) {
    		nextCharIsOnANewLine = false;
    		column = 0;
    	}
    	
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
    	int length = buf.length();

    	if (c == '\n') {
    		line--;
    		column = ((Integer)lineWidths.get(line)).intValue();
    		nextCharIsOnANewLine = true;
    	} else {
    		column--;
    	}

    	buf.append(c);
    }
    
    public boolean peek(char to) {
        char c = read();
        unread(c);
        return c == to;
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
     * Where is the reader within the source {filename,row}
     * 
     * @return the current position
     */
    public SourcePosition getPosition() {
    	return SourcePosition.getInstance(sourceName, line+1);
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
        	if (c == '\r' && ((c = reader.read()) != '\n')) {
				unread((char)c);
				c = '\n';
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
}
