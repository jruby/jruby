require 'test/unit'
class TestTimeNilOps < Test::Unit::TestCase
  def test_minus
    begin
      Time.now - ()
    rescue TypeError=>x
      assert x
      assert_equal "no implicit conversion to float from nil", x.message
    end
  end
  def test_plus
    begin
      Time.now + ()
    rescue TypeError=>x
      assert x
      assert_equal "no implicit conversion to float from nil", x.message
    end
  end
  def test_times
    t = Time.now
    begin
      t * ()
      fail "bleh"
    rescue NoMethodError=>x
      assert x
      assert_equal "undefined method `*' for #{t}:Time", x.message
    end
  end
  def test_div
    t = Time.now
    begin
      Time.now / ()
      fail "bleh"
    rescue NoMethodError=>x
      assert x
      assert_equal "undefined method `/' for #{t}:Time", x.message
    end
  end

end

