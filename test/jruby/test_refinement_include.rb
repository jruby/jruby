require 'test/unit'

module Xorcist
  module_function
  def to_string(x)
    x.to_s
  end

  module StringMethods
    def to_string
      Xorcist.to_string(self)
    end
  end

  module Refinements
    refine Integer do
      include Xorcist::StringMethods
    end
  end
end

class TestRefinementInclude < Test::Unit::TestCase
  using Xorcist::Refinements

  def test_include
    assert_equal("123", 123.to_string)
  end
end