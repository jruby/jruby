require 'test/minirunit'
test_check "Low-level Java Support"

if defined? Java

#  Java.import("java.lang")

#  test_ok(ClassLoader.type)

#  Java.name("Integer", "JavaInteger")

#  test_ok(JavaInteger.respond_to?("parseInt"))
#  test_ok(JavaInteger::MAX_VALUE > 0)
#  test_equal(10, JavaInteger.parseInt("10"))
#  test_equal(22, JavaInteger.new(22).intValue())
#  test_equal(JavaInteger.parseInt("10"),
#             JavaInteger.parseInt("10"))

#  test_exception(NameError) { Cloneable.new }
#  test_equal([Cloneable, Object, Kernel], Cloneable.ancestors)

  # Inner classes
  #Java.import("java.awt.geom")
  #test_ok(Ellipse2D.type)
  #test_ok(Ellipse2D::Float.type)

#  Java.import("org.jruby.test")

#  unless System.getProperty("jruby.script").nil?
    # FIXME: I have no idea why this class isn't available when
    # the test suite is run from ant/junit!? --Anders

#    test_equal(nil, TestHelper.getNull())

    # Test casting:
    # The instance o's actual class is private, but it's returned as a public
    # interface, which should work.
#    o = TestHelper.getInterfacedInstance()
#    test_equal("stuff done", o.doStuff())

#    o = TestHelper.getLooslyCastedInstance()
#    test_equal("stuff done", o.doStuff())


#    test_exception(NameError) { Java::JavaClass.new }
#    inner_class = Java::JavaClass.for_name("org.jruby.test.TestHelper$SomeImplementation")
#    test_equal("org.jruby.test.TestHelper$SomeImplementation", inner_class.name)
#  end

  string_class = Java::JavaClass.for_name("java.lang.String")
  test_equal(string_class, Java::JavaClass.for_name("java.lang.String"))

  test_equal("java.lang.String", string_class.to_s)
  test_exception(NameError) { Java::JavaClass.for_name("not.existing.Class") }
  test_ok(string_class.public?)
  test_ok(string_class.final?)
  runnable_class = Java::JavaClass.for_name("java.lang.Runnable")
  test_ok(! runnable_class.final?)
  test_ok(runnable_class.interface?)
  test_ok(! string_class.interface?)
  test_ok(! string_class.array?)

  inner_class = Java::JavaClass.for_name("java.lang.Character$Subset")
  test_equal("java.lang.Character$Subset", inner_class.name)

  object_class = string_class.superclass
  test_equal("java.lang.Object", object_class.name)
  test_equal(nil, object_class.superclass)

  test_ok(string_class < object_class)
  test_ok(! (string_class > object_class))
  test_ok(object_class > string_class)
  test_ok(! (object_class < string_class))
  test_ok(object_class == object_class)
  test_ok(object_class != string_class)

  string_methods = string_class.java_instance_methods
  test_ok(string_methods.detect {|m| m.name == "charAt" })
  test_ok(string_methods.detect {|m| m.name == "substring"})
  test_ok(string_methods.detect {|m| m.name == "valueOf"}.nil?)
  test_ok(string_methods.all? {|m| not m.static? })

  string_class_methods = string_class.java_class_methods
  test_ok(string_class_methods.detect {|m| m.name == "valueOf" })
  test_ok(string_class_methods.all? {|m| m.static? })

  integer_constants = Java::JavaClass.for_name("java.lang.Integer").constants
  test_ok(integer_constants.include?("MAX_VALUE"))

  method = string_class.java_method(:toString)
  test_ok(method.kind_of?(Java::JavaMethod))
  test_equal("toString", method.name)
  test_equal(0, method.arity)
  test_ok(method.public?)
  method = string_class.java_method("equals", "java.lang.Object")
  test_equal("equals", method.name)
  test_equal(1, method.arity)
  test_ok(! method.final?)

  random_class = Java::JavaClass.for_name("java.util.Random")
  method = random_class.java_method(:nextInt)

  constructors = random_class.constructors
  test_equal(2, constructors.length)
  test_equal([0, 1], constructors.collect {|c| c.arity }.sort)
  constructor = random_class.constructor(:long)
  test_equal(1, constructor.arity)
  random = constructor.new_instance(Object, Java.primitive_to_java(2002))
  result = method.invoke(random)
  test_ok("java.lang.Integer", result.java_type)
  result = Java.java_to_primitive(result)
  test_ok(result.kind_of?(Fixnum))

  test_equal("java.lang.Long", Java.primitive_to_java(10).java_type)
  method = random_class.java_method(:nextInt, "int")
  test_equal(["int"], method.argument_types)
  test_exception(TypeError) { method.invoke(random, 10) }
  result = method.invoke(random, Java.primitive_to_java(10))
  # ... result type?

  method = string_class.java_method("valueOf", "int")
  test_ok(method.static?)
  result = method.invoke(Java.primitive_to_java(101))
  test_equal(string_class.to_s, result.java_type)
end

test_print_report
