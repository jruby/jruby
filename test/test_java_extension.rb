require 'java'
require 'test/unit'

class TestJavaExtension < Test::Unit::TestCase

  class TestParent < org.jruby.test.Parent
    attr_accessor :result
    def run(a)
      @result = "TEST PARENT: #{a}"
    end
  end

  java_import org.jruby.test.Worker

  def test_overriding_method_in_java_superclass
    w = Worker.new
    p = TestParent.new
    w.run_parent(p)
    assert_equal "TEST PARENT: WORKER", p.result
  end

  import java.util.HashMap
  import java.util.ArrayList
  import java.util.HashSet

  def test_set
    set = HashSet.new
    set.add(1)
    set.add(2)

    newSet = []
    set.each { |x| newSet << x }

    assert newSet.include?(1)
    assert newSet.include?(2)
  end

  def test_comparable
    short = Java::JavaLang::Short
    one = short.new(1)
    two = short.new(2)
    three = short.new(3)
    list = [three, two, one]
    list = list.sort
    assert_equal([one, two, three], list)
  end

  def test_map
    map = HashMap.new
    map.put('A','1')
    map.put('C','3')

    hash = Hash.new
    map.each {|key, value| hash[key] = value }
    assert_equal('1', hash['A'])
    assert_equal('3', hash['C'])
  end

  def test_list
    a = ArrayList.new

    a << 3
    a << 1
    a << 2

    # Java 8 defines one-arg sort on all List impls that masks ours. See #1249.
    assert_equal([1, 2, 3], a.sort.to_a) if a.method(:sort).arity == 0
    assert_equal([1, 2], a[1...3].to_a)
    assert_equal([3, 1], a[0, 2].to_a)
    assert_equal([3, 2], a.select {|e| e > 1 })
  end

  import org.jruby.test.TestHelper
  import java.lang.RuntimeException
  import java.lang.NullPointerException
  import("org.jruby.test.TestHelper$TestHelperException") {"THException"}

  def test_catch_java_exception_with_rescue
    begin
      TestHelper.throwTestHelperException
    rescue THException => e
    end
  end

  def test_catch_multiple_java_exceptions_picks_correct_rescue
    begin
      TestHelper.throwTestHelperException
    rescue NullPointerException => e
      flunk("Should not rescue")
    rescue THException => e
    end
  end

  def test_catch_java_exception_by_superclass
    begin
      TestHelper.throwTestHelperException
    rescue RuntimeException => e
    end
  end

  def test_catch_java_exception_by_ruby_native_exception
    begin
      TestHelper.throwTestHelperException
    rescue NativeException => e
    end
  end

=begin See JRUBY-4677 for explanation of why this doesn't work yet
  def test_catch_unwrapped_java_exception_as_a_RubyException
    begin
      raise java.lang.NullPointerException.new
    rescue Exception => e
      assert e.is_a?(Exception)
    end
  end
=end

  def test_catch_unwrapped_java_exception_and_reraise
    begin
      raise java.lang.NullPointerException.new
    rescue Exception => e
      begin
        raise # should not cause ClassCastException
      rescue Exception => e
        assert_equal java.lang.NullPointerException, e.class
      end
    end
  end

  BLUE = "blue"
  GREEN = "green"

  import org.jruby.javasupport.test.Color

  def test_java_bean_conventions_in_ruby
    # Java bean convention properties as attributes
    color = Color.new(GREEN)

    assert !color.isDark
    color.dark = true
    assert color.dark
    assert color.dark?

    assert_equal GREEN, color.color

    color.color = BLUE
    assert_equal BLUE, color.color
  end

  def test_java_class_name
    assert_equal 'Java::OrgJrubyJavasupportTest::Color', Color.name

    assert_equal 'org.jruby.javasupport.test.Color', Color.java_class.name

    assert_equal 'Java::JavaLang::Runnable', java.lang.Runnable.name
    assert_equal 'java.lang.Runnable', Java::JavaLang::Runnable.java_class.name

    assert_equal 'Java::JavaLang::String', JString.name

    assert_equal 'Java::JavaLang::Thread::State', Java::java.lang.Thread::State.name
    # an enum class with custom code for each constant :
    assert_equal 'Java::JavaUtilConcurrent::TimeUnit::1', java.util.concurrent.TimeUnit::NANOSECONDS.class.name
    assert_equal 'Java::JavaUtilConcurrent::TimeUnit::2', java.util.concurrent.TimeUnit::MICROSECONDS.class.name
    assert java.util.concurrent.TimeUnit::MICROSECONDS.is_a?(java.util.concurrent.TimeUnit)
  end

  include_package 'org.jruby.javasupport.test'
  include_package 'java.lang'

  java_alias :JString, :String

  def test_java_proxy_object_equivalence
    room1 = Room.new("Bedroom")
    room2 = Room.new("Bedroom")
    room3 = Room.new("Bathroom")

    assert(room1 == room2);
    assert(room1 == room2.java_object);
    assert(room1.java_object == room2.java_object)
    assert(room1.java_object == room2)

    assert(room1 != room3)
    assert(room1 != room3.java_object)
    assert(room1.java_object != room3.java_object)
    assert(room1.java_object != room3)
    assert(room1.java_object != "Bedroom")

    assert("Bedroom" == room1.to_s)
    assert(room1.to_s == "Bedroom")

    assert(room1.equal?(room1))
    assert(!room1.equal?(room2))

    assert(JString.new("Bedroom").hashCode() == room1.hash())
    assert(JString.new("Bathroom").hashCode() == room3.hash())
    assert(room1.hash() != room3.hash())
  end

  def test_synchronized_method_available
    # FIXME: this doesn't actually test that we're successfully synchronizing
    obj = java.lang.Object.new
    result = nil
    assert_nothing_raised { result = obj.synchronized { "foo" } }
    assert_equal("foo", result)

    begin
      obj.wait 1
    rescue java.lang.IllegalMonitorStateException => e
      assert e
    else
      fail "java.lang.IllegalMonitorStateException was not thrown"
    end

    assert_nothing_raised { obj.synchronized { obj.wait 1 } }
  end

  def test_java_interface_impl_with_block
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute(Java::JavaLang::Runnable.impl { ran = true })
    assert ran
  end

  def test_ruby_object_duck_typed_as_java_interface_when_passed_to_method
    runnable = Object.new
    def runnable.run; @ran ||= true; end
    def runnable.ran; @ran; end
    SimpleExecutor::WrappedByMethodCall.new.execute(runnable)
    assert runnable.ran
  end

  def test_ruby_object_duck_typed_as_java_interface_when_passed_to_ctor
    runnable = Object.new
    def runnable.run; @ran ||= true; end
    def runnable.ran; @ran; end
    SimpleExecutor::WrappedByConstructor.new(runnable).execute
    assert runnable.ran
  end

  def test_ruby_proc_passed_to_ctor_as_last_argument
    ran = false
    SimpleExecutor::WrappedByConstructor.new(proc { ran = true }).execute
    assert ran
  end

  def test_ruby_proc_duck_typed_as_runnable
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute(proc { ran = true })
    assert ran
  end

  def test_ruby_proc_duck_typed_as_runnable_last_argument
    ran = 0
    SimpleExecutor::MultipleArguments.new.execute(2, proc { ran += 1 })
    assert_equal 2, ran
  end

  def test_ruby_block_duck_typed_as_runnable
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute { ran = true }
    assert ran
  end

  def test_ruby_block_duck_typed_as_runnable_last_argument
    ran = 0
    SimpleExecutor::MultipleArguments.new.execute(3) { ran += 1 }
    assert_equal 3, ran
  end

  def test_ruby_block_with_args_as_interface
    file = java.io.File.new(".")
    listing = file.list {|file,str| !!(str =~ /\./) }
    assert listing.size >= 0
  end

  class ExtendedClass < org.jruby.test.Abstract
    def protected_method
      "Ruby overrides java!"
    end
  end

  def test_overriding_protected_method
    a = ExtendedClass.new
    begin
      assert_equal "Ruby overrides java!", a.call_protected
    rescue Exception => e
      flunk "Exception raised: #{e}"
    end
  end

  def test_map_interface_to_array
    hash = {"one"=>"two","three"=>"four"}
    map = java.util.HashMap.new(hash)
    assert_equal hash.to_a.sort, map.to_a.sort
  end

  def test_java_object_wrapper
    wrapped1 = Java::JavaObject.wrap object = java.lang.StringBuilder.new
    assert wrapped1.is_a? Java::JavaObject
    assert_equal object, wrapped1

    wrapped2 = Java::JavaObject.wrap java.lang.StringBuilder.new
    assert_not_equal object, wrapped2
    assert ! wrapped1.equal?(wrapped2)

    wrapped3 = Java::JavaObject.wrap object
    assert_equal wrapped1, wrapped3
    assert wrapped1.equal?(wrapped3)

    cal1 = java.util.Calendar.getInstance
    cal1.setTime Java::JavaUtil::Date.new 0
    cal2 = java.util.Calendar.getInstance
    cal2.setTime Java::JavaUtil::Date.new 0

    assert ! cal1.equal?(cal2)

    wrapped1 = Java::JavaObject.wrap cal1
    wrapped2 = Java::JavaObject.wrap cal2
    assert wrapped2 == wrapped1
    assert wrapped1.eql? wrapped2
    assert ! wrapped1.equal?(wrapped2)
  end

end

