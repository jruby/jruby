/*
 * LexState.java
 * Created on 22.02.2002, 22:51:53
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "JRuby" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called
 *    "JRuby", nor may "JRuby" appear in their name, without prior
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * ====================================================================
 *
 */
package org.jruby.lexer.yacc;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class LexState {
    public static final LexState EXPR_BEG    = new LexState("EXPR_BEG");
    public static final LexState EXPR_END    = new LexState("EXPR_END");
    public static final LexState EXPR_ARG    = new LexState("EXPR_ARG");
    public static final LexState EXPR_CMDARG = new LexState("EXPR_CMDARG");
    public static final LexState EXPR_ENDARG = new LexState("EXPR_ENDARG");
    public static final LexState EXPR_MID    = new LexState("EXPR_MID");
    public static final LexState EXPR_FNAME  = new LexState("EXPR_FNAME");
    public static final LexState EXPR_DOT    = new LexState("EXPR_DOT");
    public static final LexState EXPR_CLASS  = new LexState("EXPR_CLASS");

    private final String debug;

    private LexState(String debug) {
        this.debug = debug;
    }

    public boolean isExprBeg() {
        return this == EXPR_BEG;
    }

    public boolean isExprEnd() {
        return this == EXPR_END;
    }

    public boolean isExprArg() {
        return this == EXPR_ARG;
    }

    public boolean isExprCmdArg() {
        return this == EXPR_CMDARG;
    }

    public boolean isExprEndArg() {
        return this == EXPR_ENDARG;
    }

    public boolean isExprMid() {
        return this == EXPR_MID;
    }

    public boolean isExprFName() {
        return this == EXPR_FNAME;
    }

    public boolean isExprDot() {
        return this == EXPR_DOT;
    }

    public boolean isExprClass() {
        return this == EXPR_CLASS;
    }
    
    public boolean isArgument () {
        return this == EXPR_ARG || this == EXPR_CMDARG;
    }

    public String toString() {
        return debug;
    }
}