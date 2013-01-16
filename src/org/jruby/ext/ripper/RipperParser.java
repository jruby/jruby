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
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ext.ripper.RipperLexer.LexState;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.LexerSource;
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
    IRubyWarnings warnings;
    
    
    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;
//        lexer.setWarnings(warnings);
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
        if (isDebug) {
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
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }

    public IRubyObject arg_var(IRubyObject identifier) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject assignable(IRubyObject name, IRubyObject value) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject dispatch(String method_name) {
        return RuntimeHelpers.invoke(context, ripper, method_name);
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1) {
        return RuntimeHelpers.invoke(context, ripper, method_name, arg1);
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.invoke(context, ripper, method_name, arg1, arg2);
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return RuntimeHelpers.invoke(context, ripper, method_name, arg1, arg2, arg3);
    }
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        return RuntimeHelpers.invoke(context, ripper, method_name, arg1, arg2, arg3, arg4);
    }    
    
    public IRubyObject dispatch(String method_name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        return RuntimeHelpers.invoke(context, ripper, method_name, arg1, arg2, arg3, arg4, arg5);
    }
    
    public IRubyObject escape(IRubyObject arg) {
        return arg == null ? context.runtime.getNil() : arg;
    }
    
    public IRubyObject formal_argument(IRubyObject identifier) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public boolean is_id_var(IRubyObject identifier) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject intern(String value) {
        return context.runtime.newSymbol(value);
    }
    
    public IRubyObject method_optarg(IRubyObject arg1, IRubyObject arg2) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject new_array(IRubyObject elt1) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject new_assoc(IRubyObject arg1, IRubyObject arg2) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }    
    
    public IRubyObject new_bv(IRubyObject identifier) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
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
        
    }
    
    public void setRipperResult(IRubyObject top) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");
    }
    
    public IRubyObject shadowing_lvar(IRubyObject identifier) {
        throw new UnsupportedOperationException("Something seriously wrong to call ripper methods when not in ripper");        
    }    

    StackState getConditionState() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public ISourcePosition getPosition() {
        return null;
    }

    boolean isInDef() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    boolean isInSingle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Object getStrTerm() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setStrTerm(Object object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    StackState getCmdArgumentState() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void yyerror(String begiN_in_method) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    void yyerror(String begiN_in_method, String[] b, String c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Object getLeftParenBegin() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setLeftParenBegin(Integer integer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setInDef(boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setInSingle(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    int getInSingle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setInSingle(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setState(LexState lexState) {
        lexer.setState(lexState);
    }

    void warning(ID iD, ISourcePosition position, String _interpreted_as_grouped_expression) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void warn(ID iD, ISourcePosition position, String enD_in_method_use_at_exit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Integer incrementParenNest() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    StaticScope getCurrentScope() {
        return currentScope;
    }

    public Ruby getRuntime() {
        return context.runtime;
    }
    
    public long getColumn() {
        return 0; // FIXME: Implement
    }

    public long getLineno() {
        return lexer.getPosition().getLine() + 1; //position is zero-based
    }    
    
    protected IRubyObject ripper;
    protected ThreadContext context;
    protected RipperLexer lexer;
    protected StaticScope currentScope;
}
