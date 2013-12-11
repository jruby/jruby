require 'test/unit'
require 'pathname'

class TestPathnameDup < Test::Unit::TestCase
  def test_dup
    p = Pathname.new("some/path")
    assert_equal "some/path", p.dup.to_s
  end
end
