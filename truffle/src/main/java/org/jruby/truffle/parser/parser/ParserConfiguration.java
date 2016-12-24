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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.truffle.parser.parser;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.parser.scope.DynamicScope;
import org.jruby.truffle.parser.scope.StaticScope;

public class ParserConfiguration {
    private DynamicScope existingScope = null;
    private boolean asBlock = false;
    // What linenumber will the source think it starts from?
    private int lineNumber = 0;
    // Is this inline source (aka -e "...source...")
    private boolean inlineSource = false;
    // We parse evals more often in source so assume an eval parse.
    private boolean isEvalParse = true;
    // Should we display extra debug information while parsing?
    private boolean isDebug = false;
    // whether we should save the end-of-file data as DATA
    private boolean saveData = false;

    private boolean frozenStringLiteral = false;

    private Encoding defaultEncoding;
    private RubyContext context;

    public ParserConfiguration(RubyContext context, int lineNumber, boolean inlineSource, boolean isFileParse, boolean saveData) {
        this.context = context;
        this.inlineSource = inlineSource;
        this.lineNumber = lineNumber;
        this.isEvalParse = !isFileParse;
        this.saveData = saveData;
    }

    public void setFrozenStringLiteral(boolean frozenStringLiteral) {
        this.frozenStringLiteral = frozenStringLiteral;
    }

    public boolean isFrozenStringLiteral() {
        return frozenStringLiteral;
    }

    public void setDefaultEncoding(Encoding encoding) {
        this.defaultEncoding = encoding;
    }

    public Encoding getDefaultEncoding() {
        if (defaultEncoding == null) {
            defaultEncoding = UTF8Encoding.INSTANCE;
        }
        
        return defaultEncoding;
    }

    public boolean isDebug() {
        return isDebug;
    }

    /**
     * Is the requested parse for an eval()?
     * 
     * @return true if for eval
     */
    public boolean isEvalParse() {
        return isEvalParse;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * If we are performing an eval we should pass existing scope in.
     * Calling this lets the parser know we need to do this.
     * 
     * @param existingScope is the scope that captures new vars, etc...
     */
    public void parseAsBlock(DynamicScope existingScope) {
        this.asBlock = true;
        this.existingScope = existingScope;
    }

    public RubyContext getContext() {
        return context;
    }

    /**
     * This method returns the appropriate first scope for the parser.
     * 
     * @return correct top scope for source to be parsed
     */
    public DynamicScope getScope(String file) {
        if (asBlock) return existingScope;

        // FIXME: We should really not be creating the dynamic scope for the root
        // of the AST before parsing.  This makes us end up needing to readjust
        // this dynamic scope coming out of parse (and for local static scopes it
        // will always happen because of $~ and $_).
        // FIXME: Because we end up adjusting this after-the-fact, we can't use
        // any of the specific-size scopes.
        return new DynamicScope(new StaticScope(StaticScope.Type.LOCAL, (StaticScope) null, file), existingScope);
    }

    public boolean isCoverageEnabled() {
        return !isEvalParse();
    }

    /**
     * Get whether we are saving the DATA contents of the file.
     */
    public boolean isSaveData() {
        return saveData;
    }
    
    /**
     * Are we parsing source provided as part of the '-e' option to Ruby.
     * 
     * @return true if source is from -e option
     */
    public boolean isInlineSource() {
        return inlineSource;
    }
}
