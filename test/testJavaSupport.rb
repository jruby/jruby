require 'minirunit'
test_check "Java Support"

if defined? Java

  Java.import("java.lang")

  test_ok(ClassLoader.type)

  Java.name("Integer", "JavaInteger")

  test_ok(JavaInteger.respond_to?("parseInt"))
  test_ok(JavaInteger::MAX_VALUE > 0)
  test_equal(10, JavaInteger.parseInt("10"))
  test_equal(22, JavaInteger.new(22).intValue())
  test_equal(JavaInteger.parseInt("10"),
             JavaInteger.parseInt("10"))

  test_exception(NameError) { Cloneable.new }
  test_equal([Cloneable, Object, Kernel], Cloneable.ancestors)

  # Inner classes
  #Java.import("java.awt.geom")
  #test_ok(Ellipse2D.type)
  #test_ok(Ellipse2D::Float.type)

  Java.import("org.jruby.test")

  unless System.getProperty("jruby.script").nil?
    # FIXME: I have no idea why this class isn't available when
    # the test suite is run from ant/junit!? --Anders

    test_equal(nil, TestHelper.getNull())

    # Test casting:
    # The instance o's actual class is private, but it's returned as a public
    # interface, which should work.
    o = TestHelper.getInterfacedInstance()
    test_equal("stuff done", o.doStuff())

    o = TestHelper.getLooslyCastedInstance()
    test_equal("stuff done", o.doStuff())


    test_exception(NameError) { Java::JavaClass.new }
    inner_class = Java::JavaClass.for_name("org.jruby.test.TestHelper$SomeImplementation")
    test_equal("org.jruby.test.TestHelper$SomeImplementation", inner_class.name)
  end
  string_class = Java::JavaClass.for_name("java.lang.String")
  test_equal("java.lang.String", string_class.to_s)
  test_exception(NameError) { Java::JavaClass.for_name("not.existing.Class") }
  test_ok(string_class.public?)
  test_ok(string_class.final?)
  runnable_class = Java::JavaClass.for_name("java.lang.Runnable")
  test_ok(! runnable_class.final?)
  test_ok(runnable_class.interface?)
  test_ok(! string_class.interface?)

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
  test_ok(string_methods.include?("charAt"))
  test_ok(string_methods.include?("substring"))

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
  Java::import("java.util")
  result = method.invoke(Random.new)
  test_ok(result.kind_of?(Fixnum))

  constructors = random_class.constructors
  test_equal(2, constructors.length)
  test_equal([0, 1], constructors.collect {|c| c.arity }.sort)
  constructor = random_class.constructor(:long)
  test_equal(1, constructor.arity)
  random = constructor.new_instance(Object, 2002)
  result = method.invoke(random)
  test_ok(result.kind_of?(Fixnum))
end

test_print_report
