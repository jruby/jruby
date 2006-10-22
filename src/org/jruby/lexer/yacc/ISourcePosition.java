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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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

/**
 * This interface is the combination of two needs:  1) A Ruby interpreter position (for warnings 
 * and errors).  The interpreter only cares about filename and endLine.  2) A IDE position (offsets
 * for showing ranges of grammatical elements).
 * 
 * The offsets represent positions 'between' characters.  So a variable 'abc' has a startOffset
 * of 0 and an endOffset of 3 ( 0a1b2c3 ).
 * 
 */
public interface ISourcePosition {
    /**
     * Which file does this source position live in?
     * 
     * @return name of the source file.
     */
	public String getFile();
    
    /**
     * Which is the first(start) line that this source position occurs on (zero-based)
     * @return
     */
	public int getStartLine();
    
    /**
     * Which is the last(end) line that this source position occurs on (zero-based)
     * 
     * @return the line
     */
	public int getEndLine();
    
    /**
     * Modify startOffset by a relativeValue.  At times our grammar and lexer do not give us
     * the exact positions we need so we need to manually tweak position.  The bummer of this
     * is this requires ISourcePosition implementations are mutable.  
     * 
     * @param relativeValue to nudge startOffset up or down
     */
    public void adjustStartOffset(int relativeValue);
    
    /** 
     * Get offset (relative to beginning of source file) immediately before first character 
     * represented by this source position.
     * 
     * @return the offset 
     */
	public int getStartOffset();
    
    /**
     * Get offset (relative to beginning of source file) immediately after the last character
     * represented by this source position
     *  
     * @return the offset
     */
	public int getEndOffset();
    
    /**
     * Calculates the logical union of the two positions and creates a new resulting position
     * 
     * @param position to be unioned against this position
     * @return a new position
     */
    public ISourcePosition union(ISourcePosition position);
}
