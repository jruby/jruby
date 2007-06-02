require 'test/unit'
require 'java'

class TestPrimitiveToJava < Test::Unit::TestCase
  def test_primitive_conversion
    t = Time.now

    dc = org.jruby.javasupport.test.DateHolder.new
    sdf = java.text.SimpleDateFormat.new("EEE MMM dd HH:mm:ss z yyyy", java.util.Locale::ENGLISH)
    dc.date = t

    assert(t.to_s.eql?(sdf.format(dc.date)), "Ruby time #{t} not converted to java date correctly: #{sdf.format(dc.date)}")
  end
end
