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
    assert_equal(nil , sb.dispatchObject(nil))
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
    assert_equal(9223372036854775807, Long::MAX_VALUE)
    assert(! defined? Character::Y_DATA)  # Known private field in Character
    # class definition with "_" constant causes error
    assert_nothing_raised { org.jruby.javasupport.test.ConstantHolder }
  end

  def test_using_arrays
    list = JArray.new
    list.add(10)
    list.add(20)
    array = list.toArray
    assert_equal(10, array[0])
    assert_equal(20, array[1])
    assert_equal(2, array.length)
    array[1] = 1234
    assert_equal(10, array[0])
    assert_equal(1234, array[1])
    assert_equal([10, 1234], array.entries)
    assert_equal(10, array.min)
  end

  def test_creating_arrays
    array = Double[3].new
    assert_equal(3, array.length)
    array[0] = 3.14
    array[2] = 17.0
    assert_equal(3.14, array[0])
    assert_equal(17.0, array[2])
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
    assert_raises(NameError) { Java::java.til.zip.ZipFile }
    assert_nothing_raised { Java::java.util.zip.ZipFile }
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

  @@include_proc = Proc.new do
    Thread.stop
    java_import "java.lang.System"
    java_import "java.lang.Runtime"
    Thread.current[:time] = System.currentTimeMillis
    Thread.current[:mem] = Runtime.getRuntime.freeMemory
  end

  # Disabled temporarily...keeps failing for no obvious reason
=begin
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
      threads.each {|t| Thread.pass until t.stop?}
      threads.each {|t| t.run}
      # join each to let them run
      threads.each {|t| t.join }
      # confirm they all successfully called currentTimeMillis and freeMemory
    ensure
      $stderr.reopen(old_stream)
    end

    threads.each do |t|
      assert(t[:time])
      assert(t[:mem])
    end
  end
=end

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
    stderr = $stderr; require 'stringio'
    begin
      $stderr = StringIO.new
      threads = (0..10).map do # smt not yet initialized :
        Thread.new { Java::JavaTextSpi::CollatorProvider }
      end

      threads.each { |thread| thread.join }

      # expect no already initialized constant warning written e.g.
      # file:/.../jruby.jar!/jruby/java/java_module.rb:4 warning: already initialized constant JavaTextSpi
      assert ! $stderr.string.index('already initialized constant'), $stderr.string
    ensure
      $stderr = stderr
    end
  end

  def test_no_warnings_on_concurrent_class_const_initialization
    Java.send :remove_const, :DefaultPackageClass if Java.const_defined? :DefaultPackageClass

    stderr = $stderr; require 'stringio'
    begin
      $stderr = StringIO.new
      threads = (0..10).map do
        Thread.new { Java::DefaultPackageClass }
      end

      threads.each { |thread| thread.join }

      # expect no already initialized constant warning written e.g.
      # ... warning: already initialized constant DefaultPackageClass
      assert ! $stderr.string.index('already initialized constant'), $stderr.string
    ensure
      $stderr = stderr
    end
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

end
