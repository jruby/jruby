require 'test/minirunit'
test_check "Low-level Java Support"

if defined? Java


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

  # Instance variables
  rectangle_class = Java::JavaClass.for_name("java.awt.Rectangle")
  test_ok(rectangle_class.fields.include?("x"))
  test_ok(rectangle_class.fields.include?("y"))
  field = rectangle_class.field(:x)
  test_equal("int", field.value_type)
  test_ok(field.public?)
  test_ok(! field.static?)
  # ... to be continued ...

  # Constants
  integer_class = Java::JavaClass.for_name("java.lang.Integer")
  integer_constants = integer_class.constants
  test_ok(integer_constants.include?("MAX_VALUE"))
  max_value = integer_class.get_constant(:MAX_VALUE)
  test_ok(max_value.kind_of?(JavaObject))
  test_equal(2147483647, Java.java_to_primitive(max_value))

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
  random = constructor.new_instance(Java.primitive_to_java(2002))
  result = method.invoke(random)
  test_equal("java.lang.Integer", result.java_type)
  result = Java.java_to_primitive(result)
  test_ok(result.kind_of?(Fixnum))

  test_equal("java.lang.Long", Java.primitive_to_java(10).java_type)
  method = random_class.java_method(:nextInt, "int")
  test_equal(["int"], method.argument_types)
  test_exception(TypeError) { method.invoke(random, 10) }
  result = method.invoke(random, Java.primitive_to_java(10))
  test_equal("java.lang.Integer", result.java_type)

  method = string_class.java_method("valueOf", "int")
  test_ok(method.static?)
  result = method.invoke_static(Java.primitive_to_java(101))
  test_equal(string_class.to_s, result.java_type)

  # Control over return types and values
  test_equal("java.lang.String", method.return_type)
  test_equal(nil, string_class.java_method("notifyAll").return_type)
  test_equal(JavaObject,
             method.invoke_static(Java.primitive_to_java(101)).type)

  # Arrays
  array = string_class.new_array(10)
  test_equal(10, array.length)
  string_array_class = Java::JavaClass.for_name(array.java_type)
  test_ok(string_array_class.array?)
  test_equal("[Ljava.lang.String;", string_array_class.name)
  test_ok(string_array_class.constructors.empty?)
  test_ok(array[3].nil?)
  test_exception(ArgumentError) { array[10] }

  # java.lang.reflect.Proxy
  al = "java.awt.event.ActionListener"
  ae = "java.awt.event.ActionEvent"
  action_listener_class = Java::JavaClass.for_name(al)
  action_listener_instance = Java.new_proxy_instance(action_listener_class) do
    |proxy, method, event|

    test_ok(action_listener_instance.java_class == proxy.java_class)
    test_ok(method.instance_of? Java::JavaMethod)
    test_equal("actionPerformed", method.name())

    $callback_invoked = true
  end
  test_ok(action_listener_instance.instance_of? JavaObject)
  instance_class = action_listener_instance.java_class
  proxy_class = Java::JavaClass.for_name("java.lang.reflect.Proxy")
  test_ok(instance_class < action_listener_class)
  test_ok(action_listener_instance.java_class < proxy_class)
  action_performed = action_listener_class.java_method(:actionPerformed, ae)
  $callback_invoked = false
  action_performed.invoke(action_listener_instance, Java.primitive_to_java(nil))
  test_ok($callback_invoked)
end
