require 'test/unit'

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
  
  def test_defined
    assert !Sample.the_var_inited?
    Sample.set_the_var('something')
    assert Sample.the_var_inited?
  end
end
