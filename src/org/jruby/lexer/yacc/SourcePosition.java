/*
 * ListNode.java - Base of all nodes which are a list
 * 
 * Copyright (C) 2004 Thomas E Enebo
 * Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
 package org.jruby.lexer.yacc;

import java.io.Serializable;

/**
 * 
 * Position within a source.  This could have column as well, but it currently
 * does not.  A normal ruby intrepretter does not use this information in 
 * error/warning information.  An IDE using this may need it though.  This is
 * trivially added if need be.
 * 
 * @author enebo
 */
public class SourcePosition implements Serializable {
	// Often times many nodes share the same position (LexerSource has commentary
	// on the weakness of this scheme).
    private static SourcePosition lastPosition = new SourcePosition("", -1);
    
    // The file of the source
    private String file;
    
    // The row of the source
    private int line;

    // For serialization purposes
    public SourcePosition() { super(); }
    
    /**
     * Create a new source position
     * 
     * @param file location of the source
     * @param line what line within the source
     */
	public SourcePosition(String file, int line) {
		this.file = file;
		this.line = line;
	}

	/**
	 * @return the file of the source
	 */
    public String getFile() {
        return file;
    }

    /**
     * @return the line within the source
     */
    public int getLine() {
        return line;
    }

    /**
     * @return simple Object.equals() implementation
     */
    public boolean equals(Object object) {
        if (object instanceof SourcePosition) {
        	return false;
        }
        
        SourcePosition other = (SourcePosition) object;

        return file.equals(other.file) && line == other.line;
    }

    /**
     * @return something we can use for identity in hashing, etc...
     */
    public int hashCode() {
        return file.hashCode() ^ line;
    }

    /**
     * @return simple Object.toString() implementation
     */
    public String toString() {
        return file + ":" + line;
    }

    /**
     * 
     * Extra simple caching mechanism.  By calling this instead of direct
     * instantiation, close grammatical elements will end up sharing the
     * same instance of SourcePosition.  This scheme will not work properly
     * in environment where multiple threads are parsing files.  The concept
     * of caching should be moved into LexerSource.
     * 
     * @param file for SourcePosition desired
     * @param line for SourcePosition desired
     * @return the source position
     */
    public static SourcePosition getInstance(String file, int line) {
        synchronized (SourcePosition.class) {
        	if (lastPosition.line == line && lastPosition.file.equals(file)) {
        		return lastPosition;
            }

            lastPosition = new SourcePosition(file, line);
        
            return lastPosition;
        }
    }
}
