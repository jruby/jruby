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
import org.jruby.runtime.builtin.IRubyObject;
import org.ablaf.common.ISourcePosition;

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
        ISourcePosition oldPosition = ruby.getPosition();
        ruby.setPosition(file, line);

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

        IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(ruby, args.toArray());

        Object result = JavaUtil.convertRubyToJava(ruby, ruby.getTopSelf().callMethod("__jruby_bsf_anonymous", rubyArgs));

        ruby.setPosition(oldPosition);

        return result;
    }

    public Object eval(String file, int line, int col, Object expr) throws BSFException {
        ISourcePosition oldPosition = ruby.getPosition();
        ruby.setPosition(file, line);

        try {
            Object result = ruby.evalScript((String) expr, Object.class);
            ruby.setPosition(oldPosition);
            return result;
        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public void exec(String file, int line, int col, Object expr) throws BSFException {
        try {
            ISourcePosition oldPosition = ruby.getPosition();
            ruby.setPosition(file, line);

            ruby.evalScript((String) expr, Object.class);

            ruby.setPosition(oldPosition);

        } catch (Exception excptn) {
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, "Exception", excptn);
        }
    }

    public Object call(Object recv, String method, Object[] args) throws BSFException {
        try {
            IRubyObject rubyRecv = recv != null ? JavaUtil.convertJavaToRuby(ruby, recv) : ruby.getTopSelf();

            IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(ruby, args);

            IRubyObject result = rubyRecv.callMethod(method, rubyArgs);

            return JavaUtil.convertRubyToJava(ruby, result, Object.class);
        } catch (Exception excptn) {
            printException(ruby, excptn);
            throw new BSFException(BSFException.REASON_EXECUTION_ERROR, excptn.getMessage(), excptn);
        }
    }

    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        
        ruby = Ruby.getDefaultInstance();

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
            ruby.getRuntime().printError(((RaiseException) exception).getException());
        } else if (exception instanceof ThrowJump) {
            ruby.getRuntime().printError(((ThrowJump) exception).getNameError());
        } else if (exception instanceof BreakJump) {
            ruby.getRuntime().getErrorStream().println("break without block.");
        } else if (exception instanceof ReturnJump) {
            ruby.getRuntime().getErrorStream().println("return without block.");
        }
    }


    private static class BeanGlobalVariable extends GlobalVariable {
        private BSFDeclaredBean bean;

        public BeanGlobalVariable(Ruby ruby, BSFDeclaredBean bean) {
            super(ruby, bean.name, null);

            this.bean = bean;
        }

        public IRubyObject get() {
            return JavaUtil.convertJavaToRuby(ruby, bean.bean);
        }

        public IRubyObject set(IRubyObject value) {
            bean.bean = JavaUtil.convertRubyToJava(ruby, value, bean.type);
            return value;
        }
    }
}
