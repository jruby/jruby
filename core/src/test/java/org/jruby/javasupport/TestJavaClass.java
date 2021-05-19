package org.jruby.javasupport;

import org.junit.Test;

import org.jruby.*;

public class TestJavaClass extends junit.framework.TestCase {

    @Test
    @SuppressWarnings("deprecation")
    public void testGet() {
        Ruby runtime = Ruby.newInstance();
        requireJava(runtime);

        JavaClass javaClass = JavaClass.get(runtime, String.class);
        assertSame(javaClass, JavaClass.get(runtime, String.class));
    }

    @Test
    public void testToJava() {
        Ruby runtime = Ruby.newInstance();
        requireJava(runtime);

        Object type;
        type = runtime.getClass("Object").toJava(java.lang.Class.class);
        assertSame(RubyObject.class, type);

        type = runtime.getClass("Array").toJava(java.lang.Class.class);
        assertSame(RubyArray.class, type);

        type = runtime.getClass("Hash").toJava(java.lang.Class.class);
        assertSame(RubyHash.class, type);

        type = runtime.getClass("File").toJava(java.lang.Class.class);
        assertSame(RubyFile.class, type);

        type = runtime.getClass("IO").toJava(java.lang.Class.class);
        assertSame(RubyIO.class, type);

        type = runtime.getClass("String").toJava(java.lang.Class.class);
        assertSame(RubyString.class, type);

        type = runtime.getClass("Symbol").toJava(java.lang.Class.class);
        assertSame(RubySymbol.class, type);

        type = runtime.getClass("Module").toJava(java.lang.Class.class);
        assertSame(RubyModule.class, type);

        type = runtime.getClass("Class").toJava(java.lang.Class.class);
        assertSame(RubyClass.class, type);

        type = runtime.getClass("Struct").toJava(java.lang.Class.class);
        assertSame(RubyStruct.class, type);

        type = runtime.getClass("Thread").toJava(java.lang.Class.class);
        assertSame(RubyThread.class, type);

        type = runtime.getClass("Exception").toJava(java.lang.Class.class);
        assertSame(RubyException.class, type);

        type = runtime.getClass("NameError").toJava(java.lang.Class.class);
        assertSame(RubyNameError.class, type);
    }

    @Test
    public void testToJavaObject() {
        Ruby runtime = Ruby.newInstance();
        requireJava(runtime);

        Object type;
        //Class klass = runtime.evalScriptlet("java.lang.Object").getJavaClass();
        //System.out.println(klass + " " + klass.getId());
        type = runtime.getClass("Object").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Object", ((RubyClass) type).getName());

        type = runtime.getClass("Array").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Array", ((RubyClass) type).getName());

        type = runtime.getClass("Hash").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Hash", ((RubyClass) type).getName());

        type = runtime.getClass("File").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("File", ((RubyClass) type).getName());

        type = runtime.getClass("IO").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("IO", ((RubyClass) type).getName());

        type = runtime.getClass("String").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("String", ((RubyClass) type).getName());

        type = runtime.getClass("Symbol").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Symbol", ((RubyClass) type).getName());

        type = runtime.getClass("Module").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Module", ((RubyClass) type).getName());

        type = runtime.getClass("Class").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Class", ((RubyClass) type).getName());

        //

        type = runtime.getClass("Integer").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Integer", ((RubyClass) type).getName());

        type = runtime.getClass("Fixnum").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Integer", ((RubyClass) type).getName());

        type = runtime.getClass("Float").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Float", ((RubyClass) type).getName());

        type = runtime.getClass("Rational").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Rational", ((RubyClass) type).getName());

        type = runtime.getClass("Dir").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Dir", ((RubyClass) type).getName());

        type = runtime.getModule("Kernel").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyModule);
        assertEquals("Kernel", ((RubyModule) type).getName());

        type = runtime.getModule("Enumerable").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyModule);
        assertEquals("Enumerable", ((RubyModule) type).getName());

        type = runtime.getClass("Struct").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Struct", ((RubyClass) type).getName());

        type = runtime.getClass("Thread").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Thread", ((RubyClass) type).getName());

        type = runtime.getClass("Exception").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("Exception", ((RubyClass) type).getName());

        type = runtime.getClass("NameError").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("NameError", ((RubyClass) type).getName());

        type = runtime.getClass("RuntimeError").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("RuntimeError", ((RubyClass) type).getName());

        //

        type = runtime.getClass("NilClass").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("NilClass", ((RubyClass) type).getName());

        type = runtime.getClass("FalseClass").toJava(java.lang.Object.class);
        assertTrue(type instanceof RubyClass);
        assertEquals("FalseClass", ((RubyClass) type).getName());
    }

    static void requireJava(final Ruby runtime) {
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(), "require", runtime.newString("java"));
    }

}
