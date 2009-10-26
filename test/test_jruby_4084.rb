require 'test/unit'

class TestJruby4084 < Test::Unit::TestCase
  def test_jruby_4084
    result = ["1"[0..1], "1111111"[5..6]].map{|i| i.each_char.to_a.map{|i| i == "1"}}
    assert_equal [[true], [true, true]], result
  end
end

