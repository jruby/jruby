require 'test/unit'

class TestBackref < Test::Unit::TestCase
  # JRUBY-1944
  def test_postmatch_in_scan
    "ab".scan(/a/) do |v|
      v =~ /z/
    end
    assert_equal 'b', $'
  end
end

