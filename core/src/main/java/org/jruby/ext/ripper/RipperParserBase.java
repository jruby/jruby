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
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.lexer.LexerSource;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.StackState;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
public class RipperParserBase {
    private IRubyObject currentArg;

    public RipperParserBase(ThreadContext context, IRubyObject ripper, LexerSource source) {
        this.context = context;
        this.ripper = ripper;
        this.lexer = new RipperLexer(this, source);
    }
    
    public void reset() {
//        inSingleton = 0;
     //   inDefinition = false;
    }  
    
    public Object yyparse (RipperLexer yyLex) throws java.io.IOException {
        return null;
    }
    
    public Object yyparse (RipperLexer yyLex, Object debugger) throws java.io.IOException {
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

    public IRubyObject arg_var(IRubyObject identifier) {
        String name = lexer.getIdent();
        StaticScope current = getCurrentScope();

        // Multiple _ arguments are allowed.  To not screw with tons of arity
        // issues in our runtime we will allocate unnamed bogus vars so things
        // still work. MRI does not use name as intern'd value so they don't
        // have this issue.
        if (name == "_") {
            int count = 0;
            while (current.exists(name) >= 0) {
                name = "_$" + count++;
            }
        }
        
        current.addVariableThisScope(name);
                
        return identifier;
    }

    public IRubyObject assignableConstant(IRubyObject value) {
        if (isInDef()) {
            value = dispatch("on_assign_error", value);
            error();
        }
        return value;
    }

    public IRubyObject assignableIdentifier(IRubyObject value) {
        String ident = lexer.getIdent().intern();
        getCurrentScope().assign(lexer.getRubySourceline(), context.runtime.newSymbol(lexer.getIdent()), null);
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
    
    public IRubyObject formal_argument(IRubyObject identifier) {
        return shadowing_lvar(identifier);
    }
    
    protected void getterIdentifierError(String identifier) {
        throw new SyntaxException("identifier " + identifier + " is not valid", identifier);
    }    
    
    public boolean is_id_var() {
        String ident = lexer.getIdent().intern();

        return getCurrentScope().isDefined(ident) >= 0;
    }
    
    public boolean is_local_id(String identifier) {
        return lexer.isIdentifierChar(identifier.charAt(0));
    }    
    
    public IRubyObject intern(String value) {
        return context.runtime.newSymbol(value);
    }
    
    public IRubyObject method_optarg(IRubyObject method, IRubyObject arg) {
        if (arg == null) return method;

        return dispatch("on_method_add_arg", method, arg);
    }

    public IRubyObject keyword_arg(IRubyObject key, IRubyObject value) {
        RubyArray array = RubyArray.newArray(context.runtime, 2);

        array.append(key);
        if (value != null) {
            array.append(value);
        } else {
            array.append(context.nil);
        }

        return array;
    }

    public IRubyObject new_args(IRubyObject f, IRubyObject o, IRubyObject r, IRubyObject p, ArgsTailHolder tail) {
        if (tail != null) {
            return dispatch("on_params", f, o, r, p, tail.getKeywordArgs(), tail.getKeywordRestArg(), tail.getBlockArg());
        }

        return dispatch("on_params", f, o, r, p, null, null, null);
    }

    public ArgsTailHolder new_args_tail(IRubyObject kwarg, IRubyObject kwargRest, IRubyObject block) {
        return new ArgsTailHolder(kwarg, kwargRest, block);
    }

    public IRubyObject method_add_block(IRubyObject method, IRubyObject block) {
        return dispatch("on_method_add_block", method, block);
    }

    public IRubyObject internalId() {
        return null;
    }
    
    public IRubyObject new_array(IRubyObject arg) {
        return context.runtime.newArray(arg);
    }
    
    public IRubyObject new_assoc(IRubyObject key, IRubyObject value) {
        return RubyArray.newArray(context.runtime, key, value);
    }    
    
    public IRubyObject new_bv(IRubyObject identifier) {
        String ident = lexer.getIdent();
        
        if (!is_local_id(ident)) getterIdentifierError(ident);

        return arg_var(shadowing_lvar(identifier));
    }
    
    public void popCurrentScope() {
        if (!currentScope.isBlockScope()) {
            getCmdArgumentState().pop();
            getConditionState().pop();
        }
        currentScope = currentScope.getEnclosingScope();
    }
    
    public void pushBlockScope() {
        currentScope = getRuntime().getStaticScopeFactory().newBlockScope(currentScope);
    }
    
    public void pushLocalScope() {
        currentScope = getRuntime().getStaticScopeFactory().newLocalScope(currentScope);
        getCmdArgumentState().push0();
        getConditionState().push0();
    }

    public int getHeredocIndent() {
        return lexer.getHeredocIndent();
    }

    public void setHeredocIndent(int indent) {
        lexer.setHeredocIndent(indent);
    }

    public void heredoc_dedent(IRubyObject array) {
        if (lexer.getHeredocIndent() <= 0) return;

        dispatch("on_heredoc_dedent", array, getRuntime().newFixnum(lexer.getHeredocIndent()));
    }
    
    public void setCommandStart(boolean value) {
        lexer.commandStart = value;
    }
    
    public IRubyObject shadowing_lvar(IRubyObject identifier) {
       String name = lexer.getIdent();

        if (name == "_") return identifier;

        StaticScope current = getCurrentScope();
        if (current.isBlockScope()) {
            if (current.exists(name) >= 0) yyerror("duplicated argument name");

            if (current.isDefined(name) >= 0) {
                lexer.warning("shadowing outer local variable - %s", name);
            }
        } else if (current.exists(name) >= 0) {
            yyerror("duplicated argument name");
        }

        return identifier;
    }    

    public StackState getConditionState() {
        return lexer.getConditionState();
    }

    public boolean isInDef() {
        return inDefinition;
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
    
    public void compile_error(String message) {
        dispatch("on_parse_error", getRuntime().newString(message));
    }

    public void yyerror(String message) {
        compile_error(message);
        error();
        throw new SyntaxException(message, message);
    }
    
    public void yyerror(String message, String[] expected, String found) {
        error();
        compile_error(message + ", unexpected " + found + "\n");
    }

    public void error() {
        this.isError = true;
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

    public void warning(String message) {
        if (lexer.isVerbose()) lexer.warning(message);
    }

    public void warn(String message) {
        lexer.warn(message);
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

    public IRubyObject getCurrentArg() {
        return currentArg;
    }

    public void setCurrentArg(IRubyObject arg) {
        this.currentArg = arg;
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

    public ThreadContext getContext() {
        return context;
    }
    
    protected IRubyObject ripper;
    protected ThreadContext context;
    protected RipperLexer lexer;
    protected StaticScope currentScope;
    protected boolean inDefinition;
    protected boolean inClass;
    protected boolean yydebug; // FIXME: Hook up to yydebug
    protected boolean isError;
    
    // Is the parser current within a singleton (value is number of nested singletons)
    protected int inSingleton;
}
