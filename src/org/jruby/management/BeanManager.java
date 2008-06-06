package org.jruby.management;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jruby.Ruby;
import org.jruby.compiler.JITCompilerMBean;

public class BeanManager {
    private Ruby ruby;
    private boolean managementEnabled;
    
    public BeanManager(Ruby ruby, boolean managementEnabled) {
        this.ruby = ruby;
        this.managementEnabled = managementEnabled;
    }
    
    public void register(JITCompilerMBean jitCompiler) {
        if (managementEnabled) register("org.jruby.management:type=JITCompiler", jitCompiler);
    }
    
    public void register(ConfigMBean config) {
        if (managementEnabled) register("org.jruby.management:type=Config", config);
    }
    
    private void register(String name, Object bean) {
        try {
            System.out.println("here");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            ObjectName beanName = new ObjectName(name);
            mbs.registerMBean(bean, beanName);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(BeanManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MBeanRegistrationException ex) {
            Logger.getLogger(BeanManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotCompliantMBeanException ex) {
            Logger.getLogger(BeanManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(BeanManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            Logger.getLogger(BeanManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
