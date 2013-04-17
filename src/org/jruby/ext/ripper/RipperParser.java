/*
 ***** BEGIN LICENSE BLOCK *****
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

import java.io.IOException;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.ext.ripper.RipperLexer.LexState;
import org.jruby.ext.ripper.Warnings.ID;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.StackState;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
public class RipperParser {
    public RipperParser(ThreadContext context, IRubyObject ripper, LexerSource source) {
        this.context = context;
        this.ripper = ripper;
        this.lexer = new RipperLexer(this, source);
    }
    
    static int associateEncoding(ByteList buffer, Encoding ASCII8BIT_ENCODING, int codeRange) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        
        //setEncoding(configuration.getDefaultEncoding());

        Object debugger = null;
        if (false) {
            try {
                Class yyDebugAdapterClass = Class.forName("jay.yydebug.yyDebugAdapter");
                debugger = yyDebugAdapterClass.newInstance();
            } catch (IllegalAccessException iae) {
                // ignore, no debugger present
            } catch (InstantiationException ie) {
                // ignore, no debugger present
            } catch (ClassNotFoundException cnfe) {
                // ignore, no debugger present
            }
        }
        //yyparse(lexer, new jay.yydebug.yyAnim("JRuby", 9));
        
        return (IRubyObject) yyparse(lexer, debugger);
    }    
    
    public IRubyObject arg_add_optblock(IRubyObject arg1, IRubyObject arg2) {
        if (arg2 == null) return arg1;
        
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
    
    public IRubyObject assignable(IRubyObject name) {
        // FIXME: Some grammar-level arg checking is here to prevent __ENCODING__ = 1 + other checks.
        // We lose the plot here since we are not passing tokens around anymore.  We might be able to
        // store arg type or in cases of __ENCODING__ just see if it is called __ENCODING__.
        return name;
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
    
    public IRubyObject escape(IRubyObject arg) {
        return arg == null ? context.runtime.getNil() : arg;
    }
    
    public IRubyObject formal_argument(IRubyObject identifier) {
        return shadowing_lvar(identifier);
    }
    
    protected void getterIdentifierError(Position position, String identifier) {
        throw new SyntaxException(SyntaxException.PID.BAD_IDENTIFIER, position, lexer.getCurrentLine(),
                "identifier " + identifier + " is not valid", identifier);
    }    
    
    // FIXME: Consider removing identifier.
    public boolean is_id_var(IRubyObject identifier) {
        String ident = lexer.getIdent();
        ident.intern();
        char c = ident.charAt(0);
        
        if (c == '$' || c == '@' || Character.toUpperCase(c) == c) return false;

        return getCurrentScope().getLocalScope().isDefined(ident) >= 0;
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
    
    public IRubyObject new_array(IRubyObject arg) {
        return context.runtime.newArray(arg);
    }
    
    public IRubyObject new_assoc(IRubyObject key, IRubyObject value) {
        RubyHash hash = RubyHash.newHash(context.runtime);
        
        hash.fastASet(value, value);
        
        return hash;
    }    
    
    public IRubyObject new_bv(IRubyObject identifier) {
        String ident = lexer.getIdent();
        
        if (!is_local_id(ident)) getterIdentifierError(lexer.getPosition(), ident);

        return arg_var(shadowing_lvar(identifier));
    }
    
    public void popCurrentScope() {
        currentScope = currentScope.getEnclosingScope();
    }
    
    public void pushBlockScope() {
        currentScope = getRuntime().getStaticScopeFactory().newBlockScope(currentScope);
    }
    
    public void pushLocalScope() {
        currentScope = getRuntime().getStaticScopeFactory().newLocalScope(currentScope);
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
            
            if (lexer.isVerbose() && current.isDefined(name) >= 0) {
                lexer.warning(ID.STATEMENT_NOT_REACHED,lexer.getPosition(),
                        "shadowing outer local variable - " + name);
            }
        } else if (current.exists(name) >= 0) {
            yyerror("duplicated argument name");
        }

        return identifier;
    }    

    public StackState getConditionState() {
        return lexer.getConditionState();
    }
    
    public Position getPosition() {
        return lexer.getPosition();
    }

    public boolean isInDef() {
        return inDefinition;
    }

    public boolean isInSingle() {
        return inSingleton != 0;
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

    public void yyerror(String message) {
        throw new SyntaxException(SyntaxException.PID.GRAMMAR_ERROR, lexer.getPosition(), lexer.getCurrentLine(), message);
    }
    
    public void yyerror(String message, String[] expected, String found) {
        String text = message + ", unexpected " + found + "\n";
        
        dispatch("on_parse_error", getRuntime().newString(text));
        throw new SyntaxException(SyntaxException.PID.CHARACTER_BAD, lexer.getPosition(), found, message, expected);
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

    public void setState(LexState lexState) {
        lexer.setState(lexState);
    }

    public void warning(ID id, Position position, String message) {
        if (lexer.isVerbose()) lexer.warning(id, position, message);
    }

    public void warn(ID id, Position position, String message) {
        lexer.warn(id, position, message);
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
        return lexer.getEventLocation().getStartOffset();
    }

    public long getLineno() {
        return lexer.getEventLocation().getStartLine() + 1; //position is zero-based
    }
    
    protected IRubyObject ripper;
    protected ThreadContext context;
    protected RipperLexer lexer;
    protected StaticScope currentScope;
    protected boolean inDefinition;
    
    // Is the parser current within a singleton (value is number of nested singletons)
    protected int inSingleton;
}
