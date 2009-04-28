package org.jruby.management;

import java.lang.management.ManagementFactory;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jruby.Ruby;
import org.jruby.compiler.JITCompilerMBean;

public class BeanManagerImpl implements BeanManager {
    public final String base;
    
    private final boolean managementEnabled;
    
    public BeanManagerImpl(Ruby ruby, boolean managementEnabled) {
        this.managementEnabled = managementEnabled;
        this.base = "org.jruby:type=Runtime,name=" + ruby.hashCode() + ",";
    }
    
    public void register(JITCompilerMBean jitCompiler) {
        if (managementEnabled) register(base + "service=JITCompiler", jitCompiler);
    }
    
    public void register(ConfigMBean config) {
        if (managementEnabled) register(base + "service=Config", config);
    }
    
    public void register(ParserStatsMBean parserStats) {
        if (managementEnabled) register(base + "service=ParserStats", parserStats);
    }
    
    public void register(MethodCacheMBean methodCache) {
        if (managementEnabled) register(base + "service=MethodCache", methodCache);
    }
    
    public void register(ClassCacheMBean classCache) {
        if (managementEnabled) register(base + "service=ClassCache", classCache);
    }

    public void unregisterCompiler() {
        if (managementEnabled) unregister(base + "service=JITCompiler");
    }
    public void unregisterConfig() {
        if (managementEnabled) unregister(base + "service=Config");
    }
    public void unregisterParserStats() {
        if (managementEnabled) unregister(base + "service=ParserStats");
    }
    public void unregisterClassCache() {
        if (managementEnabled) unregister(base + "service=ClassCache");
    }
    public void unregisterMethodCache() {
        if (managementEnabled) unregister(base + "service=MethodCache");
    }

    private void register(String name, Object bean) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            ObjectName beanName = new ObjectName(name);
            mbs.registerMBean(bean, beanName);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.WARNING, "mbean already registered: " + name);
        } catch (MBeanRegistrationException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotCompliantMBeanException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AccessControlException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (SecurityException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (Error e) {
            // all errors, just info; do not prevent loading
            // IKVM does not support JMX, and throws an error
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.FINE, null, e);
        }
    }

    private void unregister(String name) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            ObjectName beanName = new ObjectName(name);
            mbs.unregisterMBean(beanName);
        } catch (InstanceNotFoundException ex) {
        } catch (MBeanRegistrationException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AccessControlException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (SecurityException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (Error e) {
            // all errors, just info; do not prevent unloading
            // IKVM does not support JMX, and throws an error
            Logger.getLogger(BeanManagerImpl.class.getName()).log(Level.FINE, null, e);
        }
    }
}
