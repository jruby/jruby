package org.jruby.javasupport;

import org.jruby.api.Access;
import org.jruby.runtime.ThreadContext;
import org.junit.Test;

import org.jruby.*;

import static org.jruby.api.Access.enumerableModule;
import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Create.newString;

public class TestJavaClass extends junit.framework.TestCase {

    @Test
    @SuppressWarnings("deprecation")
    public void testGet() {
        Ruby runtime = Ruby.newInstance();
        var context = runtime.getCurrentContext();
        requireJava(context);

        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }

    @Test
    public void testToJava() {
        Ruby runtime = Ruby.newInstance();
        var context = runtime.getCurrentContext();
        requireJava(context);

        Object type;
        type = Access.getClass(context, "Object").toJava(java.lang.Class.class);
        assertSame(RubyObject.class, type);

        type = Access.getClass(context, "Array").toJava(java.lang.Class.class);
        assertSame(RubyArray.class, type);

        type = Access.getClass(context, "Hash").toJava(java.lang.Class.class);
        assertSame(RubyHash.class, type);

        type = Access.getClass(context, "File").toJava(java.lang.Class.class);
        assertSame(RubyFile.class, type);

        type = Access.getClass(context, "IO").toJava(java.lang.Class.class);
        assertSame(RubyIO.class, type);

        type = Access.getClass(context, "String").toJava(java.lang.Class.class);
        assertSame(RubyString.class, type);

        type = Access.getClass(context, "Symbol").toJava(java.lang.Class.class);
        assertSame(RubySymbol.class, type);

        type = Access.getClass(context, "Module").toJava(java.lang.Class.class);
        assertSame(RubyModule.class, type);

        type = Access.getClass(context, "Class").toJava(java.lang.Class.class);
        assertSame(RubyClass.class, type);

        type = Access.getClass(context, "Struct").toJava(java.lang.Class.class);
        assertSame(RubyStruct.class, type);

        type = Access.getClass(context, "Thread").toJava(java.lang.Class.class);
        assertSame(RubyThread.class, type);

        type = Access.getClass(context, "Exception").toJava(java.lang.Class.class);
        assertSame(RubyException.class, type);

        type = Access.getClass(context, "NameError").toJava(java.lang.Class.class);
        assertSame(RubyNameError.class, type);
    }

    @Test
    public void testToJavaObject() {
        Ruby runtime = Ruby.newInstance();
        var context = runtime.getCurrentContext();
        requireJava(context);

        Object type;
        //Class klass = runtime.evalScriptlet("java.lang.Object").getJavaClass();
        //System.out.println(klass + " " + klass.getId());
        type = Access.getClass(context, "Object").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Object", ((RubyClass) type).getName());

        type = Access.getClass(context, "Array").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Array", ((RubyClass) type).getName());

        type = Access.getClass(context, "Hash").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Hash", ((RubyClass) type).getName());

        type = Access.getClass(context, "File").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("File", ((RubyClass) type).getName());

        type = Access.getClass(context, "IO").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("IO", ((RubyClass) type).getName());

        type = Access.getClass(context, "String").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("String", ((RubyClass) type).getName());

        type = Access.getClass(context, "Symbol").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Symbol", ((RubyClass) type).getName());

        type = Access.getClass(context, "Module").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Module", ((RubyClass) type).getName());

        type = Access.getClass(context, "Class").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Class", ((RubyClass) type).getName());

        //

        type = Access.getClass(context, "Integer").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Integer", ((RubyClass) type).getName());

        type = Access.getClass(context, "Fixnum").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Integer", ((RubyClass) type).getName());

        type = Access.getClass(context, "Float").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Float", ((RubyClass) type).getName());

        type = Access.getClass(context, "Rational").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Rational", ((RubyClass) type).getName());

        type = Access.getClass(context, "Dir").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Dir", ((RubyClass) type).getName());

        type = kernelModule(context).toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyModule);
        assertEquals("Kernel", ((RubyModule) type).getName());

        type = enumerableModule(context).toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyModule);
        assertEquals("Enumerable", ((RubyModule) type).getName());

        type = Access.getClass(context, "Struct").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Struct", ((RubyClass) type).getName());

        type = Access.getClass(context, "Thread").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Thread", ((RubyClass) type).getName());

        type = Access.getClass(context, "Exception").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Exception", ((RubyClass) type).getName());

        type = Access.getClass(context, "NameError").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("NameError", ((RubyClass) type).getName());

        type = Access.getClass(context, "RuntimeError").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("RuntimeError", ((RubyClass) type).getName());

        //

        type = Access.getClass(context, "NilClass").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("NilClass", ((RubyClass) type).getName());

        type = Access.getClass(context, "FalseClass").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("FalseClass", ((RubyClass) type).getName());
    }

    static void requireJava(ThreadContext context) {
        kernelModule(context).callMethod(context, "require", newString(context, "java"));
    }

}
