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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

public class SyntaxException extends RuntimeException {
    public enum PID {
        BAD_HEX_NUMBER("BAD_HEX_NUMBER"),
        BAD_IDENTIFIER("BAD_IDENTIFIER"),
        BAD_OCTAL_DIGIT("BAD_OCTAL_DIGIT"),
        BLOCK_ARG_UNEXPECTED("BLOCK_ARG_UNEXPECTED"), 
        BLOCK_ARG_AND_BLOCK_GIVEN("BLOCK_ARG_AND_BLOCK_GIVEN"),
        CHARACTER_BAD("CHARACTER_BAD"),
        CANNOT_CHANGE_SELF("CANNOT_CHANGE_SELF"),
        CVAR_BAD_NAME("CVAR_BAD_NAME"),
        DYNAMIC_CONSTANT_ASSIGNMENT("DYNAMIC_CONSTANT_ASSIGNMENT"),
        EMPTY_BINARY_NUMBER("EMPTY_BINARY_NUMBER"),
        FLOAT_MISSING_ZERO("FLOAT_MISSING_ZERO"),
        GRAMMAR_ERROR("GRAMMAR_ERROR"),
        INCOMPLETE_CHAR_SYNTAX("INCOMPLETE_CHAR_SYNTAX"), 
        INVALID_ASSIGNMENT("INVALID_ASSIGNMENT"),
        INVALID_ESCAPE_SYNTAX("INVALID_ESCAPE_SYNTAX"),
        IVAR_BAD_NAME("IVAR_BAD_NAME"),
        MULTIPLE_ASSIGNMENT_IN_CONDITIONAL("MULTIPLE_ASSIGNMENT_IN_CONDITIONAL"),
        REGEXP_UNKNOWN_OPTION("REGEXP_UNKNOWN_OPTION"),
        STRING_HITS_EOF("STRING_HITS_EOF"),
        STRING_MARKER_MISSING("STRING_MARKER_MISSING"), 
        STRING_UNKNOWN_TYPE("STRING_UNKNOWN_TYPE"), 
        TRAILING_UNDERSCORE_IN_NUMBER("TRAILING_UNDERSCORE_IN_NUMBER"),
        DUBY_EXTENSIONS_OFF("DUBY_EXTENSIONS_OFF"),
        BLOCK_GIVEN_TO_YIELD("BLOCK_GIVEN_TO_YIELD");
        
        private String id;
        
        PID(String id) {
            this.id = id;
        }
        
        public String getID() {
            return id;
        }
    }
    
	private static final long serialVersionUID = -2130930815167932274L;
	
    private ISourcePosition position;
    private PID pid;

    public SyntaxException(PID pid, ISourcePosition position, String lastLine, String message, Object... data) {
        super(prepareMessage(message, lastLine));

        this.pid = pid;
        this.position = position;
    }

    private static String prepareMessage(String message, String line) {
        if (line != null && line.length() > 5) {
            boolean addNewline = message != null && message.endsWith("\n");
            return message + (addNewline ? "\n" : "") + line;
        }
        
        return message;
    }

    public ISourcePosition getPosition() {
        return position;
    }
    
    public PID getPid() {
        return pid;
    }
}
