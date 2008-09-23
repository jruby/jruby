require 'test/unit'
require 'jruby/core_ext'

class TestJrubyCoreExt < Test::Unit::TestCase
  def test_subclasses
    superclass = Class.new
    sub1 = Class.new(superclass)
    sub2 = Class.new(superclass)
    assert_equal([sub1.to_s, sub2.to_s].sort, superclass.subclasses.map{|c| c.to_s}.sort)
  end
end