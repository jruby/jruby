/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.lexer.yacc;

public class SyntaxException extends RuntimeException {
	private static final long serialVersionUID = -2130930815167932274L;
	
    private String file;
    private int line;

    public SyntaxException(String file, int line, String lastLine, String message, int start, int end) {
        super(prepareMessage(message, lastLine, start, end));

        this.file = file;
        this.line = line;
    }


    private static String prepareMessage(String message, String line, int start, int end) {
        if (line != null && line.length() > 5) {
            int start_line = start >> 16;
            int start_column = start & 0xffff;
            int end_line = end >> 16;
            int end_column = end & 0xffff;
            boolean addNewline = message != null && !message.endsWith("\n");
            message += (addNewline ? "\n" : "") + line;
            if (start_column >= 0) {
                addNewline = !line.endsWith("\n");
                String highlightLine = new String(new char[start_column]);
                highlightLine = highlightLine.replace("\0", " ") + "^";
                if (end_column - start_column > 1) {
                    String underscore = new String(new char[end_column - start_column - 1]);
                    underscore = underscore.replace("\0", "~");
                    highlightLine += underscore;
                }

                message += (addNewline ? "\n" : "") + highlightLine;
                return message;
            }
        }
        
        return message;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }
}
