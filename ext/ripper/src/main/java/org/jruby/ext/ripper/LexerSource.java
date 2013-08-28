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
 * Copyright (C) 2013 Thomas E Enebo <enebo@acm.org>
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

import org.jcodings.Encoding;
import org.jruby.util.ByteList;

/**
 * Simple source capable of providing the next line in Ruby source file being lex'd.
 */
public class LexerSource {
    // The name of this source (e.g. a filename: foo.rb)
    private final String name; // mri: parser_ruby_sourcefile
    
    // The entire source of the file
    private ByteList completeSource;

    // Offset into source overall
    private int offset = 0; // mri: lex_gets_ptr
    
    // Offset specified where to add to actual offset
    private int lineOffset;

    /**
     * Create our food-source for the lexer
     * 
     * @param sourceName is the file we are reading
     * @param reader is what represents the contents of file sourceName
     * @param line starting line number for source (used by eval)
     * @param extraPositionInformation will gives us extra information that an IDE may want (deprecated)
     */
    protected LexerSource(String sourceName, byte[] in, int lineOffset) {
        this.completeSource = new ByteList(in);
        this.name = sourceName;
        this.lineOffset = lineOffset;
    }

    /**
     * What file are we lexing?
     * @return the files name
     */
    public String getFilename() {
    	return name;
    }
    
    public int getLineOffset() {
        return lineOffset;
    }
    
    public void setEncoding(Encoding encoding) {
        completeSource.setEncoding(encoding);
    }
    
    public ByteList gets() {
        int length = completeSource.length();
        if (offset == length) return null; // At end of source/eof

        int end = offset;
        
        while (end < length) {
            if (completeSource.get(end) == '\n') {
                end++; // include newline
                break;
            }
            end++;
        }

        ByteList line = completeSource.makeShared(offset, end - offset);

        offset = end;
        
        return line;
    }
}
