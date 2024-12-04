open module org.jruby.base {
    requires java.base;
    requires com.headius.options;
    requires org.jruby.jcodings;
    requires org.jruby.joni;
    requires org.objectweb.asm;
    requires org.objectweb.asm.util;
    requires org.objectweb.asm.commons;
    requires com.headius.backport9;
    requires com.headius.invokebinder;
    requires org.jnrproject.constants;
    requires org.jnrproject.enxio;
    requires org.jnrproject.ffi;
    requires org.jnrproject.netdb;
    requires org.jnrproject.posix;
    requires org.jnrproject.unixsocket;
    requires org.jnrproject.jffi;
    requires org.jnrproject.jffi.nativelibs;
    requires jdk.unsupported;
    requires org.joda.time;
    requires java.management;
    requires org.jruby.dirgra;
    requires org.jruby.jzlib;
    requires static java.scripting;
    requires static java.sql;
    requires static java.compiler;
    requires me.qmx.jitescript;
    requires static slf4j.api;
    requires static org.osgi.core;
    requires org.crac;
}
