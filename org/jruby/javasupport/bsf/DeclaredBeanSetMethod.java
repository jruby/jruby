/*
 * DeclaredBeanGetMethod.java
 *
 * Created on 8. November 2001, 18:17
 */

package org.jruby.javasupport.bsf;

import org.jruby.*;
import org.jruby.core.*;
import org.jruby.javasupport.*;

import com.ibm.bsf.*;

/**
 *
 * @author  jpetersen
 */
public class DeclaredBeanSetMethod implements RubyCallbackMethod {
    private BSFDeclaredBean bean;

    public DeclaredBeanSetMethod(BSFDeclaredBean bean) {
        this.bean = bean;
    }
    
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        if (args.length == 1) {
            // ruby.raise(new RubyException(), RubyString.m_newString(ruby, "Wrong ar"));
        
            bean.bean = JavaUtil.convertRubyToJava(ruby, args[0], bean.type);
        
            return args[0];
        } else {
            return ruby.getNil();
        }        
    }
}