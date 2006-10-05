require 'test/minirunit'
require 'java'
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

  string_class   = Java::JavaClass.for_name("java.lang.String")
  test_equal(string_class, Java::JavaClass.for_name("java.lang.String"))

  test_equal("java.lang.String", string_class.to_s)
  test_equal(string_class.id, Java::JavaClass.for_name("java.lang.String").id)
  
  test_exception(NameError) { Java::JavaClass.for_name("not.existing.Class") }
  test_ok(string_class.public?)
  test_ok(string_class.final?)
  test_ok(! string_class.primitive?)
  runnable_class = Java::JavaClass.for_name("java.lang.Runnable")
  test_ok(! runnable_class.final?)
  test_ok(runnable_class.interface?)
  test_ok(! string_class.interface?)
  test_ok(! string_class.array?)

  inner_class = Java::JavaClass.for_name("java.lang.Character$Subset")
  test_equal("java.lang.Character$Subset", inner_class.name)

  #superclass
  sql_date_class = Java::JavaClass.for_name("java.sql.Date")
  object_class = string_class.superclass
  test_equal("java.lang.Object", object_class.name)
  test_equal(nil, object_class.superclass)
  test_equal(Java::JavaClass.for_name('java.util.Date'), sql_date_class.superclass)

  #interfaces
  interfaces = string_class.interfaces.collect{|i| i.name()}
  test_ok(interfaces.include?("java.lang.Comparable"))
  test_ok(interfaces.include?("java.io.Serializable"))
  test_ok(! interfaces.include?("java.lang.Object"))

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
  x_field = rectangle_class.field(:x)
  y_field = rectangle_class.field(:y)
  test_ok(rectangle_class.fields.include?(x_field))
  test_ok(rectangle_class.fields.include?(y_field))
  test_equal("int", x_field.value_type)
  test_equal("x", x_field.name)
  test_ok(x_field.public?)
  test_ok(! x_field.static?)
  test_ok(! x_field.final?)
  
  integer_ten = Java.primitive_to_java(10)
  integer_two = Java.primitive_to_java(2)
  constructor = rectangle_class.constructor(:int, :int, :int, :int)
  rectangle = constructor.new_instance(integer_two,
                                       integer_ten,
                                       integer_two,
                                       integer_two)
  value = x_field.value(rectangle)
  test_equal("java.lang.Integer", value.java_type)
  test_equal(2, Java.java_to_primitive(value))
  x_field.set_value(rectangle, integer_ten)
  value = x_field.value(rectangle)
  test_equal("java.lang.Integer", value.java_type)
  test_equal(10, Java.java_to_primitive(value))
  test_exception(TypeError) {
    x_field.set_value(rectangle, Java.primitive_to_java("hello"))
  }

  # private fields
  TestHelper = JavaUtilities.get_proxy_class('org.jruby.test.TestHelper')
  test_ok(TestHelper.java_class.declared_fields.find {|field| field.name == 'privateField' })
  privateField = TestHelper.java_class.declared_field('privateField')
  test_equal('privateField', privateField.name)
  test_ok(! privateField.public?)
  test_ok(! privateField.accessible?)
  privateField.accessible = true
  test_ok( privateField.accessible?)

  #private constructors 
  cons = TestHelper.java_class.declared_constructors() 
  con_private = cons.find {|con| con.arity == 1}

  test_ok(1,cons.length)
  test_ok(['java.lang.String'], cons[0].argument_types.collect{|arg_type| arg_type.name})
  test_ok(! con_private.public?)
  test_ok(! con_private.accessible?)
  con_private.accessible = true
  test_ok(con_private.accessible?)

  con = TestHelper.java_class.declared_constructor('java.lang.String')
  test_ok(con_private,con)

  #private instance methods
  test_ok(TestHelper.java_class.declared_instance_methods.find {|method| method.name == 'privateMethod'})
  privateMethod = TestHelper.java_class.declared_method('privateMethod')
  test_equal('privateMethod', privateMethod.name)
  test_ok( !privateMethod.accessible?)
  privateMethod.accessible = true
  test_ok( privateMethod.accessible?)

  helper = con_private.new_instance(Java::primitive_to_java("X"))
  test_equal('X', Java::java_to_primitive(privateMethod.invoke(helper)))

  #private static methods
  test_ok(TestHelper.java_class.declared_class_methods.find {|method| method.name == 'staticPrivateMethod'})
  privateMethod = TestHelper.java_class.declared_method('staticPrivateMethod')
  test_equal('staticPrivateMethod', privateMethod.name)
  test_ok( !privateMethod.accessible?)   
  privateMethod.accessible = true
  test_ok( privateMethod.accessible?)
  test_equal("staticPM", Java::java_to_primitive(privateMethod.invoke_static()))

  # Class variables
  integer_class = Java::JavaClass.for_name("java.lang.Integer")
  field = integer_class.field(:MAX_VALUE)
  test_ok(integer_class.fields.include?(field))
  test_ok(field.final?)
  test_ok(field.static?)
  test_equal(2147483647, Java.java_to_primitive(field.static_value))

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

  # Converting primitives
  java_string = random_class.java_method(:toString).invoke(random)
  test_equal("java.lang.String", java_string.java_type)
  test_ok(Java.java_to_primitive(java_string).kind_of?(String))
  test_ok(Java.java_to_primitive(random).kind_of?(Java::JavaObject))
  test_ok(Java.primitive_to_java(random) == random)
  test_ok(Java.primitive_to_java("hello").kind_of?(Java::JavaObject))
  test_equal("java.lang.String", Java.primitive_to_java("hello").java_type)
  # TODO: We want to actually find out if it is kind_of? java.util.List
  #test_equal("java.util.ArrayList", Java.primitive_to_java([1,2,3]).java_type)

  # Putting and getting objects back
  integer_zero = Java.primitive_to_java(0)
  arraylist_class = Java::JavaClass.for_name("java.util.ArrayList")
  list = arraylist_class.constructor().new_instance()
  add_method = arraylist_class.java_method(:add, "java.lang.Object")
  add_method.invoke(list, random)
  returned_random = arraylist_class.java_method(:get, "int").invoke(list, integer_zero)
  test_equal("java.util.Random", returned_random.java_type)
  random_class.java_method(:nextInt).invoke(returned_random)

  test_equal("java.lang.Long", Java.primitive_to_java(10).java_type)
  method = random_class.java_method(:nextInt, "int")
  test_equal(["int"], method.argument_types.collect{|arg_type| arg_type.name})
  test_exception(TypeError) { method.invoke(random, 10) }
  result = method.invoke(random, Java.primitive_to_java(10))
  test_equal("java.lang.Integer", result.java_type)

  method = string_class.java_method("valueOf", "int")
  test_ok(method.static?)
  result = method.invoke_static(Java.primitive_to_java(101))
  test_equal(string_class.to_s, result.java_type)

  # Control over return types and values
  test_equal("java.lang.String", method.return_type.name)
  test_equal(nil, string_class.java_method("notifyAll").return_type)
  test_equal(Java::JavaObject,
  method.invoke_static(Java.primitive_to_java(101)).class)

  # Not arrays
  test_exception(TypeError) { random[0] }     # Not an array
  test_exception(TypeError) { random.length } # Not an array

  # Arrays
  constructed_array = string_class.new_array(10)
  test_equal(10, constructed_array.length)
  string_array_class = Java::JavaClass.for_name(constructed_array.java_type)
  test_ok(string_array_class.array?)
  test_equal("[Ljava.lang.String;", string_array_class.name)
  test_ok(string_array_class.constructors.empty?)
  test_equal("java.lang.String", string_array_class.component_type.name)
  test_ok(constructed_array[3].nil?)
  test_exception(ArgumentError) { constructed_array[10] }
  constructed_array[3] = Java.primitive_to_java("hello")
  test_equal("hello", Java.java_to_primitive(constructed_array[3]))
  test_ok(constructed_array[4].nil?)

  list = arraylist_class.constructor().new_instance()
  returned_array = arraylist_class.java_method(:toArray).invoke(list)
  test_equal(0, returned_array.length)

  # java.lang.reflect.Proxy
  al = "java.awt.event.ActionListener"
  ae = "java.awt.event.ActionEvent"
  action_listener_class = Java::JavaClass.for_name(al)
  action_listener_instance = Java.new_proxy_instance(action_listener_class) do
    |proxy, method, event|

    test_ok(action_listener_instance.java_class == proxy.java_class)
    test_ok(method.instance_of?(Java::JavaMethod))
    test_equal("actionPerformed", method.name())

    $callback_invoked = true
  end
  test_ok(action_listener_instance.instance_of?(Java::JavaObject))
  instance_class = action_listener_instance.java_class
  proxy_class = Java::JavaClass.for_name("java.lang.reflect.Proxy")
  test_ok(instance_class < action_listener_class)
  test_ok(action_listener_instance.java_class < proxy_class)
  action_performed = action_listener_class.java_method(:actionPerformed, ae)
  $callback_invoked = false
  action_performed.invoke(action_listener_instance, Java.primitive_to_java(nil))
  test_ok($callback_invoked)

  # Primitive Java types
  int_class = Java::JavaClass.for_name("int")
  test_ok(int_class.primitive?)
  boolean_class = Java::JavaClass.for_name("boolean")
  test_ok(boolean_class.primitive?)
  test_ok(Java::JavaClass.for_name("char").primitive?)

  # Assignability, non-primitives
  object_class = Java::JavaClass.for_name("java.lang.Object")
  test_ok(object_class.assignable_from?(string_class))
  test_ok(! string_class.assignable_from?(object_class))
#  test_ok(object_class.assignable_from?(nil.type))
#  test_ok(string_class.assignable_from?(nil.type))

  # Assignability, primitives
  long_class = Java::JavaClass.for_name("long")
  test_ok(int_class.assignable_from?(long_class))
  test_ok(! int_class.assignable_from?(boolean_class))
  character_class = Java::JavaClass.for_name("char")
  test_ok(int_class.assignable_from?(character_class))
  test_ok(character_class.assignable_from?(int_class))
#  test_ok(! int_class.assignable_from?(nil.type))
#  test_ok(! character_class.assignable_from?(nil.type))
  
  # Check method matching
  test_equal("int",org.jruby.javasupport.TypeMatcher.new.number(123))
  test_equal("float",org.jruby.javasupport.TypeMatcher.new.number(123.0))
end
