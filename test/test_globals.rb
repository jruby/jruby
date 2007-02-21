require 'test/unit'

class TestGlobals < Test::Unit::TestCase
  def check_global_variable
    assert_equal "a global variable.", $output
  end
  def test_global_scope
    $output = "a global variable."
    check_global_variable
  end

  def test_global_alias
    $bar = 5
    alias $foo $bar
    assert_equal 5, $foo

    $bar = 10
    assert_equal 10, $foo

    $foo = 5
    assert_equal 5, $bar
  end

  # Make sure $@ == nil if $! is not nil and $!.backtrace is an array
  class MyWobblyError < StandardError
    def initialize(backtrace) ; @backtrace = backtrace ; end
    def backtrace ; @backtrace ; end
  end

  def test_global_error_vars
    begin
      raise MyWobblyError.new(nil)
    rescue
      assert_equal nil, $@
    end

    begin
      raise MyWobblyError.new("abc")
    rescue
      assert_equal nil, $@
    end

    #inconsistent with set_backtrace but it's what MRI does
    begin
      raise MyWobblyError.new(["abc", 123])
    rescue
      assert $@ != nil
    end

    begin
      raise MyWobblyError.new(["abc", "123"])
    rescue
      assert $@ != nil
    end

    begin
      raise MyWobblyError.new([])
    rescue
      assert $@ != nil
    end
  end
  
  def test_program_name
    assert_equal $0, $PROGRAM_NAME
    old, $0 = $0, "abc"
    $0 = old
  end
end
