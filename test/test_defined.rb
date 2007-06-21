require 'test/unit'

TEST_TOPLEVEL_CONST = true

class TestDefined < Test::Unit::TestCase
  class Sample
    class << self
      def set_the_var(value)
        @@some_var = value
      end
      def the_var_inited?
        defined? @@some_var
      end
    end
  end
  
  def test_defined_class_var
    assert !Sample.the_var_inited?
    Sample.set_the_var('something')
    assert Sample.the_var_inited?
  end
  
  def test_toplevel_const_defined
    assert_equal "constant", defined?(::TEST_TOPLEVEL_CONST)
  end
  
  def test_inner_const_defined
    assert_equal "constant", defined?(TestDefined::Sample)
  end
end
