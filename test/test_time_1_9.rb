require 'test/unit'

class TestTime19 < Test::Unit::TestCase

  # JRUBY-4612: strftime does not support '-' output specifier
  def test_strftime
    time = Time.local(2001,1,1,14,01,42)
    assert_equal "1/1/1 2:1 PM", time.strftime("%-m/%-d/%-y %-I:%-M %p")

    time = Time.local(2001,1,1,14,10,42)
    assert_equal "1/1/1 2:10 PM", time.strftime("%-m/%-d/%-y %-I:%-M %p")
  end
end