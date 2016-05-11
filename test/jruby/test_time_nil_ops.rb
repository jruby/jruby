require 'test/unit'
class TestTimeNilOps < Test::Unit::TestCase
  def test_minus
    begin
      Time.now - ()
    rescue TypeError=>x
      assert x
      assert_equal "no implicit conversion to rational from nil", x.message
    end
  end
  def test_plus
    begin
      Time.now + ()
    rescue TypeError=>x
      assert x
      assert_equal "no implicit conversion to rational from nil", x.message
    end
  end
  def test_times
    t = Time.now
    begin
      _ = t * ()
      fail "bleh"
    rescue NoMethodError=>x
      assert x
      assert_equal "undefined method `*' for #{t}:Time", x.message
    end
  end
  def test_div
    t = Time.now
    begin
      _ = t / ()
      fail "bleh"
    rescue NoMethodError=>x
      assert x
      assert_equal "undefined method `/' for #{t}:Time", x.message
    end
  end

end

