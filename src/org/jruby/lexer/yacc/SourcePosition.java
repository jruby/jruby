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

import java.io.Serializable;

/**
 * 
 * Position within a source.  This could have column as well, but it currently
 * does not.  A normal ruby intrepretter does not use this information in 
 * error/warning information.  An IDE using this may need it though.  This is
 * trivially added if need be.
 * 
 */
public class SourcePosition implements ISourcePosition, Serializable {
    private static final long serialVersionUID = 3762529027281400377L;
    
    // The file of the source
    private final String file;
    
    // The row of the source
    private final int startLine;
    private final int endLine;
    
    private final int startOffset;
    private final int endOffset;

    /**
     * Creates a default source position - required for serialization.
     */
    public SourcePosition() {
    	this("", 0);
    }
    
    /**
     * Creates a new source position.
     * 
     * @param file location of the source (must not be null)
     * @param endLine what line within the source
     */
	public SourcePosition(String file, int endLine) {
		if (file == null) { //otherwise equals() and getInstance() will fail
			throw new NullPointerException();  
		}
		this.file = file;
		this.startLine = 0;
		this.endLine = endLine;
		this.startOffset = 0;
		this.endOffset = 0;
	}

    /**
     * Creates a new source position.
     * 
     * @param file location of the source (must not be null)
     * @param line what line within the source
     */
	public SourcePosition(String file, int startLine, int endLine, int startOffset, int endOffset) {
		if (file == null) { //otherwise equals() and getInstance() will fail
			throw new NullPointerException();  
		}
		this.file = file;
		this.startLine = startLine;
		this.endLine = endLine;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	/**
	 * @return the file of the source
	 */
    public String getFile() {
        return file;
    }
    
    /**
     * @return the line a token starts within the source
     */
    public int getStartLine() {
    	return startLine;
    }

    /**
     * @return the line within the source
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * @param object the object which should be compared
     * @return simple Object.equals() implementation
     */
    public boolean equals(Object object) {
    	if (object == this) {
    		return true;
    	}
        if (!(object instanceof SourcePosition)) {
        	return false;
        }
        
        SourcePosition other = (SourcePosition) object;

        return file.equals(other.file) && endLine == other.endLine;
    }

    /**
     * @return something we can use for identity in hashing, etc...
     */
    public int hashCode() {
        return file.hashCode() ^ endLine;
    }

    /**
     * @return simple Object.toString() implementation
     */
    public String toString() {
        return file + ":[" + startLine + "," + endLine + "]:[" + 
            getStartOffset() + "," + getEndOffset() + "]";
    }
    
    /**
     * Not used in interpreter 
     */
    public int getStartOffset() {
    	return startOffset;
    }
    
    /**
     * Not used in interpreter 
     */
    public int getEndOffset() {
    	return endOffset;
    }
}
