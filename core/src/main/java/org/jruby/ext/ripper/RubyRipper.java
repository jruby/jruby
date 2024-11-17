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
import static org.jruby.api.Create.newSymbol;
import static org.jruby.lexer.LexingCommon.*;

public class RubyRipper extends RubyObject {
    public static void initRipper(Ruby runtime) {
        var context = runtime.getCurrentContext();
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), RubyRipper::new);
        
        ripper.defineConstant("SCANNER_EVENT_TABLE", createScannerEventTable(context));
        ripper.defineConstant("PARSER_EVENT_TABLE", createParserEventTable(context));
        defineLexStateConstants(context, ripper);

        ripper.defineAnnotatedMethods(RubyRipper.class);
    }

    private static void defineLexStateConstants(ThreadContext context, RubyClass ripper) {
        for (int i = 0; i < lexStateNames.length; i++) {
            ripper.defineConstant("EXPR_" + lexStateNames[i], newFixnum(context, lexStateValues[i]));
        }
    }
    
    // Creates mapping table of token to arity for on_* method calls for the scanner support
    private static RubyHash createScannerEventTable(ThreadContext context) {
        RubyHash hash = new RubyHash(context.runtime);

        hash.fastASet(newSymbol(context, "CHAR"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "__end__"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "backref"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "backtick"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "comma"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "comment"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "const"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "cvar"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embdoc"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embdoc_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embdoc_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embexpr_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embexpr_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "embvar"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "float"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "gvar"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "heredoc_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "heredoc_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "ident"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "ignored_nl"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "imaginary"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "int"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "ivar"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "kw"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "label"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "label_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "lbrace"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "lbracket"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "lparen"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "nl"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "op"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "period"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "qsymbols_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "qwords_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "rational"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "rbrace"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "rbracket"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "regexp_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "regexp_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "rparen"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "semicolon"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "sp"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "symbeg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "symbols_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "tlambda"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "tlambeg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "tstring_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "tstring_content"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "tstring_end"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "words_beg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "words_sep"), newFixnum(context, 1));
        
        return hash;
    }
    
    // Creates mapping table of token to arity for on_* method calls for the parser support    
    private static RubyHash createParserEventTable(ThreadContext context) {
        RubyHash hash = new RubyHash(context.runtime);

        hash.fastASet(newSymbol(context, "BEGIN"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "END"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "alias"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "alias_error"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "aref"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "aref_field"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "arg_ambiguous"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "arg_paren"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "args_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "args_add_block"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "args_add_star"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "args_forward"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "args_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "array"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "aryptn"), newFixnum(context, 4));
        hash.fastASet(newSymbol(context, "assign"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "assign_error"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "assoc_new"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "assoc_splat"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "assoclist_from_args"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "bare_assoc_hash"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "begin"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "binary"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "block_var"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "blockarg"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "bodystmt"), newFixnum(context, 4));
        hash.fastASet(newSymbol(context, "brace_block"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "break"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "call"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "case"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "class"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "class_name_error"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "command"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "command_call"), newFixnum(context, 4));
        hash.fastASet(newSymbol(context, "const_path_field"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "const_path_ref"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "const_ref"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "def"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "defined"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "defs"), newFixnum(context, 5));
        hash.fastASet(newSymbol(context, "do_block"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "dot2"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "dot3"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "dyna_symbol"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "else"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "elsif"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "ensure"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "excessed_comma"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "fcall"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "field"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "fndptn"), newFixnum(context, 4));
        hash.fastASet(newSymbol(context, "for"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "hash"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "heredoc_dedent"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "hshptn"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "if"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "if_mod"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "ifop"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "in"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "kwrest_param"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "lambda"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "magic_comment"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "massign"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "method_add_arg"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "method_add_block"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mlhs_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mlhs_add_post"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mlhs_add_star"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mlhs_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "mlhs_paren"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "module"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mrhs_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mrhs_add_star"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "mrhs_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "mrhs_new_from_args"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "next"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "nokw_param"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "opassign"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "operator_ambiguous"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "param_error"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "params"), newFixnum(context, 7));
        hash.fastASet(newSymbol(context, "paren"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "parse_error"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "program"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "qsymbols_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "qsymbols_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "qwords_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "qwords_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "redo"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "regexp_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "regexp_literal"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "regexp_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "rescue"), newFixnum(context, 4));
        hash.fastASet(newSymbol(context, "rescue_mod"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "rest_param"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "retry"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "return"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "return0"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "sclass"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "stmts_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "stmts_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "string_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "string_concat"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "string_content"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "string_dvar"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "string_embexpr"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "string_literal"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "super"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "symbol"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "symbol_literal"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "symbols_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "symbols_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "top_const_field"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "top_const_ref"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "unary"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "undef"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "unless"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "unless_mod"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "until"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "until_mod"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "var_alias"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "var_field"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "var_ref"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "vcall"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "void_stmt"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "when"), newFixnum(context, 3));
        hash.fastASet(newSymbol(context, "while"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "while_mod"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "word_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "word_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "words_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "words_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "xstring_add"), newFixnum(context, 2));
        hash.fastASet(newSymbol(context, "xstring_literal"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "xstring_new"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "yield"), newFixnum(context, 1));
        hash.fastASet(newSymbol(context, "yield0"), newFixnum(context, 0));
        hash.fastASet(newSymbol(context, "zsuper"), newFixnum(context, 0));

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
