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
import java.util.HashSet;
import java.util.Set;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.*;
import org.jruby.ast.DefHolder;
import org.jruby.ast.Node;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.LexContext;
import org.jruby.parser.NodeExits;
import org.jruby.parser.ProductionState;
import org.jruby.parser.RubyParserBase;
import org.jruby.parser.ScopedParserState;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.StackState;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.parser.RubyParserBase.IDType.AttrSet;
import static org.jruby.parser.RubyParserBase.IDType.Class;
import static org.jruby.parser.RubyParserBase.IDType.Constant;
import static org.jruby.parser.RubyParserBase.IDType.Global;
import static org.jruby.parser.RubyParserBase.IDType.Instance;
import static org.jruby.parser.RubyParserBase.IDType.Local;
import static org.jruby.util.CommonByteLists.*;
import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;

public class RipperParserBase {
    public RipperParserBase(ThreadContext context, IRubyObject ripper, LexerSource source) {
        this.context = context;
        this.ripper = ripper;
        this.lexer = new RubyLexer(this, source);
    }

    public void initTopLocalVariables() {
        scopedParserState = new ScopedParserState(null);
        pushLocalScope();
    }

    public void reset() {
        getLexContext().reset();
//        inSingleton = 0;
    }
    
    public Object yyparse (RubyLexer yyLex) throws java.io.IOException {
        return null;
    }
    
    public Object yyparse (RubyLexer yyLex, Object debugger) throws java.io.IOException {
        return null;
    }        
    
    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public IRubyObject parse(boolean isDebug) throws IOException {
        reset();
        
        lexer.parser_prepare();
        return (IRubyObject) yyparse(lexer, null);
    }    
    
    public IRubyObject arg_add_optblock(IRubyObject arg1, IRubyObject arg2) {
        // This has to be an MRI bug
        if (arg2 == null) return dispatch("on_args_add_block", arg1, getRuntime().getFalse());
        
        if (arg2.isNil()) return arg1;
        
        return dispatch("on_args_add_block", arg1, arg2);
    }

    public RubyString arg_var(ByteList byteName) {
        numparam_name(byteName);
        getCurrentScope().addVariableThisScope(symbolID(byteName).idString());
        return getRuntime().newString(byteName);
    }

    public IRubyObject assignableConstant(IRubyObject value) {
        if (isInDef()) {
            error();
            value = dispatch("on_assign_error", value);
        }
        return value;
    }

    public IRubyObject assignableIdentifier(IRubyObject value) {
        String ident = lexer.getIdent().intern();
        getCurrentScope().assign(lexer.getRubySourceline(), context.runtime.newSymbol(ident, lexer.getEncoding()), null);
        return value;
    }

    public IRubyObject dispatch(String method_name) {
        return Helpers.invoke(context, ripper, method_name);
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1));
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1), escape(arg2));
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1), escape(arg2), escape(arg3));
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1), escape(arg2), escape(arg3), escape(arg4));
    }    
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1), escape(arg2), escape(arg3), escape(arg4), escape(arg5));
    }

    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7) {
        return Helpers.invoke(context, ripper, method_name, escape(arg1), escape(arg2), escape(arg3), escape(arg4), escape(arg5), escape(arg6), escape(arg7));
    }

    public IRubyObject escape(IRubyObject arg) {
        return arg == null ? context.nil : arg;
    }

    IRubyObject new_nil_at() {
        return context.nil;
    }

    private IRubyObject param_error(String message, IRubyObject lhs) {
        error();
        return dispatch("on_param_error", lhs);
    }
    
    public IRubyObject formal_argument(ByteList identifier, IRubyObject lhs) {
        RubyParserBase.IDType type = RubyParserBase.id_type(identifier);
        switch(type) {
            case Constant: return 	param_error("formal argument cannot be a constant", lhs);
            case Instance: return 	param_error("formal argument cannot be an instance variable", lhs);
            case Global: return 	param_error("formal argument cannot be a global variable", lhs);
            case Class: return 	param_error("formal argument cannot be a class variable", lhs);
        }

        shadowing_lvar(identifier);

        return lhs;
    }
    
    protected void getterIdentifierError(ByteList identifier) {
        // FIXME: Should work with any m17n encoding.
        throw new SyntaxException("identifier " + identifier + " is not valid", getRuntime().newString(identifier).asJavaString());
    }
    
    public boolean id_is_var(ByteList value) {
        if (value == null) return false; // sigils without characters end up null (@, @@, $).

        RubyParserBase.IDType type = RubyParserBase.id_type(value);         // FIXME: Using Ruby Parser version...

        switch (type) {
            case Constant:
            case Global:
            case Instance:
            case Class:
                return true;
        }

        String id = getRuntime().newSymbol(value).idString();

        if (currentScope.isBlockScope() && isNumParamId(id)) return true;

        return getCurrentScope().isDefined(id) >= 0;
    }
    
    public IRubyObject intern(String value) {
        return asSymbol(context, value);
    }

    public IRubyObject intern(ByteList value) {
        return asSymbol(context, value);
    }

    protected IRubyObject new_defined(long _line, IRubyObject value) {
        return dispatch("on_defined", value);
    }

    public IRubyObject new_regexp(int line, IRubyObject contents, IRubyObject end) {
        return dispatch("on_regexp_literal", contents, end);
    }

    protected IRubyObject match_op(IRubyObject left, IRubyObject right) {
        return call_bin_op(left, EQUAL_TILDE, right, 0);
    }

    protected IRubyObject call_bin_op(IRubyObject left, IRubyObject id, IRubyObject right, int line) {
        if (id instanceof RubyString) {
            id = intern(((RubyString) id).getByteList());
        }

        return dispatch("on_binary", left, id, right);
    }

    protected IRubyObject call_bin_op(IRubyObject left, ByteList id, IRubyObject right, int line) {
        return dispatch("on_binary", left, intern(id), right);
    }

    protected IRubyObject call_uni_op(IRubyObject recv, IRubyObject id) {
        return dispatch("on_unary", id, recv);
    }

    protected IRubyObject call_uni_op(IRubyObject recv, ByteList id) {
        return call_uni_op(recv, intern(id));
    }

    protected IRubyObject logop(IRubyObject left, ByteList id, IRubyObject right) {
        return call_bin_op(left, intern(id), right, -1);
    }

    public Set<ByteList> push_pvtbl() {
        Set<ByteList> currentTable = variableTable;

        variableTable = new HashSet<>();

        return currentTable;
    }

    protected void pop_pvtbl(Set<ByteList> table) {
        variableTable = table;
    }

    protected Set<ByteList> push_pktbl() {
        Set<ByteList> currentTable = keyTable;

        keyTable = new HashSet<>();

        return currentTable;
    }

    protected void pop_pktbl(Set<ByteList> table) {
        keyTable = table;
    }

    public Node numparam_push() {
        Node inner = numParamInner;

        if (numParamOuter == null) numParamOuter = numParamCurrent;

        numParamInner = null;
        numParamCurrent = null;

        return inner;
    }

    public void numparam_pop(Node previousInner) {
        if (previousInner != null) {
            numParamInner = previousInner;
        } else if (numParamCurrent != null) {
            numParamInner = numParamCurrent;
        }

        if (maxNumParam > 0) {
            numParamCurrent = numParamOuter;
            numParamOuter = null;
        } else {
            numParamCurrent = null;
        }

    }

    public int resetMaxNumParam() {
        return restoreMaxNumParam(0);
    }

    public int restoreMaxNumParam(int maxNum) {
        int temp = maxNumParam;

        maxNumParam = maxNum;

        return temp;
    }

    public void ordinalMaxNumParam() {
        maxNumParam = -1;
    }

    protected int src_line() {
        return lexer.getRubySourceline();
    }

    protected IRubyObject value_expr(IRubyObject value) {
        return value;
    }

    private String assignableCheckValid(RubySymbol name) {
        String id = name.idString();

        switch(id) {
            case "self": return "Can't change the value of self";
            case "nil": return "Can't assign to nil";
            case "true": return "Can't assign to true";
            case "false": return "Can't assign to false";
            case "__FILE__": return "Can't assign to __FILE__";
            case "__LINE__": return "Can't assign to __LINE__";
            case "__ENCODING__": return "Can't assign to __ENCODING__";
        }

        RubyParserBase.IDType type = RubyParserBase.id_type(name.getBytes());
        switch(type) {
            case Local:
                if (currentScope.isBlockScope()) {
                    if (maxNumParam > 0 && isNumParamId(id)) {
                        return "Can't assign to numbered parameter " + id;
                    }
                }
                break;
            case Constant:
                if (isInDef()) return "dynamic constant assignment";
                break;
        }

        return null;
    }

    protected IRubyObject assignable(ByteList name, IRubyObject value) {
        RubySymbol symbol = getRuntime().newSymbol(name);
        String error = assignableCheckValid(symbol);

        if (error != null) return assign_error(error, value);

        currentScope.addVariableThisScope(symbol.idString());

        return value;
    }

    protected IRubyObject backref_error(IRubyObject ref, IRubyObject expr) {
        var runtime = ref.getRuntime();
        RubyString str = runtime.newString("Can't set variable ");
        str = str.append(ref);

        return dispatch("on_assign_error",  str, expr);
    }

    protected int getParenNest() {
        return lexer.getParenNest();
    }

    public void add_forwarding_args() {
        arg_var(FWD_REST);
        arg_var(FWD_KWREST);
        arg_var(FWD_BLOCK);
    }

    public IRubyObject method_optarg(IRubyObject method, IRubyObject arg) {
        return arg == null ? method : dispatch("on_method_add_arg", method, arg);
    }

    public IRubyObject keyword_arg(IRubyObject key, IRubyObject value) {
        return newArray(context, key, value != null ? value : context.nil);
    }

    public IRubyObject new_args(int _line, IRubyObject f, IRubyObject o, IRubyObject r, IRubyObject p, ArgsTailHolder tail) {
        return tail != null ?
                dispatch("on_params", f, o, r, p, tail.getKeywordArgs(), tail.getKeywordRestArg(), tail.getBlockArg()) :
                dispatch("on_params", f, o, r, p, null, null, null);
    }

    public ArgsTailHolder new_args_tail(int line, IRubyObject kwarg, IRubyObject kwargRest, ByteList block) {
        return new_args_tail(line, kwarg, kwargRest, symbolID(block));
    }

    public ArgsTailHolder new_args_tail(int _line, IRubyObject kwarg, IRubyObject kwargRest, IRubyObject block) {
        return new ArgsTailHolder(kwarg, kwargRest, block);
    }

    public ArgsTailHolder new_args_tail(int _line, IRubyObject kwarg, ByteList kwargRest, IRubyObject block) {
        RubySymbol keywordRestArg = kwargRest != null ? symbolID(kwargRest) : null;

        return new ArgsTailHolder(kwarg, keywordRestArg, block);
    }

    protected ArgsTailHolder new_args_tail(int line, IRubyObject keywordArg,
                                           ByteList keywordRestArgName, ByteList block) {
        RubySymbol blockArg = block != null ? symbolID(block) : null;

        return new_args_tail(line, keywordArg, keywordRestArgName, blockArg);
    }

        public IRubyObject method_add_block(IRubyObject method, IRubyObject block) {
        return dispatch("on_method_add_block", method, block);
    }

    public Encoding getEncoding() {
        return lexer.getEncoding();
    }

    public IRubyObject createStr(ByteList data, int flags) {
        return lexer.createStr(data, flags);
    }

    public IRubyObject internalId() {
        return null;
    }
    
    public IRubyObject new_array(IRubyObject ...args) {
        return newArray(context, args);
    }

    public IRubyObject new_assoc(IRubyObject key, IRubyObject value) {
        return newArray(context, key, value);
    }    
    
    public IRubyObject new_bv(ByteList identifier) {
        if (!is_local_id(identifier)) getterIdentifierError(identifier);

        shadowing_lvar(identifier);
        return arg_var(identifier);
    }

    public void popCurrentScope() {
        if (!currentScope.isBlockScope()) { // blocks are soft scopes. All others are roots of lvars we are leaving.
            lexer.getCmdArgumentState().pop();
            lexer.getConditionState().pop();
        }

        //scopedParserState.warnUnusedVariables(getRuntime(), warnings, currentScope.getFile());
        currentScope = currentScope.getEnclosingScope();
        scopedParserState = scopedParserState.getEnclosingScope();
    }

    public void pushBlockScope() {
        currentScope = getRuntime().getStaticScopeFactory().newBlockScope(currentScope, lexer.getFile());
        scopedParserState = new ScopedParserState(scopedParserState);
    }

    public void pushLocalScope() {
        currentScope = getRuntime().getStaticScopeFactory().newLocalScope(currentScope, lexer.getFile());
        scopedParserState = new ScopedParserState(scopedParserState, lexer.getCmdArgumentState().getStack(), lexer.getConditionState().getStack());
        lexer.getCmdArgumentState().push0();
        lexer.getConditionState().push0();
    }

    public int getHeredocIndent() {
        return lexer.getHeredocIndent();
    }

    public void setHeredocIndent(int indent) {
        lexer.setHeredocIndent(indent);
    }

    public IRubyObject heredoc_dedent(IRubyObject array) {
        int indent = lexer.getHeredocIndent();
        if (indent <= 0) return array;

        lexer.setHeredocIndent(0);
        dispatch("on_heredoc_dedent", array, RubyFixnum.newFixnum(getRuntime(), indent));

        return array;
    }
    
    public void setCommandStart(boolean value) {
        lexer.commandStart = value;
    }

    // 1.9
    public ByteList shadowing_lvar(ByteList nameBytes) {
        if (is_private_local_id(nameBytes)) return nameBytes;

        RubySymbol name = symbolID(nameBytes);
        String id = name.idString();

        StaticScope current = getCurrentScope();
        if (current.exists(id) >= 0) yyerror("duplicated argument name");

        int slot = current.isDefined(id);
        if (slot != -1) {
            scopedParserState.addDefinedVariable(name, lexer.getRubySourceline());
            scopedParserState.markUsedVariable(name, slot >> 16);
        }

        return nameBytes;
    }

    public StackState getConditionState() {
        return lexer.getConditionState();
    }

    public boolean isInDef() {
        return lexer.getLexContext().in_def;
    }

    public boolean isInClass() {
        return inClass;
    }

    public void setIsInClass(boolean inClass) {
        this.inClass = inClass;
    }

    public StrTerm getStrTerm() {
        return lexer.getStrTerm();
    }

    public void setStrTerm(StrTerm object) {
        lexer.setStrTerm(object);
    }

    public StackState getCmdArgumentState() {
        return lexer.getCmdArgumentState();
    }
    
    public IRubyObject compile_error(String message) {
        return dispatch("on_parse_error", getRuntime().newString(message));
    }

    public void yyerror(String message) {
        compile_error(message);
        error();
        throw new SyntaxException(message, message);
    }
    
    public void yyerror(String message, String[] expected, String found) {
        error();
        compile_error(message + ", unexpected " + found);
    }

    public void error() {
        this.isError = true;
    }

    protected LexContext getLexContext() {
        return lexer.getLexContext();
    }

    public Integer getLeftParenBegin() {
        return lexer.getLeftParenBegin();
    }

    public void setLeftParenBegin(Integer integer) {
        lexer.setLeftParenBegin(integer);
    }

    public void setInDef(boolean inDefinition) {
        this.inDefinition = inDefinition;
    }

    public void setInSingle(int inSingleton) {
        this.inSingleton = inSingleton;
    }

    public int getInSingle() {
        return inSingleton;
    }

    public int getBraceNest() {
        return lexer.getBraceNest();
    }

    public int getState() {
        return lexer.getState();
    }

    public void setBraceNest(int braceNest) {
        lexer.setBraceNest(braceNest);
    }

    public void setState(int lexState) {
        lexer.setState(lexState);
    }

    public void warning(int _line, String message) {
        warning(message);
    }

    public void warning(String message) {
        if (lexer.isVerbose()) lexer.warning(message);
    }

    public void warn(int _line, String message) {
        lexer.warn(message);
    }
    public void warn(String message) {
        lexer.warn(message);
    }

    protected IRubyObject get_value(IRubyObject value) {
        return value;
    }

    protected IRubyObject ripper_new_yylval(IRubyObject id, IRubyObject b, IRubyObject c) {
        return b;
    }

    protected IRubyObject get_value(DefHolder holder) {
        return holder.value;
    }

    protected Set get_value(Set set) {
        return set;
    }

    protected IRubyObject get_value(RubyArray holder) {
        return holder.eltOk(0);
    }

    public void endless_method_name(DefHolder holder, ProductionState loc) {
        ByteList name = holder.name.getBytes();
        RubyParserBase.IDType type = RubyParserBase.id_type(name);

        if (type == RubyParserBase.IDType.AttrSet) {
            compile_error("setter method cannot be defined in an endless method definition");
        }
    }

    public void restore_defun(DefHolder holder) {
        lexer.getLexContext().restore(holder);
        lexer.setCurrentArg(holder.current_arg);
    }

    public RubySymbol symbolID(ByteList identifierValue) {
        // FIXME: We walk this during identifier construction so we should calculate CR without having to walk twice.
        if (RubyString.scanForCodeRange(identifierValue) == StringSupport.CR_BROKEN) {
            Ruby runtime = getRuntime();
            throw runtime.newEncodingError(str(runtime, "invalid symbol in encoding " + lexer.getEncoding() + " :\"", inspectIdentifierByteList(runtime, identifierValue), "\""));
        }

        return RubySymbol.newIDSymbol(getRuntime(), identifierValue);
    }

    public RubySymbol symbolID(IRubyObject ident) {
        return (RubySymbol) ident;
    }

    public void numparam_name(ByteList name) {
        String id = getRuntime().newSymbol(name).idString();
        if (isNumParamId(id)) compile_error(id + " is reserved for numbered parameter");
    }

    private boolean isNumParamId(String id) {
        if (id.length() != 2 || id.charAt(0) != '_') return false;

        char one = id.charAt(1);
        return one != '0' && Character.isDigit(one); // _1..._9
    }

    public IRubyObject new_array_pattern(int _line, IRubyObject constant, IRubyObject preArg, RubyArray arrayPattern) {
        var preArgs = (RubyArray<?>) arrayPattern.eltOk(0);
        IRubyObject restArg = arrayPattern.eltOk(1);
        IRubyObject postArgs = arrayPattern.eltOk(2);

        if (preArg != null) {
            if (preArgs != null) {
                preArgs.unshift(context, preArg);
            } else {
                preArgs = newArray(context, preArg);
            }
        }

        return dispatch("on_aryptn", constant, preArgs, restArg, postArgs);
    }

    public RubyArray new_array_pattern_tail(int _line, IRubyObject preArgs, boolean hasRest, IRubyObject restArg, RubyArray postArgs) {
        if (hasRest) {
            restArg = dispatch("on_var_field", restArg != null ? restArg : getRuntime().getNil());
        } else {
            restArg = getRuntime().getNil();
        }

        return RubyArray.newArray(getRuntime(), preArgs, restArg, postArgs);
    }


    public IRubyObject new_find_pattern(IRubyObject constant, RubyArray findPattern) {
        IRubyObject preArgs = findPattern.eltOk(0);
        IRubyObject restArg = findPattern.eltOk(1);
        IRubyObject postArgs = findPattern.eltOk(2);

        return dispatch("on_fndptn", constant, preArgs, restArg, postArgs);
    }

    public RubyArray new_find_pattern_tail(int _line, IRubyObject preRestArg, IRubyObject args, IRubyObject postRestArg) {
        preRestArg = dispatch("on_var_field", preRestArg != null ? preRestArg : context.nil);
        postRestArg = dispatch("on_var_field", postRestArg != null ? postRestArg : context.nil);

        return RubyArray.newArray(getRuntime(), preRestArg, args, postRestArg);
    }

    protected IRubyObject const_decl(IRubyObject path) {
        if (getLexContext().in_def) {
            path = assign_error("dynamic constant assignment", path);
        }

        return path;
    }

    private IRubyObject assign_error(String message, IRubyObject value) {
        value = dispatch("on_assign_error", getRuntime().newString(message), value);
        error();
        return value;
    }

    public IRubyObject new_hash_pattern(IRubyObject constant, RubyArray hashPattern) {
        IRubyObject keywordArgs = hashPattern.eltOk(0);
        IRubyObject keywordRestArgs = hashPattern.eltOk(1);

        return dispatch("on_hshptn", constant, keywordArgs, keywordRestArgs);
    }

    public IRubyObject none() {
        return nil();
    }

    public RubyArray new_hash_pattern_tail(int _line, IRubyObject keywordArgs, IRubyObject keywordRestValue) {
        // To not make parser construct an array we will just detect the case of '**' with no arguments before it.
        if (keywordArgs == null) keywordArgs = newEmptyArray(context);

        IRubyObject restArg = keywordRestValue != null ?
                dispatch("on_var_field", keywordRestValue) : context.nil;

        return newArray(context, keywordArgs, restArg);
    }

    public IRubyObject makeNullNil(IRubyObject value) {
        return value == null ? context.nil : value;
    }

    public boolean local_id(ByteList value) {
        // FIXME: local_id_ref is more complicated and we just blanket look for a scope var of the same name.
        return currentScope.isDefined(symbolID(value).idString()) >= 0;
    }

    public boolean check_forwarding_args() {
        if (local_id(FWD_REST) &&
                local_id(FWD_KWREST) &&
                local_id(FWD_BLOCK)) return true;

        compile_error("unexpected ...");
        return false;
    }

    public IRubyObject method_cond(IRubyObject value) {
        return value;
    }

    public void error_duplicate_pattern_key(ByteList key) {
        // This is for bare one-line matches ({a: 1} => a:).
        if (keyTable == null) keyTable = new HashSet<>();
        if (keyTable.contains(key)) yyerror("duplicated key name");

        keyTable.add(key);
    }

    protected void error_duplicate_pattern_variable(ByteList variable) {
        if (is_private_local_id(variable)) return;
        if (variableTable.contains(variable)) yyerror("duplicated variable name");

        variableTable.add(variable);
    }

    public boolean is_private_local_id(ByteList name) {
        if (name.realSize() == 1 && name.charAt(0) == '_') return true;
        if (!is_local_id(name)) return false;

        return name.charAt(0) == '_';
    }

    // ENEBO: Totally weird naming (in MRI is not allocated and is a local var name) [1.9]
    public boolean is_local_id(ByteList name) {
        // FIXME: Using Ruby Parser version...
        RubyParserBase.IDType type = RubyParserBase.id_type(name);

        byte last = (byte) name.get(name.length() - 1);
        // FIXME: MRI version of Local must handle this but I don't see where..so I am adding manual check of last char here
        return type == RubyParserBase.IDType.Local && last != '?' && last != '=' && last != '!';
    }

    protected ByteList extractByteList(Object value) {
        if (value instanceof ByteList) return (ByteList) value;
        if (value instanceof RubyString) return ((RubyString) value).getByteList();
        if (value instanceof RubySymbol) return ((RubySymbol) value).getBytes();
        if (value instanceof RubyArray) return ((RubyString) ((RubyArray) value).eltOk(1)).getByteList();

        throw new RuntimeException("Got unexpected object: " + value);
    }

    public Integer incrementParenNest() {
        return lexer.incrementParenNest();
    }

    
    public StaticScope getCurrentScope() {
        return currentScope;
    }

    public Ruby getRuntime() {
        return context.runtime;
    }
    
    public long getColumn() {
        return lexer.column();
    }

    public long getLineno() {
        return lexer.lineno();
    }
    
    public boolean hasStarted() {
        return lexer.hasStarted();
    }
    
    public Encoding encoding() {
        return lexer.getEncoding();
    }

    public ByteList getCurrentArg() {
        return currentArg;
    }

    public void setCurrentArg(IRubyObject arg) {
        this.currentArg = arg != null ? extractByteList(arg) : null;
    }
    
    public boolean getYYDebug() {
        return yydebug;
    }
    
    public void setYYDebug(boolean yydebug) {
        this.yydebug = yydebug;
    }
    
    public boolean isEndSeen() {
        return lexer.isEndSeen();
    }

    public boolean isError() {
        return isError;
    }

    public IRubyObject nil() {
        return context.nil;
    }

    public IRubyObject fals() {
        return context.fals;
    }

    public IRubyObject var_field(IRubyObject value) {
        return dispatch("on_var_field", value);
    }

    public IRubyObject remove_begin(IRubyObject value) {
        return value;
    }

    public IRubyObject void_stmts(IRubyObject value) {
        return value;
    }

    protected void setHeredocLineIndent(int value) {
        lexer.setHeredocLineIndent(value);
    }

    public void warn_experimental(int line, String message) {
        lexer.warning(lexer.getFile(), message);
    }

    public ThreadContext getContext() {
        return context;
    }

    public RubySymbol get_id(ByteList value) {
        return getRuntime().newSymbol(value);
    }

    public RubySymbol get_id(IRubyObject _ignored) {
        if (_ignored instanceof RubySymbol) {
            return (RubySymbol) _ignored;
        }

        return getRuntime().newSymbol(lexer.identValue);
    }

    protected NodeExits allow_block_exit() {
        // FIXME: Impl
        return null;
    }

    protected void clear_block_exit(boolean value) {

    }

    protected void next_rescue_context(LexContext context, LexContext.InRescue value) {

    }

    protected NodeExits init_block_exit() {
        return new NodeExits();
    }

    protected void restore_block_exit(NodeExits nodeExits) {
        // FIXME:Impl
    }

    protected void begin_definition(String name) {
        LexContext ctxt = getLexContext();
        ctxt.in_class = name != null;
        if (!ctxt.in_class) {
            ctxt.in_def = false;
        } else if (ctxt.in_def) {
            yyerror((name != null ? (name + " ") : "") + "definition in method body");
        }
        pushLocalScope();
    }

    protected void push_end_expect_token_locations(int line) {
        // FIXME: impl
    }

    protected void token_info_setup(String value, ProductionState loc) {

    }

    protected void token_info_push(String value, ProductionState loc) {

    }

    protected void token_info_pop(String value, ProductionState loc) {
        //  FIXME: impl
    }
    protected void token_info_drop(String value, ProductionState loc) {
        //  FIXME: impl
    }

    protected void token_info_warn(String name, int same, ProductionState loc) {

    }

    protected boolean isEval() {
        return false;
    }

    protected void WARN_EOL(String name) {
        // FIXME: IMpl
    }

    public enum IDType {
        Local, Global, Instance, AttrSet, Constant, Class;
    }

    public static RubyParserBase.IDType id_type(ByteList identifier) {
        char first = identifier.charAt(0);

        if (Character.isUpperCase(first)) return Constant;

        switch(first) {
            case '@':
                return identifier.charAt(1) == '@' ? Class : Instance;
            case '$':
                return Global;
        }

        byte last = (byte) identifier.get(identifier.length() - 1);
        if (last == '=') {
            return AttrSet;
        }

        return Local;
    }

    protected IRubyObject it_id() {
        return itId;
    }

    protected void set_it_id(IRubyObject node) {
        this.itId = node;
    }

    private IRubyObject itId;

    protected void forwarding_arg_check(ByteList rest, ByteList all, String var) {
        // FIXME: Impl
    }

    protected void setLexContext(LexContext lexContext) {
        lexer.setLexContext(lexContext);
    }

    protected IRubyObject NEW_ERROR(ProductionState value) {
        return context.nil;
    }

    protected boolean dyna_in_block() {
        return currentScope.isBlockScope() && !isEval();
    }

    protected IRubyObject ripper;
    protected ThreadContext context;
    protected RubyLexer lexer;
    protected StaticScope currentScope;
    protected boolean inDefinition;
    protected boolean inClass;
    protected boolean yydebug; // FIXME: Hook up to yydebug
    protected boolean isError;
    
    // Is the parser current within a singleton (value is number of nested singletons)
    protected int inSingleton;
    private ByteList currentArg;
    protected ScopedParserState scopedParserState;
    private Set<ByteList> keyTable;
    private Set<ByteList> variableTable;

    private int maxNumParam = 0;
    private Node numParamCurrent = null;
    private Node numParamInner = null;
    private Node numParamOuter = null;
    public IRubyObject case_labels;

    public static final ByteList NOT = new ByteList(new byte[] {'n', 'o', 't'}, USASCIIEncoding.INSTANCE);
}
