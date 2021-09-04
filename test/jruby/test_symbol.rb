require 'test/unit'

class TestSymbol < Test::Unit::TestCase

  def setup
    @gc_stress = GC.stress
  end

  def teardown
    GC.stress = @gc_stress
  end

  # moved from mri/ruby/test_symbol.rb

  def test_to_proc_yield
    #assert_ruby_status([], <<-"end;", timeout: 5.0)
      GC.stress = true
      assert_same true, true.tap(&:itself)
    #end;
  end

  def test_to_proc_new_proc
    #assert_ruby_status([], <<-"end;", timeout: 5.0)
      GC.stress = true
      assert_equal 2, 2.times { Proc.new(&:itself) }
    #end;
  end

  def test_to_proc_no_method
    #assert_separately([], <<-"end;", timeout: 5.0)
      bug11566 = '[ruby-core:70980] [Bug #11566]'
      assert_raise(NoMethodError, bug11566) { Proc.new(&:foo).(1) }
      assert_raise(NoMethodError, bug11566) { :foo.to_proc.(1) }
    #end;
  end

end