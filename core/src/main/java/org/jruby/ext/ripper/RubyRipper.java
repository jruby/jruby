/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
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

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.ByteListLexerSource;
import org.jruby.lexer.GetsLexerSource;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newFixnum;
import static org.jruby.lexer.LexingCommon.*;

public class RubyRipper extends RubyObject {
    public static void initRipper(Ruby runtime) {
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), RubyRipper::new);
        
        ripper.defineConstant("SCANNER_EVENT_TABLE", createScannerEventTable(runtime));
        ripper.defineConstant("PARSER_EVENT_TABLE", createParserEventTable(runtime));
        defineLexStateConstants(runtime, ripper);

        ripper.defineAnnotatedMethods(RubyRipper.class);
    }

    private static void defineLexStateConstants(Ruby runtime, RubyClass ripper) {
        for (int i = 0; i < lexStateNames.length; i++) {
            ripper.defineConstant("EXPR_" + lexStateNames[i], runtime.newFixnum(lexStateValues[i]));
        }
    }
    
    // Creates mapping table of token to arity for on_* method calls for the scanner support
    private static RubyHash createScannerEventTable(Ruby runtime) {
        RubyHash hash = new RubyHash(runtime);
        ThreadContext context = runtime.getCurrentContext();

        hash.fastASet(runtime.newSymbol("CHAR"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("__end__"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("backref"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("backtick"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("comma"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("comment"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("const"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("cvar"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embdoc"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embdoc_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embdoc_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embexpr_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embexpr_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("embvar"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("float"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("gvar"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("heredoc_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("heredoc_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("ident"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("ignored_nl"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("imaginary"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("int"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("ivar"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("kw"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("label"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("label_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("lbrace"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("lbracket"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("lparen"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("nl"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("op"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("period"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("qsymbols_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("qwords_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("rational"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("rbrace"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("rbracket"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("regexp_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("regexp_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("rparen"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("semicolon"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("sp"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("symbeg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("symbols_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("tlambda"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("tlambeg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("tstring_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("tstring_content"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("tstring_end"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("words_beg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("words_sep"), newFixnum(context, 1));
        
        return hash;
    }
    
    // Creates mapping table of token to arity for on_* method calls for the parser support    
    private static RubyHash createParserEventTable(Ruby runtime) {
        RubyHash hash = new RubyHash(runtime);
        ThreadContext context = runtime.getCurrentContext();

        hash.fastASet(runtime.newSymbol("BEGIN"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("END"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("alias"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("alias_error"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("aref"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("aref_field"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("arg_ambiguous"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("arg_paren"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("args_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("args_add_block"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("args_add_star"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("args_forward"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("args_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("array"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("aryptn"), newFixnum(context, 4));
        hash.fastASet(runtime.newSymbol("assign"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("assign_error"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("assoc_new"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("assoc_splat"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("assoclist_from_args"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("bare_assoc_hash"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("begin"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("binary"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("block_var"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("blockarg"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("bodystmt"), newFixnum(context, 4));
        hash.fastASet(runtime.newSymbol("brace_block"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("break"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("call"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("case"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("class"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("class_name_error"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("command"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("command_call"), newFixnum(context, 4));
        hash.fastASet(runtime.newSymbol("const_path_field"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("const_path_ref"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("const_ref"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("def"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("defined"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("defs"), newFixnum(context, 5));
        hash.fastASet(runtime.newSymbol("do_block"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("dot2"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("dot3"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("dyna_symbol"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("else"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("elsif"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("ensure"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("excessed_comma"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("fcall"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("field"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("fndptn"), newFixnum(context, 4));
        hash.fastASet(runtime.newSymbol("for"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("hash"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("heredoc_dedent"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("hshptn"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("if"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("if_mod"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("ifop"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("in"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("kwrest_param"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("lambda"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("magic_comment"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("massign"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("method_add_arg"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("method_add_block"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mlhs_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mlhs_add_post"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mlhs_add_star"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mlhs_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("mlhs_paren"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("module"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mrhs_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mrhs_add_star"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("mrhs_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("mrhs_new_from_args"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("next"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("nokw_param"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("opassign"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("operator_ambiguous"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("param_error"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("params"), newFixnum(context, 7));
        hash.fastASet(runtime.newSymbol("paren"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("parse_error"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("program"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("qsymbols_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("qsymbols_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("qwords_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("qwords_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("redo"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("regexp_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("regexp_literal"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("regexp_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("rescue"), newFixnum(context, 4));
        hash.fastASet(runtime.newSymbol("rescue_mod"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("rest_param"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("retry"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("return"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("return0"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("sclass"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("stmts_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("stmts_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("string_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("string_concat"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("string_content"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("string_dvar"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("string_embexpr"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("string_literal"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("super"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("symbol"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("symbol_literal"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("symbols_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("symbols_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("top_const_field"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("top_const_ref"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("unary"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("undef"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("unless"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("unless_mod"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("until"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("until_mod"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("var_alias"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("var_field"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("var_ref"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("vcall"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("void_stmt"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("when"), newFixnum(context, 3));
        hash.fastASet(runtime.newSymbol("while"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("while_mod"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("word_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("word_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("words_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("words_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("xstring_add"), newFixnum(context, 2));
        hash.fastASet(runtime.newSymbol("xstring_literal"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("xstring_new"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("yield"), newFixnum(context, 1));
        hash.fastASet(runtime.newSymbol("yield0"), newFixnum(context, 0));
        hash.fastASet(runtime.newSymbol("zsuper"), newFixnum(context, 0));

        return hash;
    }
    
    private RubyRipper(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src) {
        return initialize(context, src, null, null);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject file) {
        return initialize(context, src, file, null);
    }
    
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src,IRubyObject file, IRubyObject line) {
        filename = filenameAsString(context, file).dup();
        parser = new RipperParser(context, this, source(context, src, filename.asJavaString(), lineAsInt(context, line)));
         
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject column(ThreadContext context) {
        if (!parser.hasStarted()) throw context.runtime.newArgumentError("method called for uninitialized object");
            
        if (!parseStarted) return context.nil;
        
        return asFixnum(context, parser.getColumn());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(parser.encoding());
    }

    @JRubyMethod(name = "end_seen?")
    public IRubyObject end_seen_p(ThreadContext context) {
        return asBoolean(context, parser.isEndSeen());
    }

    @JRubyMethod(name = "error?")
    public IRubyObject error_p(ThreadContext context) {
        return asBoolean(context, parser.isError());
    }
    @JRubyMethod
    public IRubyObject filename(ThreadContext context) {
        return filename;
    }

    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        if (!parser.hasStarted()) throw context.runtime.newArgumentError("method called for uninitialized object");
        
        if (!parseStarted) return context.nil;
            
        return asFixnum(context, parser.getLineno());
    }

    @JRubyMethod
    public IRubyObject state(ThreadContext context) {
        int state = parser.getState();

        return state == 0 ? context.nil : asFixnum(context, state);
    }

    @JRubyMethod
    public IRubyObject token(ThreadContext context) {
        return context.runtime.newString(parser.lexer.tokenByteList());
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context) {
        parseStarted = true;
        
        try {
            return parser.parse(true);
        } catch (IOException e) {
            System.out.println("ERRROR: " + e);
        } catch (SyntaxException e) {
            
        }
        return context.nil;
    }    

    @JRubyMethod
    public IRubyObject yydebug(ThreadContext context) {
        return asBoolean(context, parser.getYYDebug());
    }
    
    @JRubyMethod(name = "yydebug=")
    public IRubyObject yydebug_set(ThreadContext context, IRubyObject arg) {
        parser.setYYDebug(arg.isTrue());
        return arg;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject dedent_string(ThreadContext context, IRubyObject self, IRubyObject _input, IRubyObject _width) {
        RubyString input = _input.convertToString();
        int wid = _width.convertToInteger().getIntValue();
        input.modifyAndClearCodeRange();
        int col = LexingCommon.dedent_string(input.getByteList(), wid);
        return asFixnum(context, col);
    }

    @JRubyMethod
    public IRubyObject dedent_string(ThreadContext context, IRubyObject _input, IRubyObject _width) {
        return dedent_string(context, this, _input, _width);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject lex_state_name(ThreadContext context, IRubyObject self, IRubyObject lexStateParam) {
        int lexState = lexStateParam.convertToInteger().getIntValue();

        boolean needsSeparator = false;
        RubyString name = null;
        for (int i = 0; i < singleStateLexStateNames; i++) {
            if ((lexState & lexStateValues[i]) != 0) {
                if (!needsSeparator) {
                    name = context.runtime.newString(lexStateNames[i]);
                    needsSeparator = true;
                } else {
                    name.cat('|');
                    name.catString(lexStateNames[i]);
                }
            }
        }

        if (name == null) name = context.runtime.newString("EXPR_NONE");

        return name;
    }
    
    private LexerSource source(ThreadContext context, IRubyObject src, String filename, int lineno) {
        // FIXME: respond_to? returns private methods
        DynamicMethod method = src.getMetaClass().searchMethod("gets");
        
        if (method.isUndefined() || method.getVisibility() == Visibility.PRIVATE) {
            return new ByteListLexerSource(filename, lineno, src.convertToString().getByteList(), null);
        }

        return new GetsLexerSource(filename, lineno, src, null);
    }
    
    private IRubyObject filenameAsString(ThreadContext context, IRubyObject filename) {
        if (filename == null || filename.isNil()) return context.runtime.newString("(ripper)");
        
        return filename.convertToString();
    }

    private int lineAsInt(ThreadContext context, IRubyObject line) {
        if (line == null || line.isNil()) return 0;

        return RubyNumeric.fix2int(line.convertToInteger()) - 1;
    }

    private RipperParserBase parser = null;
    private IRubyObject filename = null;
    private boolean parseStarted = false;

    // Number of expr states which represent a distinct value.  unions expr values occur after these.
    private static int singleStateLexStateNames = 13; // EXPR_BEG -> EXPR_FITEM
    private static String[] lexStateNames = new String[] {
            "BEG", "END", "ENDARG", "ENDFN", "ARG", "CMDARG", "MID",
            "FNAME", "DOT", "CLASS", "LABEL", "LABELED", "FITEM", // end of single states
            "VALUE", "BEG_ANY", "ARG_ANY", "END_ANY"
    };

    private static int[] lexStateValues = new int[] {
            EXPR_BEG, EXPR_END, EXPR_ENDARG, EXPR_ENDFN, EXPR_ARG, EXPR_CMDARG, EXPR_MID, EXPR_FNAME,
            EXPR_DOT, EXPR_CLASS, EXPR_LABEL, EXPR_LABELED, EXPR_FITEM, EXPR_VALUE, EXPR_BEG_ANY,
            EXPR_ARG_ANY, EXPR_END_ANY
    };

}
