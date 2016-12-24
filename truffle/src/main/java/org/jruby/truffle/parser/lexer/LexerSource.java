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
package org.jruby.truffle.parser.lexer;

import com.oracle.truffle.api.source.Source;
import org.jcodings.Encoding;
import org.jruby.truffle.core.string.ByteList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *  Lexer source for ripper when we have all bytes available to us.
 */
public class LexerSource {
    private final Source source;

    // Offset specified where to add to actual offset
    private int lineOffset;

    private final List<ByteList> scriptLines = new ArrayList<>();

    private ByteList completeSource; // The entire source of the file
    private int offset = 0; // Offset into source overall (mri: lex_gets_ptr)

    public LexerSource(Source source, int line, Encoding defaultEncoding) {
        this.source = source;
        this.lineOffset = line;
        this.completeSource = new ByteList(source.getCode().getBytes(StandardCharsets.UTF_8), defaultEncoding);
    }

    public Encoding getEncoding() {
        return completeSource.getEncoding();
    }

    public void setEncoding(Encoding encoding) {
        completeSource.setEncoding(encoding);
        encodeExistingScriptLines(encoding);
    }

    public ByteList gets() {
        int length = completeSource.length();
        if (offset >= length) return null; // At end of source/eof

        int end = completeSource.indexOf('\n', offset) + 1;
        if (end == 0) end = length;

        ByteList line = completeSource.makeShared(offset, end - offset);
        offset = end;

        if (scriptLines != null) scriptLines.add(line);

        return line;
    }

    public Source getSource() {
        return source;
    }

    public int getOffset() {
        return offset;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    public void encodeExistingScriptLines(Encoding encoding) {
        if (scriptLines == null) return;

        int length = scriptLines.size();
        for (int i = 0; i < length; i++) {
            ByteList line = scriptLines.get(0);

            line.setEncoding(encoding);
        }
    }

}
