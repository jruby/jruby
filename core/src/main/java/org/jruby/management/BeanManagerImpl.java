package org.jruby.management;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jruby.Ruby;
import org.jruby.compiler.JITCompilerMBean;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class BeanManagerImpl implements BeanManager {

    private static final Logger LOG = LoggerFactory.getLogger("BeanManagerImpl");

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    public final String base;
    
    private final boolean managementEnabled;
    
    public BeanManagerImpl(Ruby ruby, boolean managementEnabled) {
        this.managementEnabled = managementEnabled;
        this.base = "org.jruby:type=Runtime,name=" + FORMAT.format(new Date()) +
                ruby.getRuntimeNumber() + ",";
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
    
    public void register(Runtime runtime) {
        if (managementEnabled) register(base + "service=Runtime", runtime);
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
    public void unregisterMethodCache() {
        if (managementEnabled) unregister(base + "service=MethodCache");
    }
    public void unregisterRuntime() {
        if (managementEnabled) unregister(base + "service=Runtime");
    }

    public boolean tryShutdownAgent() {
        try {
            Class agent = Class.forName("sun.management.Agent");
            Method shutdown = agent.getDeclaredMethod("stopRemoteManagementAgent");
            shutdown.setAccessible(true);
            shutdown.invoke(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean tryRestartAgent() {
        try {
            sun.management.Agent.startAgent();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void register(String name, Object bean) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            ObjectName beanName = new ObjectName(name);
            mbs.registerMBean(bean, beanName);
        } catch (InstanceAlreadyExistsException ex) {
            LOG.warn("mbean already registered: {}", name);
        } catch (MBeanRegistrationException ex) {
            LOG.error(ex);
        } catch (NotCompliantMBeanException ex) {
            LOG.error(ex);
        } catch (MalformedObjectNameException ex) {
            LOG.error(ex);
        } catch (NullPointerException ex) {
            LOG.error(ex);
        } catch (AccessControlException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (SecurityException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (Error e) {
            // all errors, just info; do not prevent loading
            // IKVM does not support JMX, and throws an error
            LOG.debug(e);
        }
    }

    private void unregister(String name) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            ObjectName beanName = new ObjectName(name);
            mbs.unregisterMBean(beanName);
        } catch (InstanceNotFoundException ex) {
        } catch (MBeanRegistrationException ex) {
            LOG.error(ex);
        } catch (MalformedObjectNameException ex) {
            LOG.error(ex);
        } catch (NullPointerException ex) {
            LOG.error(ex);
        } catch (AccessControlException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (SecurityException ex) {
            // ignore...bean doesn't get registered
            // TODO: Why does that bother me?
        } catch (Error e) {
            // all errors, just info; do not prevent unloading
            // IKVM does not support JMX, and throws an error
            LOG.debug(e);
        }
    }
}
