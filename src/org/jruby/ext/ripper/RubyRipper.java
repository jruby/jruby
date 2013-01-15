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
 * Copyright (C) 2013 The JRuby Team (jruby@jruby.org)
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
package org.jruby.ext.ripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.lexer.yacc.InputStreamLexerSource;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyRipper extends RubyObject {
    public static void initRipper(Ruby runtime) {
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyRipper(runtime, klazz);
            }
        });
        
        ripper.defineConstant("SCANNER_EVENT_TABLE", createScannerEventTable(runtime, ripper));
        ripper.defineConstant("PARSER_EVENT_TABLE", createParserEventTable(runtime, ripper));
        
        ripper.defineAnnotatedMethods(RubyRipper.class);
    }
    
    private static IRubyObject createScannerEventTable(Ruby runtime, RubyClass ripper) {
        RubyHash hash = new RubyHash(runtime);
        
        hash.fastASet(runtime.newString("CHAR"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("__end__"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("backref"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("backtick"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("comma"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("comment"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("const"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("cvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embdoc"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embdoc_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embdoc_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embexpr_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embexpr_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("embvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("float"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("gvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("heredoc_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("heredoc_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("ident"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("ignored_nl"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("int"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("ivar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("kw"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("label"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("lbrace"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("lbracket"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("lparen"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("nl"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("op"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("period"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("qwords_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("rbrace"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("rbracket"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("regexp_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("regexp_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("rparen"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("semicolon"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("sp"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("symbeg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("tlambda"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("tlambeg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("tstring_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("tstring_content"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("tstring_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("words_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newString("words_sep"), runtime.newFixnum(1));
        
        return hash;
    }
    
    private static IRubyObject createParserEventTable(Ruby runtime, RubyClass ripper) {
        IRubyObject idBackref = runtime.newString("on_backref");
        IRubyObject idBacktick = runtime.newString("on_backtick");
        IRubyObject idComma = runtime.newString("on_comma");
        IRubyObject idConst = runtime.newString("on_const");
        IRubyObject idCvar = runtime.newString("on_cvar");
        IRubyObject idEmbexpBeg = runtime.newString("on_embexpr_beg");
        IRubyObject idEmbexpEnd = runtime.newString("on_embexpr_end");
        IRubyObject idEmbvar = runtime.newString("on_embvar");
        IRubyObject idFloat = runtime.newString("on_float");
        IRubyObject idGvar = runtime.newString("on_gvar");
        IRubyObject idIndent = runtime.newString("on_indent");
        IRubyObject idInt = runtime.newString("on_int");
        IRubyObject idIvar = runtime.newString("on_ivar");
        IRubyObject idKw = runtime.newString("on_kw");
        IRubyObject idLbrace = runtime.newString("on_lbrace");
        IRubyObject idLbracket = runtime.newString("on_lbracket");
        IRubyObject idLparen = runtime.newString("on_lparen");
        IRubyObject idNl = runtime.newString("on_nl");
        IRubyObject idOp = runtime.newString("on_op");
        IRubyObject idPeriod = runtime.newString("on_period");
        IRubyObject idRbrace = runtime.newString("on_rbrace");
        IRubyObject idRbracket = runtime.newString("on_rbracket");
        IRubyObject idRparen = runtime.newString("on_rparen");
        IRubyObject idSemicolon = runtime.newString("on_semicolon");
        IRubyObject idSymbeg = runtime.newString("on_symbeg");
        IRubyObject idTstringBeg = runtime.newString("on_tstring_beg");
        IRubyObject idTstringContent = runtime.newString("on_tstring_content");
        IRubyObject idTstringEnd = runtime.newString("on_tstring_end");
        IRubyObject idWordsBeg = runtime.newString("on_words_beg");
        IRubyObject idQwordsBeg = runtime.newString("on_qwords_beg");
        IRubyObject idWordsSep = runtime.newString("on_words_sep");
        IRubyObject idRegexpBeg = runtime.newString("on_regexp_beg");
        IRubyObject idRegexpEnd = runtime.newString("on_regexp_end");
        IRubyObject idLabel = runtime.newString("on_label");
        IRubyObject idTlambda = runtime.newString("on_tlambda");
        IRubyObject idTlambeg = runtime.newString("on_tlambeg");
        IRubyObject idIgnoredNL = runtime.newString("on_ignored_nl");
        IRubyObject idComment = runtime.newString("on_comment");
        IRubyObject idEmbdocBeg = runtime.newString("on_embdoc_beg");
        IRubyObject idEmbdoc = runtime.newString("on_embdoc");
        IRubyObject idEmbdocEnd = runtime.newString("on_embdoc_end");
        IRubyObject idSp = runtime.newString("on_sp");
        IRubyObject idHeredocBeg = runtime.newString("on_heredoc_beg");
        IRubyObject idHeredocEnd = runtime.newString("on_heredoc_end");
        IRubyObject id__end__ = runtime.newString("on___end__");
        IRubyObject id_CHAR = runtime.newString("on_CHAR");
                                                                                                        
        
        RubyHash hash = new RubyHash(runtime);
//        hash.fastASet(runtime.newFixnum(' '), idWordsSep);
//        hash.fastASet(runtime.newFixnum('+'), idOp);


        return hash;
    }
    
    private RubyRipper(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src) {
        return initialize(context, src, null, null);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject file) {
        return initialize(context, src, file, null);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src,IRubyObject file, IRubyObject line) {
        String stringSource = sourceAsString(context, src);
        filename = filenameAsString(context, file);
        int lineno = lineAsInt(context, line);
        ByteArrayInputStream bos = new ByteArrayInputStream(stringSource.getBytes());
        LexerSource source = new InputStreamLexerSource(filename.asJavaString(), bos, null, lineno, true);
        parser = new Ripper19Parser(context, this, source);
         
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject column(ThreadContext context) {
        return context.runtime.newFixnum(parser.getColumn());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return null;
    }

    @JRubyMethod(name = "end_seen?")
    public IRubyObject end_seen_p(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod
    public IRubyObject filename(ThreadContext context) {
        return filename;
    }

    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        return context.runtime.newFixnum(parser.getLineno());
    }
    
    @JRubyMethod
    public IRubyObject parse(ThreadContext context) {
        try {
            parser.parse(true);
        } catch (IOException e) {
            System.out.println("ERRROR: " + e);
        }
        return null;
    }    

    @JRubyMethod
    public IRubyObject yydebug(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod(name = "yydebug=")
    public IRubyObject yydebug_set(ThreadContext context, IRubyObject arg0) {
        return null;
    }
    
    private String sourceAsString(ThreadContext context, IRubyObject src) {
        // FIXME: WTF...respondsTo is true? for a string
        System.out.println("RESPONDS_TO: " + src + "SC:"+ src.getClass()  +", RS: " + src.respondsTo("gets"));
/*        if (!src.respondsTo("gets"))*/ return src.convertToString().asJavaString();

/*        return src.callMethod(context, "gets").asJavaString();*/
    }
    
    private IRubyObject filenameAsString(ThreadContext context, IRubyObject filename) {
        if (filename == null || filename.isNil()) return context.runtime.newString("(ripper)");
        
        return filename.convertToString();
    }
    
    private int lineAsInt(ThreadContext context, IRubyObject line) {
        if (line == null || line.isNil()) return 0;
        
        return RubyNumeric.fix2int(line.convertToInteger());
    }
    
    private RipperParser parser = null;
    private IRubyObject filename = null;
}
