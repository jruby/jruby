/*
 * DeclaredBeanGetMethod.java
 *
 * Created on 8. November 2001, 18:17
 */

package org.jruby.javasupport.bsf;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.core.Callback;
import org.jruby.javasupport.JavaUtil;

import com.ibm.bsf.BSFDeclaredBean;

/**
 *
 * @author  jpetersen
 */
public class DeclaredBeanGetMethod implements Callback {
    private BSFDeclaredBean bean;

    public DeclaredBeanGetMethod(BSFDeclaredBean bean) {
        this.bean = bean;
    }

    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        return JavaUtil.convertJavaToRuby(ruby, bean.bean, bean.type);
    }
}