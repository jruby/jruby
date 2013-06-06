require 'test/unit'

class TestTimeTZ < Test::Unit::TestCase
  def test_tz_without_minutes
    begin
      old_tz, ENV['TZ'] = ENV['TZ'], 'JST-9'
      assert_nothing_raised {
        time = Time.at(86400)
        assert_equal '1970-01-02T09:00:00+0900', time.strftime('%FT%T%z')
      }
    ensure
      ENV['TZ'] = old_tz
    end
  end

  def test_tz_with_minutes
    begin
      old_tz, ENV['TZ'] = ENV['TZ'], 'UTC+5:45'
      assert_nothing_raised {
        time = Time.at(86400)
        assert_equal '1970-01-01T18:15:00-0545', time.strftime('%FT%T%z')
      }
    ensure
      ENV['TZ'] = old_tz
    end
  end
end
