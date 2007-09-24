require 'test/unit'
require 'rbconfig'

class TestKernel < Test::Unit::TestCase

  def test_stuff_seemingly_out_of_context
    assert !Kernel.eval("defined? some_unknown_variable")
    assert_equal nil, true && defined?(unknownConstant)

    # JRUBY-117 - to_a should be public
    assert_equal ["to_a"], Object.public_instance_methods.grep("to_a")

    # JRUBY-117 - remove_instance_variable should be private
    assert_equal ["remove_instance_variable"], Object.private_instance_methods.grep("remove_instance_variable")
  end


  def test_Array_should_return_empty_array_for_nil
    assert_equal [], Kernel.Array(nil)
  end

  class ToAryReturnsAnArray; def to_ary() [1] end; end
  class ToAryReturnsAnInteger; def to_ary() 2 end; end

  def test_Array_should_use_to_ary_and_make_sure_that_it_returns_either_array_or_nil
    assert_equal [1], Kernel.Array(ToAryReturnsAnArray.new)
    assert_raises(TypeError) { Kernel.Array(ToAryReturnsAnInteger.new) }    
  end

  class BothToAryAndToADefined; def to_ary() [3] end; def to_a() raise "to_a() should not be called" end; end
  class OnlyToADefined; def to_a() [4] end; end
  def test_Array_should_fallback_on_to_a_if_to_ary_is_not_defined
    assert_equal [3], Kernel.Array(BothToAryAndToADefined.new)
    assert_equal [4], Kernel.Array(OnlyToADefined.new)
  end

  class NeitherToANorToAryDefined; end
  def test_Array_should_return_array_containing_argument_if_the_argument_has_neither_to_ary_nor_to_a
    assert_equal [1], Kernel.Array(1)
    assert_equal [:foo], Kernel.Array(:foo)
    obj = NeitherToANorToAryDefined.new 
    assert_equal [obj], Kernel.Array(obj)
  end

  class ToAryReturnsNil; def to_ary() nil end; end
  def test_Array_should_return_array_containing_arg_if_to_ary_returns_nil
    obj = ToAryReturnsNil.new
    assert_equal [obj], Kernel.Array(obj)
  end

  def test_Integer
    assert_equal 10, Kernel.Integer("0xA")
    assert_equal 8, Kernel.Integer("010")
    assert_equal 2, Kernel.Integer("0b10")
    assert_raises(ArgumentError) { Kernel.Integer("abc") }
    assert_raises(ArgumentError) { Kernel.Integer("x10") }
    assert_raises(ArgumentError) { Kernel.Integer("xxxx10000000000000000000000000000000000000000000000000000") }
  end

  def test_Float
    assert_equal 1.0, Kernel.Float("1")
    assert_equal 10.0, Kernel.Float("1e1")
    assert_raises(ArgumentError) { Kernel.Float("abc") }
    assert_raises(ArgumentError) { Kernel.Float("x10") }
    assert_raises(ArgumentError) { Kernel.Float("xxxx10000000000000000000000000000000000000000000000000000") }
  end

#  String
#  URI
#  `
#  abort
#  at_exit
#  binding
#  block_given?
  class CheckBlockGiven; def self.go() block_given? end; end
  def test_iterator?
    assert !(Kernel.block_given?)
    assert(CheckBlockGiven.go { true })
    assert(!CheckBlockGiven.go)
    assert(!CheckBlockGiven.go(&Proc.new))
  end

#  callcc
#  caller

  def test_catch_throw
    been_where_it_shouldnt = false

    catch :fred do throw :fred; fail("shouldn't get here") end
    assert(!been_where_it_shouldnt)

    assert_raises (NameError) do
      catch :fred do throw :wilma end
    end
  end

  def test_throw_should_bubble_up_to_the_right_catch
    been_at_fred2 = false
    catch :fred1 do
      catch :fred2 do
        catch :fred3 do
          catch :fred4 do
            throw :fred2
            fail("should have jumped to fred2 catch")
          end
          fail("should have jumped to fred2 catch")
        end
      end
      been_at_fred2 = true
    end
    assert been_at_fred2 
  end

  def test_invalid_throw_after_inner_catch_should_unwind_the_stack_all_the_way_to_the_top
    been_at_fred1 = false
    been_at_fred2 = false
    assert_raises(NameError) do
      catch :fred1 do
        catch :fred2 do
          catch :fred3 do
            throw :fred2
            test_fail("should have jumped to after fred2 catch")
          end
          test_fail("should have jumped to after fred2 catch")
        end
        been_at_fred2 = true
        throw :wilma
      end
      been_at_fred1 = true
    end
    assert been_at_fred2
    assert !been_at_fred1
  end

  def throw_outside_of_any_block_shoudl_raise_an_error
    assert_raises (NameError) { throw :wilma }
  end

  def test_catch_stack_should_be_cleaned_up
    assert_raises(NameError) do
      catch :fred1 do
        catch :fred2 do
          catch :fred3 do
          end
        end
      end
      throw :fred2
      fail "catch stack should have been cleaned up"
    end
  end
  
#  chomp
#  chomp!
#  chop
#  chop!


  #  eval
  def test_eval_should_use_local_variable_defined_in_parent_scope
    x = 1
    assert_equal 1, Kernel.eval('x')
    Kernel.eval("x = 2")
    assert_equal 2, x
  end

  def test_eval_should_not_bring_local_variables_defined_in_its_input_to_parent_scope
    existing_variable = 0
    another_variable = 1
    assert !(defined? new_variable)

    Kernel.eval("new_variable = another_variable = existing_variable")

    assert_equal 0, existing_variable 
    assert_equal 0, another_variable 
    assert !(defined? new_variable)
  end

#  exec
  # should not really work, since there is no fork

#  exit
#  exit!
#  fail
#  fork

  def test_format
    assert_equal "Hello, world", Kernel.format("Hello, %s", "world") 
    assert_raises(TypeError) { Kernel.format("%01.3f", nil) }
  end

#  getc
#  gets
#  global_variables
#  gsub
#  gsub!

  class CheckIterator; def self.go() iterator? end; end
  def test_iterator?
    assert !(Kernel.iterator?)
    assert(CheckIterator.go { true })
    assert(!CheckIterator.go)
  end

#  lambda
#  load
  class ToStrPointToRequireTarget
    attr_accessor :target_required
    def to_str() "#{File.dirname(__FILE__)}/require_target.rb" end
  end
  def test_load_should_call_to_str_on_arg
    $target_required = false
    Kernel.load ToStrPointToRequireTarget.new
    assert $target_required

    assert_raises(TypeError) { Kernel.load Object.new }
  end

  def test_local_variables
    if Config::CONFIG['ruby_install_name'] == 'jruby'
      a = lambda do
        assert_equal %w(a b), local_variables
        b = 1
        assert_equal %w(a b), local_variables
      end
      a.call
    else
      # This behaves like MRI 1.9, and fails on 1.8, so, skip it
    end
  end

#  loop
#  method_missing
#  open
#  p
#  print

  def test_printf_should_raise_argument_error_when_asked_to_format_string_as_a_number
    assert_raises(ArgumentError) { Kernel.printf "%d", 's' }
  end

#  proc
#  putc
#  puts

  def test_raise
    assert_raises(RuntimeError) { Kernel.raise }
    assert_raises(StandardError) { Kernel.raise(StandardError) }
    assert_raises(TypeError) { Kernel.raise(TypeError.new) }

    begin
      Kernel.raise(StandardError, 'oops')
      fail "shouldn't get here!"
    rescue => e1
      assert e1.is_a?(StandardError)
      assert_equal 'oops', e1.message
    end

    begin
      Kernel.raise('oops')
      fail "shouldn't get here!"
    rescue => e2
      assert e2.is_a?(StandardError)
      assert_equal 'oops', e2.message
    end
  end

#  rand
#  readline
#  readlines
#  scan
#  select
#  set_trace_func


  def test_sleep
    assert_raises(ArgumentError) { sleep(-10) }
    # FIXME: below is true for MRI, but not for JRuby 
    # assert_raises(TypeError) { sleep "foo" }
    assert_equal 0, sleep(0)
    t1 = Time.now
    sleep(0.1)
    t2 = Time.now
    # this should cover systems with 10 msec clock resolution
    assert t2 >= t1 + 0.08
  end

  def test_sprintf
    assert_equal 'Hello', Kernel.sprintf('Hello')
  end
  
  def test_srand
    Kernel.srand
    Kernel.srand(0)
    Kernel.srand(:foo)
    assert_raises(TypeError) { Kernel.srand([:foo]) }
  end

#  sub
#  sub!
#  syscall

  def test_system
    assert_equal false, system('non_existant_command')
    # TODO: test for other OSes (Windows, for example, wouldn't know what to do with /dev/null)
    if Config::CONFIG['target_os'] == 'linux'
      assert_equal true, system('echo > /dev/null')
    end
  end
  
#  test
#  trace_var
#  trap
#  untrace_var
#  warn

end

