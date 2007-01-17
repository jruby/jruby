require 'test/unit'
require 'java'

class TestPrimitiveToJava < Test::Unit::TestCase
  def test_primitive_conversion
    t = Time.now

    dc = org.jruby.javasupport.test.DateHolder.new
    dc.date = t

    assert(t.to_s.eql?(dc.date.to_s), "Ruby time #{t} not converted to java date correctly: #{dc.date}")
  end
end