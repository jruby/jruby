require 'java'
require 'test/unit'

class TestJavaExtension < Test::Unit::TestCase
  import org.jruby.test.Worker
  import org.jruby.test.Abstract

  class TestParent < org.jruby.test.Parent
    attr_accessor :result
    def run(a)
      @result = "TEST PARENT: #{a}"
    end
  end

  def test_overriding_method_in_java_superclass
    w = Worker.new
    p = TestParent.new
    w.run_parent(p)
    assert_equal "TEST PARENT: WORKER", p.result
  end

  import java.util.HashMap
  import java.util.ArrayList
  import java.util.HashSet
  import java.lang.Short

  def test_set
    set = HashSet.new
    set.add(1)
    set.add(2)

    newSet = []
    set.each {|x| newSet << x }

    assert newSet.include?(1)
    assert newSet.include?(2)
  end

  def test_comparable
    one = Short.new(1)
    two = Short.new(2)
    three = Short.new(3)
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

    assert([1, 2, 3], a.sort)
		assert([1, 2], a[2...3])
		assert([3, 1], a[0, 2])
    assert([1], a.select {|e| e >= 1 })
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

    roomArray = Room[1].new
    roomArray[0] = room1
    assert_equal(room1, roomArray[0])
    assert_equal(1, roomArray.length)
  end

  def test_synchronized_method_available
    # FIXME: this doesn't actually test that we're successfully synchronizing
    obj = java.lang.Object.new
    result = nil
    assert_nothing_raised {
        result = obj.synchronized { "foo" }
    }
    assert_equal("foo", result)
    assert_raises(NativeException) {
        obj.wait 1
    }
    assert_nothing_raised {
        obj.synchronized { obj.wait 1 }
    }
  end

  
  def test_java_interface_impl_with_block
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute(Runnable.impl {ran = true})
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
    SimpleExecutor::WrappedByConstructor.new(proc {ran = true}).execute
    assert ran
  end

  def test_ruby_proc_duck_typed_as_runnable
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute(proc {ran = true})
    assert ran
  end

  def test_ruby_proc_duck_typed_as_runnable_last_argument
    ran = 0
    SimpleExecutor::MultipleArguments.new.execute(2, proc {ran += 1})
    assert_equal 2, ran
  end

  def test_ruby_block_duck_typed_as_runnable
    ran = false
    SimpleExecutor::WrappedByMethodCall.new.execute { ran = true }
    assert ran
  end

  def test_ruby_block_duck_typed_as_runnable_last_argument
    ran = 0
    SimpleExecutor::MultipleArguments.new.execute(2) {ran += 1}
    assert_equal 2, ran
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
      flunk "Exception raised: #{$!}"
    end
  end
  
  def test_map_interface_to_array
    hash = {"one"=>"two","three"=>"four"}
    map = java.util.HashMap.new(hash)
    assert_equal hash.to_a.sort, map.to_a.sort
  end
end

