/*
 * JRubyEngine.java - No description
 * Created on 11.01.2002, 14:33:37
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.javasupport.bsf;

import java.util.Vector;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.GlobalVariable;

import com.ibm.bsf.*;
import com.ibm.bsf.util.BSFEngineImpl;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JRubyEngine extends BSFEngineImpl {
    private Ruby ruby;

    public Object apply(String file, int line, int col, Object funcBody, Vector paramNames, Vector args) {
        String oldFile = ruby.getSourceFile();
        int oldLine = ruby.getSourceLine();

        ruby.setSourceFile(file);
        ruby.setSourceLine(line);

        StringBuffer sb = new StringBuffer(((String) funcBody).length() + 100);

        sb.append("def __jruby_bsf_anonymous (");

        int paramLength = paramNames.size();
        for (int i = 0; i < paramLength; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paramNames.elementAt(i));
        }
        sb.append(") \n");

        sb.append(funcBody);

        sb.append("\nend\n");

        ruby.evalScript((String) sb.toString(), null);

        RubyObject[] rubyArgs = new RubyObject[args.size()];
        for (int i = args.size(); i >= 0; i--) {
            rubyArgs[i] = JavaUtil.convertJavaToRuby(ruby, args.elementAt(i));
        }

        Object result = JavaUtil.convertRubyToJava(ruby, ruby.getRubyTopSelf().funcall("__jruby_bsf_anonymous", rubyArgs));

        ruby.setSourceFile(oldFile);
        ruby.setSourceLine(oldLine);

        return result;
    }

    public Object eval(String file, int line, int col, Object expr) throws BSFException {
        String oldFile = ruby.getSourceFile();
        int oldLine = ruby.getSourceLine();

        ruby.setSourceFile(file);
        ruby.setSourceLine(line);

        try {
            Object result = ruby.evalScript((String) expr, Object.class);

            ruby.setSourceFile(oldFile);
            ruby.setSourceLine(oldLine);

            return result;
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public void exec(String file, int line, int col, Object expr) throws BSFException {
        try {
            String oldFile = ruby.getSourceFile();
            int oldLine = ruby.getSourceLine();

            ruby.setSourceFile(file);
            ruby.setSourceLine(line);

            ruby.evalScript((String) expr, Object.class);

            ruby.setSourceFile(oldFile);
            ruby.setSourceLine(oldLine);
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public Object call(Object recv, String method, Object[] args) throws BSFException {
        try {
            RubyObject rubyRecv = recv != null ? JavaUtil.convertJavaToRuby(ruby, recv) : ruby.getRubyTopSelf();

            RubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(ruby, args);

            RubyObject result = rubyRecv.funcall(method, rubyArgs);

            return JavaUtil.convertRubyToJava(ruby, result, Object.class);
        } catch (Exception excptn) {
            printException(ruby, excptn);
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, excptn.getMessage(), excptn);
        }
    }

    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        
        ruby = Ruby.getDefaultInstance(org.jruby.runtime.regexp.GNURegexpAdapter.class);

        int size = declaredBeans.size();
        for (int i = 0; i < size; i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean) declaredBeans.elementAt(i);
            ruby.defineVariable(new BeanGlobalVariable(ruby, bean));
        }

        // ruby.defineGlobalFunction("declareBean", method);
    }

    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        ruby.defineVariable(new BeanGlobalVariable(ruby, bean));
    }

    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        ruby.undefineGlobalVar(bean.name);
    }

    public void handleException(BSFException bsfExcptn) {
        printException(ruby, (Exception)bsfExcptn.getTargetException());
    }

    /**
     *
     * Prints out an error message.
     *
     * @param exception An Exception thrown by JRuby
     */
    private static void printException(Ruby ruby, Exception exception) {
        if (exception instanceof RaiseException) {
            ruby.getRuntime().printError(((RaiseException) exception).getActException());
        } else if (exception instanceof ThrowJump) {
            ruby.getRuntime().printError(((ThrowJump) exception).getNameError());
        } else if (exception instanceof BreakJump) {
            ruby.getRuntime().getErrorStream().println("break without block.");
        } else if (exception instanceof ReturnException) {
            ruby.getRuntime().getErrorStream().println("return without block.");
        }
    }


    private static class BeanGlobalVariable extends GlobalVariable {
        private BSFDeclaredBean bean;

        public BeanGlobalVariable(Ruby ruby, BSFDeclaredBean bean) {
            super(ruby, bean.name, null);
            this.bean = bean;
        }

        public RubyObject get() {
            return JavaUtil.convertJavaToRuby(this.ruby, bean.bean);
        }

        public RubyObject set(RubyObject value) {
            bean.bean = JavaUtil.convertRubyToJava(this.ruby, value, bean.type);
            return value;
        }
    }
}
