require 'java'
require 'rbconfig'
require 'test/unit'
require 'test/jruby/test_helper'
require 'jruby/core_ext'

TopLevelConstantExistsProc = Proc.new do
  java_import 'java.lang.String'
end

class TestHigherJavasupport < Test::Unit::TestCase
  include TestHelper
  TestHelper = org.jruby.test.TestHelper
  JArray = ArrayList = java.util.ArrayList
  FinalMethodBaseTest = org.jruby.test.FinalMethodBaseTest
  Annotation = java.lang.annotation.Annotation
  ClassWithPrimitive = org.jruby.test.ClassWithPrimitive

  ALLOW_UPPERCASE_PACKAGE_NAMES = JRuby.runtime.getInstanceConfig.getAllowUppercasePackageNames

  def test_java_int_primitive_assignment
    assert_nothing_raised {
      cwp = ClassWithPrimitive.new
      cwp.an_int = nil
      assert_equal 0, cwp.an_int
    }
  end

  class JRUBY5564; end
  def test_reified_class_in_jruby_class_loader
    a_class = JRUBY5564.become_java!(false)

    # load the java class from the classloader
    cl = java.lang.Thread.current_thread.getContextClassLoader
    if IS_COMMAND_LINE_EXECUTION
      assert_equal cl.load_class(a_class.get_name), a_class
    else
      assert_raise { cl.load_class(a_class.get_name) }
    end
  end

  def test_java_passing_class
    assert_equal("java.util.ArrayList", TestHelper.getClassName(ArrayList))
  end

  @@include_java_lang = Proc.new {
      include_package "java.lang"
      java_alias :JavaInteger, :Integer
  }

  def test_java_class_loading_and_class_name_collisions
    assert_raises(NameError) { System }
    @@include_java_lang.call
    assert_nothing_raised { System }
    assert_equal(10, JavaInteger.new(10).intValue)
    assert_raises(NoMethodError) { Integer.new(10) }
  end

  Random = java.util.Random
  Double = java.lang.Double
  def test_constructors_and_instance_methods
    r = Random.new
    assert_equal(Random, r.class)
    r = Random.new(1001)
    assert_equal(10.0, Double.new(10).doubleValue())
    assert_equal(10.0, Double.new("10").doubleValue())

    assert_equal(Random, r.class)
    assert_equal(Fixnum, r.nextInt.class)
    assert_equal(Fixnum, r.nextInt(10).class)
  end

  Long = java.lang.Long
  def test_instance_methods_differing_only_on_argument_type
    l1 = Long.new(1234)
    l2 = Long.new(1000)
    assert(l1.compareTo(l2) > 0)
  end

  def test_dispatching_on_nil
    sb = TestHelper.getInterfacedInstance()
    assert_equal(nil, sb.dispatchObject(nil))
  end

  def test_class_methods
    result = java.lang.System.currentTimeMillis()
    assert_equal(Fixnum, result.class)
  end

  Boolean = java.lang.Boolean
  def test_class_methods_differing_only_on_argument_type
    assert_equal(true, Boolean.valueOf("true"))
    assert_equal(false, Boolean.valueOf(false))
  end

  Character = java.lang.Character
  def test_constants
    assert_equal 9223372036854775807, Long::MAX_VALUE
    assert(! defined? Character::Y_DATA)  # Known private field in Character

    # class definition with "_" constant causes error
    org.jruby.javasupport.test.VariableArguments
    assert_equal '_', org.jruby.javasupport.test.VariableArguments::_LEADING_UNDERSCORE
  end

  VarArgsCtor = org.jruby.javasupport.test.VariableArguments

  def test_varargs_constructor
    var_ar = VarArgsCtor.new
    assert_equal nil, var_ar.constants

    var_ar = VarArgsCtor.new '0'
    assert_equal '0', var_ar.constants[0]

    var_ar = org.jruby.javasupport.test.VariableArguments.new '0', '1'
    assert_equal '1', var_ar.constants[1]

    assert_raises(NameError) do
      org.jruby.javasupport.test.VariableArguments.new '0', 1
    end
  end

  class RubyVarArgsCtor1 < VarArgsCtor
    def initialize(a1, a2); super(a1, a2) end
  end

  class RubyVarArgsCtor2 < VarArgsCtor
    def initialize(); super(nil) end
  end

  class RubyVarArgsCtor3 < VarArgsCtor
    def initialize(); super() end
  end

  class RubyVarArgsCtor4 < VarArgsCtor
    # implicit initialize()
  end

  VarArgOnly = org.jruby.javasupport.test.VariableArguments::VarArgOnly

  class RubyVarArgsOnlyCtor1 < VarArgOnly
    # implicit initialize()
  end

  class RubyVarArgsOnlyCtor2 < VarArgOnly
    def initialize(); end
  end

  class RubyVarArgsOnlyCtor3 < VarArgOnly
    def initialize(*args); super end
  end

  class RubyVarArgsOnlyCtor4 < VarArgOnly
    def initialize(*args)
      super(args.to_java)
    end
  end

  StringVarArgOnly = org.jruby.javasupport.test.VariableArguments::StringVarArgOnly

  class RubyVarArgsOnlyCtor5 < StringVarArgOnly
    # NOTE: do not work (so far) due component type mismatch :
    #def initialize(*args); super end
    #def initialize(*args); super(*args) end
    def initialize(*args)
      super(args.to_java(:String))
    end
  end

  def test_varargs_constructor_in_ruby_subclass
    var_args = RubyVarArgsCtor1.new 'foo', 'bar'
    assert_equal 'foo', var_args.constants[0]
    assert_equal 'bar', var_args.constants[1]

    var_args = RubyVarArgsCtor2.new
    assert_equal nil, var_args.constants[0]

    var_args = RubyVarArgsCtor3.new
    assert_equal nil, var_args.constants

    var_args = RubyVarArgsCtor4.new
    assert_equal nil, var_args.constants

    #

    var_args = RubyVarArgsOnlyCtor1.new
    assert_equal 0, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor2.new
    assert_equal 0, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor3.new
    assert_equal 0, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor3.new('1')
    assert_equal 1, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor4.new
    assert_equal 0, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor4.new(1)
    assert_equal 1, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor4.new(1, 2)
    assert_equal 2, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor5.new
    assert_equal 0, var_args.constants.length

    var_args = RubyVarArgsOnlyCtor5.new('1')
    assert_equal 1, var_args.constants.length
  end

  def test_varargs_overloaded_method
    var_args = VarArgsCtor.new 'foo'
    var_args.setConstants 'bar'
    assert_equal 'bar', var_args.constants[0]

    var_args.setConstants 'foo,bar' # (String)
    assert_equal 'foo', var_args.constants[0]
    assert_equal 'bar', var_args.constants[1]

    var_args = RubyVarArgsOnlyCtor2.new
    var_args.setConstants 'foo', 'bar' # (String, String)
    assert_equal 'foo', var_args.constants[0]
    assert_equal 'bar', var_args.constants[1]

    var_args.setConstants 'foo', 'bar', 'baz' # (String, String...)
    assert_equal 'bar', var_args.constants[0]
    assert_equal 'baz', var_args.constants[1]
    assert_equal 'foo', var_args.constants[2]

    var_args.setConstants '1', '2', '3', '4', '5'
    assert_equal '2', var_args.constants[0]
    assert_equal '1', var_args.constants[4]
  end

  def test_varargs_only_method
    a = java.util.Arrays.asList(1)
    assert_equal 1, a[0]
    a = java.util.Arrays.asList('1', '2', '3')
    assert_equal '1', a[0]
    assert_equal '3', a[2]
    a = java.util.Arrays.asList('1', 2, 3.0, 4)
    assert_equal '1', a[0]
    assert_equal 2, a[1]
    a = java.util.Arrays.asList([1, 2])
    assert_equal [1, 2], a[0]
    a = java.util.Arrays.asList([1, 2], [3])
    assert_equal [1, 2], a[0]
    assert_equal [3], a[1]
    a = java.util.Arrays.asList([1, 2].to_java)
    assert_equal 1, a[0]
    assert_equal 2, a[1]
    a = java.util.Arrays.asList
    assert_equal 0, a.size
  end

  java_import org.jruby.javasupport.test.Room

  def test_using_arrays
    list = JArray.new
    list.add(10); list.add(20)
    array = list.toArray
    assert_equal(10, array[0])
    assert_equal(20, array[1])
    assert_equal 2, array.length

    array[1] = 1234
    assert_equal 10, array[0]
    assert_equal 1234, array[1]
    assert_equal [10, 1234], array.entries
    assert_equal 10, array.min

    assert_raises(ArgumentError) { array[3] } # IndexOutOfBoundsException

    array = Java::int[5].new
    assert_equal 5, array.size
    assert_equal 0, array[0]
    array[1] = 1; array[2] = 2; array[3] = 3; array[4] = 4
    assert_equal 1, array[1]
    assert_equal 4, array.max

    assert_raises(ArgumentError) { array[6] } # IndexOutOfBoundsException
    assert_raises(ArgumentError) { array[-1] } # IndexOutOfBoundsException

    room_array = Room[1].new
    assert_equal 1, room_array.length
    room1 = Room.new("Bedroom")
    room_array[0] = room1
    assert_equal room1, room_array[0]
    assert_equal 1, room_array.length
  end

  def test_creating_arrays
    array = Double[3].new
    assert_equal(3, array.length)
    array[0] = 3.14
    array[2] = 17.0
    assert_equal(3.14, array[0])
    assert_equal(17.0, array[2])

    array = Java::double[3, 2].new
    assert_equal(3, array.length)
    assert_equal(2, array[0].length)
    assert_equal(2, array[1].length)
    array[0][0] = 4.2
    array[1][1] = 0.1

    array = Java::byte[3]
    array = array[2].new # [3, 2]
    assert_equal 3, array.length
    assert_equal 2, array[0].length
    assert_equal 2, array[1].size
    array[0][0] = 1
    array[1][1] = 2

    array = java.lang.String[3, 2][1].new_instance
    assert_equal 3, array.length
    assert_equal 2, array[0].size
    assert_equal 2, array[1].length
    assert_equal 1, array[0][0].length
    assert_equal 1, array[0][1].size
    assert_equal 1, array[1][1].length
    array[0][0][0] = '0'
    array[1][1][0] = '1'

    array = java.lang.CharSequence[10].new
    assert_equal 10, array.size
    array[9] = 'ferko-suska'

    array = Java::JavaLang::Runnable[1, 2].new
    assert_equal 1, array.size
    assert_equal 2, array[0].size

    args = []; 1025.times { args << 1025 }
    begin
      array = java.lang.Object[ *args ]
      array.new
      fail "expected to raise (creating 1025 dimensional array)"
    rescue ArgumentError => e
      assert e.message
    end
  end

  def test_creating_arrays_proxy_class
    array = java.lang.String.new_array(10)
    assert_equal 10, array.length
    assert_equal 'jedna', ( array[1] = 'jedna' )

    array = Java::short.new_array(2)
    assert_equal 2, array.size
    assert_equal 256, ( array[0] = 256 )
  end

  def test_ruby_array_to_java_array
    array = [ 1 ].to_java(:int)
    assert_equal 1, array.length
    assert_equal 1, array[0]
    assert_equal Java::int, array.component_type
    array[0] = 1000001
    array[0] = java.lang.Integer.valueOf(100000)
    assert_equal 100000, array[0]

    array = [ 1, 2 ].to_java(:byte)
    assert_equal 2, array.length
    array[1] = 10
    begin
      array[1] = 1000
    rescue => e # RangeError: too big for byte: 1000
      assert e.message
    end
  end

  def test_ruby_array_to_multi_dimensional_java_array
    array = [ [ 1 ] ].to_java
    assert_equal 1, array.length
    assert_equal java.lang.Object, array.component_type
    assert_equal [ 1 ], array[0]
    assert_equal Array, array[0].class
  end

  def test_ruby_array_with_java_array_element_to_array
    array = [ "foo".to_java_bytes ]
    array = array.to_java Java::byte[]
    assert_equal 1, array.size
    assert_equal 3, array[0].size
    assert_equal Java::byte, array[0].component_type

    array = [ [ 1 ], "bar".to_java_bytes, [ 2 ] ].to_java(Java::byte[])
    assert_equal 3, array.length
    assert_equal Java::byte[], array.component_type
    assert_equal 3, array[1].length
    assert_equal 1, array[0][0]
    assert_equal 2, array[2][0]
  end

  def test_ruby_array_copy_data
    array = Java::short[5].new
    [ 0, 1, 2 ].copy_data array, 10
    assert_equal 0, array[0]
    assert_equal 1, array[1]
    assert_equal 2, array[2]
    assert_equal 10, array[3]
    assert_equal 10, array[4]

    array = java.lang.Long[5].new
    [ 0, 1, 2 ].copy_data array
    assert_equal 0, array[0]
    assert_equal 1, array[1]
    assert_equal 2, array[2]
    assert_equal nil, array[3]
    assert_equal nil, array[4]
  end

  def test_ruby_array_dimensions
    assert_equal [ 0 ], [].dimensions
    assert_equal [ 1 ], [ 0 ].dimensions
    assert_equal [ 1 ], [ 42 ].dimensions
    assert_equal [ 1, 1 ], [ [ 42 ] ].dimensions
    assert_equal [ 2, 3 ], [ [ 0 ], [ 1, [2, 3], 4 ] ].dimensions
    assert_equal [ 2, 1 ], [ [], [ 0 ] ].dimensions
  end

  def test_void
    assert Java::void
    assert Java::Void
    begin
      Java::void[1].new
      fail "expected to raise"
    rescue ArgumentError => e
      assert_equal "Java package `void' does not have a method `[]'", e.message
    end
  end

  class IntLike
    def initialize(value)
      @value = value
    end
    def to_int; @value end
  end

  def test_array_with_non_ruby_integer_indexes
    size = IntLike.new(2)
    array = Java::byte[size].new

    array[ 0 ] = 42.to_java(:byte)
    assert_equal 42, array[ 0.to_java(:int) ]
    # TODO: this should work as well, right?!
    #assert_equal 42, array[ 0.to_java(:short) ]

    assert_equal 42, array[ IntLike.new(0) ]

    array[ 1.to_java('java.lang.Integer') ] = 21
    assert_equal 21, array[1]

    array[ IntLike.new(1) ] = 41
    assert_equal 41, array[1]

    assert_nil array.at(3)
    assert_equal 41, array.at( 1.0 )
    assert_nil array.at( IntLike.new(2) )
    assert_equal 42, array.at( IntLike.new(-2) )
    assert_equal 41, array.at( -1.to_java(:int) )
  end

  def test_array_eql_and_hash
    array1 = java.lang.Long[4].new
    array2 = java.lang.Long[4].new

    do_test_eql_arrays(array1, array2)

    array1 = Java::long[5].new
    array2 = Java::long[5].new

    do_test_eql_arrays(array1, array2)

    array1 = Java::long[4].new
    array2 = Java::long[5].new
    assert_equal false, array1 == array2
  end

  def do_test_eql_arrays(array1, array2)
    assert_equal(array1, array2)
    assert array1.eql?(array2)

    array1[0] = 1
    assert_equal false, array1.eql?(array2)

    array2[0] = 1
    array2[1] = 2
    array2[2] = 3
    array1[1] = 2
    array1[2] = 3

    assert_equal(array2, array1)
    assert_equal(array2.hash, array1.hash)
    assert array2.eql?(array1)
    assert_equal true, array1 == array2

    assert ! array2.equal?(array1)
    assert array2.equal?(array2)
  end
  private :do_test_eql_arrays

  def test_bare_eql_and_hash
    l1 = java.lang.Long.valueOf 100_000_000
    l2 = java.lang.Long.valueOf 100_000_000
    assert l1.eql? l2
    assert l1 == l2

    s1 = Java::JavaLang::String.new 'a-string'
    s2 = 'a-string'.to_java
    assert ! s1.equal?(s2)
    assert s1.eql? s2
    assert s1 == s2

    a1 = java.util.Arrays.asList(1, 2, 3)
    a2 = java.util.ArrayList.new
    a2 << 0; a2 << 2; a2 << 3
    assert_equal false, a1.eql?(a2)
    assert_equal false, a2 == a1
    assert_not_equal a1.hash, a2.hash
    a2[0] = 1
    assert_equal true, a2.eql?(a1)
    assert_equal true, a1 == a2
    assert_equal true, a2 == a1
    assert_equal a1.hash, a2.hash
    assert_equal false, a2.equal?(a1)
  end

  Pipe = java.nio.channels.Pipe
  def test_inner_classes
    assert_equal("java.nio.channels.Pipe$SinkChannel",
                 Pipe::SinkChannel.java_class.name)
    assert(Pipe::SinkChannel.instance_methods.include?(:keyFor))
  end

  def test_subclasses_and_their_return_types
    l = ArrayList.new
    r = Random.new
    l.add(10)
    assert_equal(10, l.get(0))
    l.add(r)
    r_returned = l.get(1)
    # Since Random is a public class we should get the value casted as that
    assert_equal("java.util.Random", r_returned.java_class.name)
    assert(r_returned.nextInt.kind_of?(Fixnum))
  end

  HashMap = java.util.HashMap
  def test_private_classes_interfaces_and_return_types
    h = HashMap.new
    assert_equal(HashMap, h.class)
    h.put("a", 1)
    iter = h.entrySet.iterator
    inner_instance_entry = iter.next
    # The class implements a public interface, MapEntry, so the methods
    # on that should be available, even though the instance is of a
    # private class.
    assert_equal("a", inner_instance_entry.getKey)
  end

  def test_extending_java_interfaces
    if java.lang.Comparable.instance_of?(Module)
      anonymous = Class.new(Object)
      anonymous.send :include, java.lang.Comparable
      anonymous.send :include, java.lang.Runnable
      assert anonymous < java.lang.Comparable
      assert anonymous < java.lang.Runnable
      assert anonymous.new.kind_of?(java.lang.Runnable)
      assert anonymous.new.kind_of?(java.lang.Comparable)
    else
      assert Class.new(java.lang.Comparable)
    end
  end

  def test_support_of_other_class_loaders
    assert_helper_class = Java::JavaClass.for_name("org.jruby.test.TestHelper")
    assert_helper_class2 = Java::JavaClass.for_name("org.jruby.test.TestHelper")
    assert(assert_helper_class.java_class == assert_helper_class2.java_class, "Successive calls return the same class")
    method = assert_helper_class.java_method('loadAlternateClass')
    alt_assert_helper_class = method.invoke_static()

    constructor = alt_assert_helper_class.constructor();
    alt_assert_helper = constructor.new_instance();
    identityMethod = alt_assert_helper_class.java_method('identityTest')
    identity = identityMethod.invoke(alt_assert_helper)
    assert_equal("ABCDEFGH",  identity)
  end

  module Foo
    java_import("java.util.ArrayList")
  end

  java_import("java.lang.String") {|package,name| "J#{name}" }
  java_import ["java.util.Hashtable", "java.util.Vector"]

  def test_class_constants_defined_under_correct_modules
    assert_equal(0, Foo::ArrayList.new.size)
    assert_equal("a", JString.new("a").to_s)
    assert_equal(0, Vector.new.size)
    assert_equal(0, Hashtable.new.size)
  end

  def test_high_level_java_should_only_deal_with_proxies_and_not_low_level_java_class
    a = JString.new
    assert(a.getClass().class != "Java::JavaClass")
  end

  # We had a problem with accessing singleton class versus class earlier. Sanity check
  # to make sure we are not writing class methods to the same place.
  java_import 'org.jruby.test.AlphaSingleton'
  java_import 'org.jruby.test.BetaSingleton'

  def test_make_sure_we_are_not_writing_class_methods_to_the_same_place
    assert_nothing_raised { AlphaSingleton.getInstance.alpha }
  end

  java_import 'org.jruby.javasupport.test.Color'

  def test_lazy_proxy_method_tests_for_alias_and_respond_to
    color = Color.new('green')
    assert_equal(true, color.respond_to?(:setColor))
    assert_equal(false, color.respond_to?(:setColorBogus))
  end

  class MyColor < Color
    alias_method :foo, :getColor
    def alias_test
      alias_method :foo2, :setColorReallyBogus
    end
  end

  def test_accessor_methods
    my_color = MyColor.new('blue')
    assert_equal('blue', my_color.foo)
    assert_raises(NoMethodError) { my_color.alias_test }
    my_color.color = 'red'
    assert_equal('red', my_color.color)
    my_color.setDark(true)
    assert_equal(true, my_color.dark?)
    my_color.dark = false
    assert_equal(false, my_color.dark?)
  end

  # No explicit test, but implicitly EMPTY_LIST.each should not blow up interpreter
  # Old error was EMPTY_LIST is a private class implementing a public interface with public methods
  java_import 'java.util.Collections'

  def test_empty_list_each_should_not_blow_up_interpreter
    assert_nothing_raised { Collections::EMPTY_LIST.each {|element| } }
  end

  def test_already_loaded_proxies_should_still_see_extend_proxy
    JavaUtilities.extend_proxy('java.util.List') do
      def foo
        true
      end
    end
    assert_equal(true, Foo::ArrayList.new.foo)
  end

  def test_same_proxy_does_not_raise
    # JString already included and it is the same proxy, so do not throw an error
    # (e.g. intent of java_import already satisfied)
    assert_nothing_raised do
      begin
        old_stream = $stderr.dup
        $stderr.reopen(RbConfig::CONFIG['target_os'] =~ /Windows|mswin/ ? 'NUL:' : '/dev/null')
        $stderr.sync = true
        class << self
          java_import("java.lang.String") {|package,name| "J#{name}" }
        end
      ensure
        $stderr.reopen(old_stream)
      end
    end
  end

  java_import 'java.util.Calendar'

  def test_date_time_conversion
    # Test java.util.Date <=> Time implicit conversion
    calendar = Calendar.getInstance
    calendar.setTime(Time.at(0))
    java_date = calendar.getTime

    assert_equal(java_date.getTime, Time.at(0).to_i)
  end

  def test_expected_java_string_methods_exist
    jstring_methods = %w[bytes charAt char_at compareTo compareToIgnoreCase compare_to
    compare_to_ignore_case concat contentEquals content_equals endsWith
    ends_with equals equalsIgnoreCase equals_ignore_case getBytes getChars
    getClass get_bytes get_chars get_class hashCode hash_code indexOf
    index_of intern java_class java_object java_object= lastIndexOf last_index_of
    length matches notify notifyAll notify_all regionMatches region_matches replace
    replaceAll replaceFirst replace_all replace_first split startsWith starts_with
    subSequence sub_sequence substring taint tainted? toCharArray toLowerCase
    toString toUpperCase to_char_array to_lower_case to_string
    to_upper_case trim wait]

    jstring_methods = jstring_methods.map(&:to_sym)

    jstring_methods.each { |method| assert(JString.public_instance_methods.include?(method), "#{method} is missing from JString") }
  end

  java_import 'java.math.BigDecimal'

  def test_big_decimal_interaction
    assert_equal(BigDecimal, BigDecimal.new("1.23").add(BigDecimal.new("2.34")).class)
  end

  def test_direct_package_access
    a = java.util.ArrayList.new
    assert_equal(0, a.size)
  end

  def test_reflected_field
    j_integer = Java::JavaClass.for_name('java.lang.Integer')
    begin
      j_integer.field('value')
      fail('value field is not public!')
    rescue NameError => e
      assert e
    end
    value_field = j_integer.declared_field('value')
    assert_equal false, value_field.static?
    assert_equal false, value_field.public?
    assert_equal true, value_field.final?
    assert_equal false, value_field.accessible?
    value_field.accessible = true
    assert_equal 123456789, value_field.value( 123456789.to_java(:int) )
    assert_equal 'int', value_field.value_type

    value1_field = Java::JavaClass.for_name('java.lang.reflect.Method').field(:DECLARED)
    value2_field = Java::JavaLangReflect::Constructor.java_class.field('DECLARED')

    assert_equal value1_field, value2_field
    assert_equal true, value2_field.eql?(value1_field)
    assert_equal 1, value2_field.static_value
    assert_equal true, value1_field.static?
    assert_equal true, value2_field.public?
    assert_equal true, value1_field.final?
  end

  def test_reflected_callable_to_s_and_inspect
    java_class = Java::JavaClass.for_name('java.util.ArrayList')
    constructors = java_class.constructors
    c = constructors.find { |constructor| constructor.parameter_types == [ Java::int.java_class ] }
    assert_equal '#<Java::JavaConstructor(int)>', c.inspect
    assert_equal 'public java.util.ArrayList(int)', c.to_s
    c = constructors.find { |constructor| constructor.parameter_types == [] }
    assert_equal '#<Java::JavaConstructor()>', c.inspect

    m = java_class.java_instance_methods.find { |method| method.name == 'get' }
    assert_equal '#<Java::JavaMethod/get(int)>', m.inspect
    assert_equal 'public java.lang.Object java.util.ArrayList.get(int)', m.to_s

    m = java_class.java_instance_methods.find { |method| method.name == 'set' }
    assert_equal '#<Java::JavaMethod/set(int,java.lang.Object)>', m.inspect
  end

  def test_java_class_callable_methods
    java_class = Java::JavaClass.for_name('java.util.ArrayList')
    assert java_class.declared_constructor
    assert java_class.constructor Java::int

    assert java_class.declared_method 'add', java.lang.Object
    assert java_class.declared_method 'add', 'int', 'java.lang.Object'
    assert java_class.declared_method 'size'
    assert java_class.declared_method 'indexOf', java.lang.Object.java_class
    begin
      java_class.declared_method 'indexOf'
      fail('not failed')
    rescue NameError => e
      assert e.message.index "undefined method 'indexOf'"
    end
  end

  def test_exposed_java_proxy_types
    Java::JavaProxyClass
    Java::JavaProxyMethod
    Java::JavaProxyConstructor
  end

#  def test_java_class_equality
#    long_class = java.lang.Long
#    assert_equal long_class, Java::DefaultPackageClass.returnLongClass
#  end

  Properties = Java::java.util.Properties

  def test_declare_constant
    p = Properties.new
    p.setProperty("a", "b")
    assert_equal("b", p.getProperty("a"))
  end

  if java.awt.event.ActionListener.instance_of?(Module)
    class MyBadActionListener
      include java.awt.event.ActionListener
    end
  else
    class MyBadActionListener < java.awt.event.ActionListener
    end
  end

  def test_expected_missing_interface_method
    assert_raises(NoMethodError) { MyBadActionListener.new.actionPerformed }
  end

  def test_that_misspelt_fq_class_names_dont_stop_future_fq_class_names_with_same_inner_most_package
    # NOTE: with ALLOW_UPPERCASE_PACKAGE_NAMES this raises nothing !
    assert_raises(NameError) { Java::java.til.zip.ZipFile }
    Java::java.util.zip.ZipFile
    Java::java.util.zip.ZipFile::OPEN_READ
  end

  def test_that_subpackages_havent_leaked_into_other_packages
    assert_equal(false, Java::java.respond_to?(:zip))
    assert_equal(false, Java::com.respond_to?(:util))
  end

  def test_that_sub_packages_called_java_javax_com_org_arent_short_circuited
    #to their top-level conterparts
    assert(!com.equal?(java.flirble.com))
  end

  def test_that_we_get_the_same_package_instance_on_subsequent_calls
    assert(com.flirble.equal?(com.flirble))
  end

  def test_uppercase_package_name_and_lowercase_class_name # and upper-case method
    Java::org.jruby.javasupport.TestApp
    Java::org.jruby.javasupport.TestApp.UpperClass
    assert_equal 'UGLY!', Java::org.jruby.javasupport.TestApp::UpperClass.UglyMethod

    Java::org.jruby.javasupport.TestApp::lowerClass
    assert_equal 'ugly!', Java::org.jruby.javasupport.TestApp.lowerClass.UglyMethod

    # NOTE: can not work due package case conventions :
    # Java::OrgJrubyJavasupportTestApp::UpperClass
    # Java::OrgJrubyJavasupportTestApp::lowerClass
  end if ALLOW_UPPERCASE_PACKAGE_NAMES

  @@include_proc = Proc.new do
    Thread.stop
    java_import "java.lang.System"
    java_import "java.lang.Runtime"
    Thread.current[:time] = System.currentTimeMillis
    Thread.current[:mem] = Runtime.getRuntime.freeMemory
  end

  # Disabled temporarily...keeps failing for no obvious reason
  def test_that_multiple_threads_including_classes_dont_step_on_each_other
    # we swallow the output to $stderr, so testers don't have to see the
    # warnings about redefining constants over and over again.
    threads = []

    begin
      old_stream = $stderr.dup
      $stderr.reopen(RbConfig::CONFIG['target_os'] =~ /Windows|mswin/ ? 'NUL:' : '/dev/null')
      $stderr.sync = true

      50.times do
        threads << Thread.new(&@@include_proc)
      end

      # wait for threads to all stop, then wake them up
      threads.each { |t| Thread.pass until t.stop? }
      threads.each(&:run)
      # join each to let them run
      threads.each(&:join)
      # confirm they all successfully called currentTimeMillis and freeMemory
    ensure
      $stderr.reopen(old_stream)
    end

    threads.each do |t|
      assert(t[:time])
      assert(t[:mem])
    end
  end

  if javax.xml.namespace.NamespaceContext.instance_of?(Module)

    class NSCT
      include javax.xml.namespace.NamespaceContext
      # JRUBY-66: No super here...make sure we still work.
      def initialize(arg); end
      def getNamespaceURI(prefix)
        'ape:sex'
      end
    end

  else

    class NSCT < javax.xml.namespace.NamespaceContext
      # JRUBY-66: No super here...make sure we still work.
      def initialize(arg); end
      def getNamespaceURI(prefix)
        'ape:sex'
      end
    end

  end

  def test_no_need_to_call_super_in_initialize_when_implementing_java_interfaces
    # No error is a pass here for JRUBY-66
    assert_nothing_raised do
      javax.xml.xpath.XPathFactory.newInstance.newXPath.setNamespaceContext(NSCT.new(1))
    end
  end

  def test_can_see_inner_class_constants_with_same_name_as_top_level
    # JRUBY-425: make sure we can reference inner class names that match
    # the names of toplevel constants
    ell = java.awt.geom.Ellipse2D
    assert_nothing_raised { ell::Float.new }
  end

  def test_that_class_methods_are_being_camel_cased
    assert(java.lang.System.respond_to?("current_time_millis"))
  end

  if Java::java.lang.Runnable.instance_of?(Module)
    class TestInitBlock
      include Java::java.lang.Runnable
      def initialize(&block)
        raise if !block
        @bar = block.call
      end
      def bar; @bar; end
    end
  else
    class TestInitBlock < Java::java.lang.Runnable
      def initialize(&block)
        raise if !block
        @bar = block.call
      end
      def bar; @bar; end
    end
  end

  def test_that_blocks_are_passed_through_to_the_constructor_for_an_interface_impl
    assert_nothing_raised {
      assert_equal("foo", TestInitBlock.new { "foo" }.bar)
    }
  end

  def test_no_collision_with_ruby_allocate_and_java_allocate
    # JRUBY-232
    assert_nothing_raised { java.nio.ByteBuffer.allocate(1) }
  end

  # JRUBY-636 and other "extending Java classes"-issues
  class BigInt < java.math.BigInteger
    def initialize(val)
      super(val)
    end
    def test
      "Bit count = #{bitCount}"
    end
  end

  def test_extend_java_class
    assert_equal 2, BigInt.new("10").bitCount
    assert_equal "Bit count = 2", BigInt.new("10").test
  end

  class TestOS < java.io.OutputStream
    attr_reader :written
    def write(p)
      @written = true
    end
  end

  def test_extend_output_stream
    _anos = TestOS.new
    bos = java.io.BufferedOutputStream.new _anos
    bos.write 32
    bos.flush
    assert _anos.written
  end

  def test_impl_shortcut
    $has_run = false
    java.lang.Runnable.impl do
      $has_run = true
    end.run

    assert $has_run
  end

  # JRUBY-674
  OuterClass = org.jruby.javasupport.test.OuterClass
  def test_inner_class_proxies
    assert defined?(OuterClass::PublicStaticInnerClass)
    assert OuterClass::PublicStaticInnerClass.instance_methods.include?(:a)

    assert !defined?(OuterClass::ProtectedStaticInnerClass)
    assert !defined?(OuterClass::DefaultStaticInnerClass)
    assert !defined?(OuterClass::PrivateStaticInnerClass)

    assert defined?(OuterClass::PublicInstanceInnerClass)
    assert OuterClass::PublicInstanceInnerClass.instance_methods.include?(:a)

    assert !defined?(OuterClass::ProtectedInstanceInnerClass)
    assert !defined?(OuterClass::DefaultInstanceInnerClass)
    assert !defined?(OuterClass::PrivateInstanceInnerClass)
  end

  # Test the new "import" syntax
  def test_import

    assert_nothing_raised {
      import java.nio.ByteBuffer
      ByteBuffer.allocate(10)
    }
  end

  def test_java_exception_handling
    list = ArrayList.new
    begin
      list.get(5)
      assert(false)
    rescue java.lang.IndexOutOfBoundsException => e
      assert(e.message =~ /Index: 5, Size: 0$/)
    end
  end

  # test for JRUBY-698
  def test_java_method_returns_null
    java_import 'org.jruby.test.ReturnsNull'
    rn = ReturnsNull.new

    assert_equal("", rn.returnNull.to_s)
  end

  # test for JRUBY-664
  class FinalMethodChildClass < FinalMethodBaseTest
  end

  def test_calling_base_class_final_method
    assert_equal("In foo", FinalMethodBaseTest.new.foo)
    assert_equal("In foo", FinalMethodChildClass.new.foo)
  end

  # test case for JRUBY-679
  # class Weather < java.util.Observable
  #   def initialize(temp)
  #     super()
  #     @temp = temp
  #   end
  # end
  # class Meteorologist < java.util.Observer
  #   attr_reader :updated
  #   def initialize(weather)
  #     weather.addObserver(self)
  #   end
  #   def update(obs, arg)
  #     @updated = true
  #   end
  # end
  # def test_should_be_able_to_have_different_ctor_arity_between_ruby_subclass_and_java_superclass
  #   assert_nothing_raised do
  #     w = Weather.new(32)
  #     m = Meteorologist.new(w)
  #     w.notifyObservers
  #     assert(m.updated)
  #   end
  # end

  class A < java.lang.Object
    include org.jruby.javasupport.test.Interface1

    def method1
    end
  end
  A.new

  class B < A
  	include org.jruby.javasupport.test.Interface2

  	def method2
  	end
  end
  B.new

  class C < B
  end
  C.new

  def test_interface_methods_seen
     ci = org.jruby.javasupport.test.ConsumeInterfaces.new
     ci.addInterface1(A.new)
     ci.addInterface1(B.new)
     ci.addInterface2(B.new)
     ci.addInterface1(C.new)
     ci.addInterface2(C.new)

  end

  class LCTestA < java::lang::Object
    include org::jruby::javasupport::test::Interface1

    def method1
    end
  end
  LCTestA.new

  class LCTestB < LCTestA
  	include org::jruby::javasupport::test::Interface2

  	def method2
  	end
  end
  LCTestB.new

  class java::lang::Object
    def boo
      'boo!'
    end
  end

  def test_lowercase_colon_package_syntax
    assert_equal(java::lang::String, java.lang.String)
    assert_equal('boo!', java.lang.String.new('xxx').boo)
    ci = org::jruby::javasupport::test::ConsumeInterfaces.new
    assert_equal('boo!', ci.boo)
    assert_equal('boo!', LCTestA.new.boo)
    assert_equal('boo!', LCTestB.new.boo)
    ci.addInterface1(LCTestA.new)
    ci.addInterface1(LCTestB.new)
    ci.addInterface2(LCTestB.new)
  end

  def test_marsal_java_object_fails
    assert_raises(TypeError) { Marshal.dump(java::lang::Object.new) }
  end

  def test_string_from_bytes
    assert_equal('foo', String.from_java_bytes('foo'.to_java_bytes))
  end

  # JRUBY-2088
  def test_package_notation_with_arguments
    assert_raises(ArgumentError) do
      java.lang("ABC").String
    end

    assert_raises(ArgumentError) do
      java.lang.String(123)
    end

    assert_raises(ArgumentError) do
      Java::se("foobar").com.Foobar
    end
  end

  # JRUBY-1545
  def test_creating_subclass_to_java_interface_raises_type_error
    assert_raises(TypeError) do
      eval(<<CLASSDEF)
class FooXBarBarBar < Java::JavaLang::Runnable
end
CLASSDEF
    end
  end

  # JRUBY-781
  def test_that_classes_beginning_with_small_letter_can_be_referenced
    assert_equal Module, org.jruby.test.smallLetterClazz.class
    assert_equal Class, org.jruby.test.smallLetterClass.class
  end

  # JRUBY-1076
  def test_package_module_aliased_methods
    assert java.lang.respond_to?(:__constants__)
    assert java.lang.respond_to?(:__methods__)

    java.lang.String # ensure java.lang.String has been loaded
    assert java.lang.__constants__.include?(:String)
  end

  # JRUBY-2106
  def test_package_load_doesnt_set_error
    $! = nil
    undo = javax.swing.undo
    assert_nil($!)
    assert undo
  end

  # JRUBY-2106
  def test_top_level_package_load_doesnt_set_error
    $! = nil
    Java::boom
    assert_nil($!)

    $! = nil
    Java::Boom
    assert_nil($!)
  end

  # JRUBY-2169
  def test_java_class_resource_methods
    path = 'test/org/jruby/javasupport/test/'
    # workaround for https://github.com/jruby/jruby/issues/2216
    path = File.expand_path(path) if ENV_JAVA['user.dir'] != Dir.pwd
    $CLASSPATH << path

    file = 'test_java_class_resource_methods.properties'

    jc = JRuby.runtime.jruby_class_loader

    # get resource as URL
    url = jc.resource_as_url(file)
    assert(java.net.URL === url)
    assert(url.path == File.expand_path(url.path))
    assert(/^foo=bar/ =~ java.io.DataInputStream.new(url.content).read_line)

    # get resource as stream
    is = jc.resource_as_stream(file)
    assert(java.io.InputStream === is)
    assert(/^foo=bar/ =~ java.io.DataInputStream.new(is).read_line)


    # get resource as string
    str = jc.resource_as_string(file)
    assert(/^foo=bar/ =~ str)
  end

  # JRUBY-2169
  def test_ji_extended_methods_for_java_1_5
    jc = java.lang.String.java_class
    ctor = jc.constructors[0]
    meth = jc.java_instance_methods[0]
    field = jc.fields[0]

    # annotations
    assert(Annotation[] === jc.annotations)
    assert(Annotation[] === ctor.annotations)
    assert(Annotation[] === meth.annotations)
    assert(Annotation[] === field.annotations)

    # TODO: more extended methods to test


  end

  # JRUBY-2169
  def test_java_class_ruby_class
    assert java.lang.Object.java_class.ruby_class == java.lang.Object
    assert java.lang.Runnable.java_class.ruby_class == java.lang.Runnable
  end

  def test_null_toString
    assert nil == org.jruby.javasupport.test.NullToString.new.to_s
  end

  # JRUBY-2277
  # kind of a strange place for this test, but the error manifested
  # when JI was enabled.  the actual bug was a problem in alias_method,
  # and not related to JI; see related test in test_methods.rb
  def test_alias_method_with_JI_enabled_does_not_raise
    name = Object.new
    def name.to_str
      "new_name"
    end
    assert_nothing_raised { String.send("alias_method", name, "to_str") }
  end

  # JRUBY-2671
  def test_coerce_array_to_java_with_javaobject_inside
    x = nil
    assert_nothing_raised { x = java.util.ArrayList.new([java.lang.Integer.new(1)]) }
    assert_equal("[1]", x.to_string)
  end

  # JRUBY-2865
  def test_extend_default_package_class
    cls = Class.new(Java::DefaultPackageClass);
    assert_nothing_raised { cls.new }
  end

  # JRUBY-3046
  def test_java_methods_have_arity
    assert_nothing_raised do
      assert_equal(-1, java.lang.String.instance_method(:toString).arity)
    end
  end

  # JRUBY-3476
  def test_object_with_singleton_returns_java_class
    x = java.lang.Object.new
    def x.foo; end
    assert(x.java_class.kind_of?Java::JavaClass)
  end

  # JRUBY-4524
  class IncludePackageSuper
    def self.const_missing(a)
      a
    end
  end

  class IncludePackageSub < IncludePackageSuper
    include_package 'java.util'

    def arraylist
      ArrayList
    end

    def blahblah
      BlahBlah
    end
  end
  def test_include_package_chains_super
    assert_equal java.util.ArrayList, IncludePackageSub.new.arraylist
    assert_equal :BlahBlah, IncludePackageSub.new.blahblah
  end

  # JRUBY-4529
  def test_float_always_coerces_to_java_float
    assert_nothing_raised do
      a = 1.0
      loop do
        a /= 2
        a.to_java :float
        break if a == 0.0
      end
    end
  end

  def test_no_warnings_on_concurrent_package_const_initialization
    output = with_stderr_captured do
      threads = (0..10).map do # smt not yet initialized :
        Thread.new { Java::JavaTextSpi::CollatorProvider }
      end
      threads.each { |thread| thread.join }
    end
    # expect no already initialized constant warning written e.g.
    # file:/.../jruby.jar!/jruby/java/java_module.rb:4 warning: already initialized constant JavaTextSpi
    assert ! output.index('already initialized constant'), output
  end

  def test_no_warnings_on_concurrent_class_const_initialization
    Java.send :remove_const, :DefaultPackageClass if Java.const_defined? :DefaultPackageClass

    output = with_stderr_captured do
      threads = (0..10).map do
        Thread.new { Java::DefaultPackageClass }
      end
      threads.each { |thread| thread.join }
    end
    # expect no already initialized constant warning written e.g.
    # ... warning: already initialized constant DefaultPackageClass
    assert ! output.index('already initialized constant'), output
  end

  # reproducing https://github.com/jruby/jruby/issues/2014
  def test_concurrent_proxy_class_initialization_invalid_method_dispatch
    abort_on_exception = Thread.abort_on_exception
    begin
      Thread.abort_on_exception = true
      # some strange enough (un-initialized proxy) classes not used elsewhere
      threads = (0..10).map do
        Thread.new { Java::java.awt.Desktop::Action.valueOf('OPEN') }
      end
      threads.each { |thread| thread.join }

      # same but top (package) level class and using an aliased method :
      threads = (0..10).map do
        Thread.new { java.lang.management.MemoryType.value_of('HEAP') }
      end
      threads.each { |thread| thread.join }
    ensure
      Thread.abort_on_exception = abort_on_exception
    end
  end

  # reproducing https://github.com/jruby/jruby/issues/1621
  def test_concurrent_proxy_class_initialization_dead_lock
    timeout = 0.5; threads_to_kill = []
    begin
      threads = %w{ A B C D E F G H }.map do |sym|
        Thread.new { Java::Default.const_get "Bug1621#{sym}" }
      end
      threads.each do |thread|
        threads_to_kill << thread if thread.join(timeout).nil?
      end
      if threads_to_kill.any?
        fail "threads: #{threads_to_kill.inspect} dead-locked!"
      end
    ensure
      threads_to_kill.each { |thread| thread.exit rescue nil }
    end
  end

  def test_callable_no_match_raised_errors
    begin
      java.lang.StringBuilder.new([])
      fail 'expected to raise'
    rescue NameError => e
      msg = e.message
      assert msg.start_with?('no constructor for arguments (org.jruby.RubyArray) on Java::JavaLang::StringBuilder'), msg
      assert msg.index('available overloads'), msg
      assert msg.index('  (int)'), msg
      assert msg.index('  (java.lang.String)'), msg
      assert msg.index('  (java.lang.CharSequence)'), msg
    end

    begin
      java.lang.Short.valueOf({})
      fail 'expected to raise'
    rescue => e # NameError
      msg = e.message
      assert msg.start_with?("no method 'valueOf' for arguments (org.jruby.RubyHash) on Java::JavaLang::Short"), msg
      assert msg.index('available overloads'), msg
      assert msg.index('  (short)'), msg
      assert msg.index('  (java.lang.String)'), msg
    end

    begin # no arguments (has special handling)
      java.lang.Short.valueOf
      fail 'expected to raise'
    rescue ArgumentError => e
      assert e.message.start_with?("no method 'valueOf' (for zero arguments) on Java::JavaLang::Short"), e.message
    end

    begin # instance method
      java.lang.String.new('').getBytes 42
      fail 'expected to raise'
    rescue => e # NameError
      msg = e.message
      assert msg.start_with?("no method 'getBytes' for arguments (org.jruby.RubyFixnum) on Java::JavaLang::String"), msg
      assert msg.index('available overloads'), msg
      assert msg.index('  (java.lang.String)'), msg
    end
  end

  def test_raised_errors_on_array_proxy
    begin # array proxy
      Java::byte[3].new.length('')
      fail 'expected to raise'
    rescue ArgumentError => e
      msg = e.message
      assert msg.start_with?("wrong number of arguments calling `length` (1 for 0)"), msg
    end

    begin # array proxy class
      Java::byte[3].size
      fail 'expected to raise'
    rescue NoMethodError => e
      assert e.message # undefined method `size' for #<ArrayJavaProxyCreator:0x3125fd2d>
    end
  end

  def test_no_ambiguous_java_constructor_warning_for_exact_match
    output = with_stderr_captured do # exact match should not warn :
      color = java.awt.Color.new(100.to_java(:int), 1.to_java(:int), 1.to_java(:int))
      assert_equal 100, color.getRed # assert we called (int,int,int)
    end
    # warning: ambiguous Java methods found, using java.awt.Color(int,int,int)
    assert ! output.index('ambiguous'), output

    output = with_stderr_captured do # java.lang.Object match should not warn
      # ... when overloaded methods are primitive and we're not passing one :
      format = Java::JavaText::DecimalFormat.new('')
      value = java.math.BigDecimal.new('10.000000000012')
      assert_equal "10.000000000012", format.format(value) # format(java.lang.Object)

      value = java.lang.Float.valueOf('0.0000000001')
      assert format.format(value).start_with?('.00000000010') # format(double)
      assert format.format(value.to_java).start_with?('.00000000010') # format(double)
    end
    # warning: ambiguous Java methods found, using format(java.lang.Object)
    assert ! output.index('ambiguous'), output
  end

  def test_no_ambiguous_java_constructor_warning_with_semi_exact_match
    output = with_stderr_captured do # exact match should not warn :
      color = java.awt.Color.new(10, 10, 10)
      assert_equal 10, color.getRed # assert we called (int,int,int)
    end
    # warning: ambiguous Java methods found, using java.awt.Color(int,int,int)
    assert ! output.index('ambiguous'), output

    output = with_stderr_captured do # exact match should not warn :
      color = java.awt.Color.new(1.0, 1.0, 1.0)
      assert_equal 255, color.getRed # assert we called (float,float,float)
    end
    # warning: ambiguous Java methods found, using java.awt.Color(int,int,int)
    assert ! output.index('ambiguous'), output
  end

# NOTE: this might be desired to be implemented - except coercion it's all in
#  def test_java_constructor_with_prefered_match
#    output = with_stderr_captured do # exact match should not warn :
#      color = java.awt.Color.new(10, 10, 1.0)
#      assert_equal 10, color.getRed # assert we called (int,int,int)
#    end
#    # warning: ambiguous Java methods found, using java.awt.Color(int,int,int)
#    assert ! output.index('ambiguous'), output
#
#    output = with_stderr_captured do # exact match should not warn :
#      color = java.awt.Color.new(1.0, 0.1, 1)
#      assert_equal 255, color.getRed # assert we called (float,float,float)
#    end
#    # warning: ambiguous Java methods found, using java.awt.Color(int,int,int)
#    assert ! output.index('ambiguous'), output
#  end

  # original report: https://jira.codehaus.org/browse/JRUBY-5582
  # NOTE: we're not testing this "early" on still a good JI exercise
  def test_set_security_manager
    security_manager = java.lang.System.getSecurityManager
    begin
      java.lang.System.setSecurityManager( JRubySecurityManager.new )
      assert java.lang.System.getSecurityManager.is_a?(JRubySecurityManager)
      #puts java.lang.System.getSecurityManager.checked_perms.inspect
    ensure
      java.lang.System.setSecurityManager( security_manager )
    end
  end

  class JRubySecurityManager < java.lang.SecurityManager
    def initialize; @checked = [] end
    def checked_perms; @checked end

    def checkPermission( perm ); @checked << perm end
  end

  private

  def with_stderr_captured
    stderr = $stderr; require 'stringio'
    begin
      $stderr = StringIO.new
      yield
      $stderr.string
    ensure
      $stderr = stderr
    end
  end

end
