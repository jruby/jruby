require 'test/unit'
require 'java'

class TestPrimitiveToJava < Test::Unit::TestCase
  def test_primitive_conversion
    t = Time.now
    date = t.to_java(java.util.Date)

    assert_equal(t.to_i, date.time / 1000, "Ruby time #{t} not converted to java date correctly: #{date}")
  end
end
