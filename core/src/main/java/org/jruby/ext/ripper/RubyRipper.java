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

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.lexer.LexingCommon.*;

public class RubyRipper extends RubyObject {
    public static RubyClass initRipper(ThreadContext context) {
        var Ripper = (RubyClass) defineClass(context, "Ripper", objectClass(context), RubyRipper::new).
                defineMethods(context, RubyRipper.class).
                defineConstant(context, "SCANNER_EVENT_TABLE", createScannerEventTable(context)).
                defineConstant(context, "PARSER_EVENT_TABLE", createParserEventTable(context));

        defineLexStateConstants(context, Ripper);

        return Ripper;
    }

    private static void defineLexStateConstants(ThreadContext context, RubyClass ripper) {
        for (int i = 0; i < lexStateNames.length; i++) {
            ripper.defineConstant(context, "EXPR_" + lexStateNames[i], asFixnum(context, lexStateValues[i]));
        }
    }
    
    // Creates mapping table of token to arity for on_* method calls for the scanner support
    private static RubyHash createScannerEventTable(ThreadContext context) {
        RubyHash hash = new RubyHash(context.runtime);

        hash.fastASet(asSymbol(context, "CHAR"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "__end__"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "backref"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "backtick"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "comma"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "comment"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "const"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "cvar"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embdoc"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embdoc_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embdoc_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embexpr_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embexpr_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "embvar"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "float"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "gvar"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "heredoc_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "heredoc_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "ident"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "ignored_nl"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "imaginary"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "int"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "ivar"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "kw"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "label"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "label_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "lbrace"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "lbracket"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "lparen"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "nl"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "op"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "period"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "qsymbols_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "qwords_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "rational"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "rbrace"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "rbracket"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "regexp_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "regexp_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "rparen"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "semicolon"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "sp"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "symbeg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "symbols_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "tlambda"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "tlambeg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "tstring_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "tstring_content"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "tstring_end"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "words_beg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "words_sep"), asFixnum(context, 1));
        
        return hash;
    }
    
    // Creates mapping table of token to arity for on_* method calls for the parser support    
    private static RubyHash createParserEventTable(ThreadContext context) {
        RubyHash hash = new RubyHash(context.runtime);

        hash.fastASet(asSymbol(context, "BEGIN"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "END"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "alias"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "alias_error"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "aref"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "aref_field"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "arg_ambiguous"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "arg_paren"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "args_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "args_add_block"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "args_add_star"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "args_forward"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "args_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "array"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "aryptn"), asFixnum(context, 4));
        hash.fastASet(asSymbol(context, "assign"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "assign_error"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "assoc_new"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "assoc_splat"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "assoclist_from_args"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "bare_assoc_hash"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "begin"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "binary"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "block_var"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "blockarg"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "bodystmt"), asFixnum(context, 4));
        hash.fastASet(asSymbol(context, "brace_block"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "break"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "call"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "case"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "class"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "class_name_error"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "command"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "command_call"), asFixnum(context, 4));
        hash.fastASet(asSymbol(context, "const_path_field"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "const_path_ref"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "const_ref"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "def"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "defined"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "defs"), asFixnum(context, 5));
        hash.fastASet(asSymbol(context, "do_block"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "dot2"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "dot3"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "dyna_symbol"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "else"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "elsif"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "ensure"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "excessed_comma"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "fcall"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "field"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "fndptn"), asFixnum(context, 4));
        hash.fastASet(asSymbol(context, "for"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "hash"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "heredoc_dedent"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "hshptn"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "if"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "if_mod"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "ifop"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "in"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "kwrest_param"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "lambda"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "magic_comment"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "massign"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "method_add_arg"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "method_add_block"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mlhs_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mlhs_add_post"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mlhs_add_star"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mlhs_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "mlhs_paren"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "module"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mrhs_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mrhs_add_star"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "mrhs_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "mrhs_new_from_args"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "next"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "nokw_param"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "opassign"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "operator_ambiguous"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "param_error"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "params"), asFixnum(context, 7));
        hash.fastASet(asSymbol(context, "paren"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "parse_error"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "program"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "qsymbols_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "qsymbols_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "qwords_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "qwords_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "redo"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "regexp_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "regexp_literal"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "regexp_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "rescue"), asFixnum(context, 4));
        hash.fastASet(asSymbol(context, "rescue_mod"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "rest_param"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "retry"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "return"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "return0"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "sclass"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "stmts_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "stmts_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "string_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "string_concat"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "string_content"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "string_dvar"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "string_embexpr"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "string_literal"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "super"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "symbol"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "symbol_literal"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "symbols_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "symbols_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "top_const_field"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "top_const_ref"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "unary"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "undef"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "unless"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "unless_mod"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "until"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "until_mod"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "var_alias"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "var_field"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "var_ref"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "vcall"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "void_stmt"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "when"), asFixnum(context, 3));
        hash.fastASet(asSymbol(context, "while"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "while_mod"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "word_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "word_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "words_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "words_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "xstring_add"), asFixnum(context, 2));
        hash.fastASet(asSymbol(context, "xstring_literal"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "xstring_new"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "yield"), asFixnum(context, 1));
        hash.fastASet(asSymbol(context, "yield0"), asFixnum(context, 0));
        hash.fastASet(asSymbol(context, "zsuper"), asFixnum(context, 0));

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
        if (!parser.hasStarted()) throw argumentError(context, "method called for uninitialized object");
            
        return !parseStarted ? context.nil : asFixnum(context, parser.getColumn());
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
        if (!parser.hasStarted()) throw argumentError(context, "method called for uninitialized object");
        
        return !parseStarted ? context.nil : asFixnum(context, parser.getLineno());
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
