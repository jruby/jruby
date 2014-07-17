require 'test/unit'

# See JRUBY3036
module IncludedTestCaseModule; end

class Top
  def self.top; true; end
end

class Next < Top
  include IncludedTestCaseModule
end

class TestIncludedInObjectSpace < Test::Unit::TestCase
  def test_included_does_not_hit_each_class
    ObjectSpace.each_object(Class) do |cls|
      if cls < Top
        assert cls.top
      end
    end
  end
end