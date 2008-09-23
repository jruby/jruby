require 'test/unit'
require 'jruby/core_ext'

class TestJrubyCoreExt < Test::Unit::TestCase
  def test_subclasses
    superclass = Class.new
    sub1 = Class.new(superclass)
    sub2 = Class.new(superclass)
    assert_equal([sub1, sub2].sort, superclass.subclasses.sort)
  end
end