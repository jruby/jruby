/*
 * JRubyEngine.java - No description
 * Created on 12. Oktober 2001, 00:11
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import java.util.*;

import org.jruby.*;
import org.jruby.javasupport.*;

import com.ibm.bsf.*;
import com.ibm.bsf.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JRubyEngine extends BSFEngineImpl {
    private Ruby ruby;
    private RubyObject topSelf;

    public Object apply(String source, int lineNo, int columnNo, Object funcBody,
                      Vector paramNames, Vector args) {

        StringBuffer sb = new StringBuffer(((String)funcBody).length() + 100);
        
        sb.append("def jruby_bsf_anonymous (");
        
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
        
        topSelf.eval(ruby.getRubyParser().compileJavaString(source, 
              (String)sb.toString(), ((String)sb.toString()).length(), lineNo));

        RubyObject[] rubyArgs = new RubyObject[args.size()];
        for (int i = args.size(); i >= 0; i--) {
            rubyArgs[i] = JavaUtil.convertJavaToRuby(ruby, args.elementAt(i), 
                                                  args.elementAt(i).getClass());
        }
        
        RubyObject result = topSelf.funcall("jruby_bsf_anonymous", rubyArgs);

        return JavaUtil.convertRubyToJava(ruby, result, Object.class);
    }
    
    public Object eval(String source, int lineNo, int columnNo, Object expr) throws BSFException {
        RubyObject result = topSelf.eval(ruby.getRubyParser().compileJavaString(
                        source, (String)expr, ((String)expr).length(), lineNo));

        return JavaUtil.convertRubyToJava(ruby, result, Object.class);
    }
    
    public void exec(String source, int lineNo, int columnNo, Object expr) throws BSFException {
        
        
        topSelf.eval(ruby.getRubyParser().compileJavaString(source, 
                            (String)expr, ((String)expr).length(), lineNo));
    }
    
    public Object call(Object recv, String method, Object[] args) throws BSFException {
        RubyObject rubyRecv = JavaUtil.convertJavaToRuby(ruby, recv, recv.getClass());

        RubyObject[] rubyArgs = new RubyObject[args.length];
        for (int i = args.length; i >= 0; i--) {
            rubyArgs[i] = JavaUtil.convertJavaToRuby(ruby, args[i], args[i].getClass());
        }

        RubyObject result = rubyRecv.funcall(method, rubyArgs);
        
        return JavaUtil.convertRubyToJava(ruby, result, Object.class);
    }
 
    public void initialize(BSFManager mgr, String lang, Vector declaredBeans)  throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        
        ruby = new Ruby();
        ruby.setRegexpAdapterClass(org.jruby.regexp.GNURegexpAdapter.class);
        ruby.init();
        
        topSelf = ruby.getRubyTopSelf();
        
        int size = declaredBeans.size();
        for (int i = 0; i < size; i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean)declaredBeans.elementAt(i);
            topSelf.getSingletonClass().defineMethod(bean.name, new DeclaredBeanGetMethod(bean));
            topSelf.getSingletonClass().defineMethod(bean.name + "=", new DeclaredBeanSetMethod(bean));
        }
    }
    
    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        topSelf.getSingletonClass().defineMethod(bean.name, new DeclaredBeanGetMethod(bean));
        topSelf.getSingletonClass().defineMethod(bean.name + "=", new DeclaredBeanSetMethod(bean));
    }
    
    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        topSelf.getSingletonClass().undefMethod(bean.name);
        topSelf.getSingletonClass().undefMethod(bean.name + "=");
    }
}
