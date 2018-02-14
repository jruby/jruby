require 'test/unit'

class TestDate < Test::Unit::TestCase

  def setup
    require 'date'
  end

  def test_years_around_0 # Joda Time vs (Ruby) Date
    (-2..2).each do |year|
      assert_equal year, Date.new(year).year
      assert_equal year, DateTime.new(year).year
      [Date::GREGORIAN, Date::ITALY, Date::ENGLAND, Date::JULIAN].each do |sg|
        assert_equal year, Date.new(year, 1, 1, sg).year
        assert_equal year, DateTime.new(year, 1, 1, 0, 0, 0, 0, sg).year
      end
    end
  end

  def test_new_civil
    date = Date.new
    assert_equal -4712, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new -4712
    assert_equal -4712, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new 0
    assert_equal 0, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new 1
    assert_equal 1, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new -1
    assert_equal -1, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new -2, 10
    assert_equal -2, date.year
    assert_equal 10, date.month
    assert_equal 1, date.day

    date = Date.new 1492, -3, -3
    assert_equal 1492, date.year
    assert_equal 10, date.month
    assert_equal 29, date.day

    date = Date.new -6, -6, -6
    assert_equal -6, date.year
    assert_equal 7, date.month
    assert_equal 26, date.day

    date = Date.new -9, -11, -20
    assert_equal -9, date.year
    assert_equal 2, date.month
    assert_equal 9, date.day

    date = Date.new -9999, -12, -31
    assert_equal -9999, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.new -6, -6, -6, Date::ITALY
    assert_equal -6, date.year
    assert_equal 7, date.month
    assert_equal 26, date.day

    date = Date.new -6, -6, -6, Date::GREGORIAN
    assert_equal -6, date.year
    assert_equal 7, date.month
    assert_equal 26, date.day
  end

  def test_date_time_methods
    date = Date.new(1, 2, 3)
    assert_equal 1, date.year
    assert_equal 2, date.month
    assert_equal 3, date.day
    assert_equal 34, date.yday
    assert_equal 4, date.wday
    assert_equal 1, date.cwyear
    assert_equal 0, date.send(:hour)
    assert_equal 0, date.send(:min)
    assert_equal 0, date.send(:second)

    date = Date.new(2001, 3, -1)
    assert_equal 2001, date.year
    assert_equal 3, date.month
    assert_equal 31, date.day

    date = Date.new(1001, 2, -10)
    assert_equal 1001, date.year
    assert_equal 2, date.month
    assert_equal 19, date.day

    begin
      Date.new(1900, 1, 0)
      fail 'expected to fail'
    rescue ArgumentError => e
      assert_equal 'invalid date', e.message
    end
  end

  def test_new_start
    date = Date.new(2000, 1, 1)

    new_date = date.italy
    assert_equal 2000, new_date.year
    assert_equal 1, new_date.month
    assert_equal 1, new_date.day

    new_date = date.england
    assert_equal 2000, new_date.year
    assert_equal 1, new_date.month
    assert_equal 1, new_date.day

    new_date = date.julian
    assert_equal 1999, new_date.year
    assert_equal 12, new_date.month
    assert_equal 19, new_date.day

    new_date = date.new_start
    assert_equal 2000, new_date.year
    assert_equal 1, new_date.month
    assert_equal 1, new_date.day
    assert_equal Date::ITALY, new_date.start

    new_date = date.new_start(Date::JULIAN)
    assert_equal 1999, new_date.year
    assert_equal 12, new_date.month
    assert_equal 19, new_date.day
    assert_equal Date::JULIAN, new_date.start

    new_date = new_date.new_start(Date::GREGORIAN)
    assert_equal 2000, new_date.year
    assert_equal 1, new_date.month
    assert_equal 1, new_date.day
    assert_equal Date::GREGORIAN, new_date.start

    # new_date = date.new_start(1)
    # assert_equal 1999, new_date.year
    # assert_equal 12, new_date.month
    # assert_equal 19, new_date.day
  end

  def test_new_offset
    date = Date.new(2000, 1, 1)

    assert_equal '+00:00', date.send(:zone)
    new_date = date.send :new_offset, 'nst'
    assert_equal 1999, new_date.year
    assert_equal 12, new_date.month
    assert_equal 31, new_date.day
    assert_equal '-03:30', new_date.send(:zone)
    assert_equal 20, new_date.send(:hour)
    assert_equal 30, new_date.send(:min)
    assert_equal 0, new_date.send(:sec)

    new_date = date.send :new_offset, Rational(7, 24)
    assert_equal 2000, new_date.year
    assert_equal 1, new_date.month
    assert_equal 1, new_date.day
    assert_equal '+07:00', new_date.send(:zone)
    assert_equal 7, new_date.send(:hour)
    assert_equal 0, new_date.send(:min)
    assert_equal 0, new_date.send(:sec)
  end

  def test_julian
    date = Date.new(2000, 1, 1)
    assert_equal true, date.gregorian?
    assert_equal false, date.julian?

    date = Date.new(1000, 1, 1)
    assert_equal true, date.julian?
  end

  def test_to_s_strftime
    date = Date.new(2000, 1, 1)
    assert_equal '2000-01-01', date.to_s
    assert_equal '2000-01-01', date.strftime

    date = Date.new(-111, 10, 11)
    assert_equal '-0111-10-11', date.to_s

    date = Date.new(-1111, 11)
    assert_equal '-1111-11-01', date.strftime
  end

  def test_inspect
    date = Date.new(2000, 12, 31)
    assert_equal '#<Date: 2000-12-31 ((2451910j,0s,0n),+0s,2299161j)>', date.inspect

    date = Date.new(-2, 2)
    assert_equal '#<Date: -0002-02-01 ((1720359j,0s,0n),+0s,2299161j)>', date.inspect

    date = Date.today
    assert_match /#<Date: 20\d\d\-\d\d\-\d\d \(\(\d+j,0s,0n\),\+0s,2299161j\)>/, date.inspect
  end

  def test_shift
    date = Date.new(2000, 1, 1)
    new_date = date >> 11
    assert_equal 12, new_date.month
    assert_equal 1, new_date.day
    assert_equal 1, (date >> 12).day
    assert_equal 1, (date >> 12).month
    assert_equal 2001, (date >> 12).year

    assert_raise(TypeError) { date >> "foo" }
  end

  def test_plus
    date = Date.new(2000, 1, 1)
    new_date = date + 31
    assert_equal 2, new_date.month
    assert_equal 1, new_date.day
    assert_equal 2000, new_date.year

    new_date = date + 10.5
    assert_equal 1, new_date.month
    assert_equal 11, new_date.day
    assert_equal 2000, new_date.year

    assert_raise(TypeError) { date + Object.new }
  end

  def test_day_fraction
    date = Date.new(2000, 1, 1)
    assert_equal 0, date.day_fraction

    date = DateTime.new(2000, 1, 10)
    assert date.day_fraction.eql? Rational(0, 1)

    date = DateTime.new(2000, 10, 10, 12, 24, 36, 48)
    assert_equal Rational(1241, 2400), date.day_fraction
  end

  def test_civil_invalid_sg
    date = Date.civil(2000, 1, 1, sg = 2426355 + 1_000_000)
    assert_equal 2000, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day

    date = Date.civil(2000, 1, 1, sg = Date::JULIAN)
    assert_equal 2000, date.year
    assert_equal 1, date.month
    assert_equal 1, date.day
  end

  def test_now_zone
    begin
      old_tz, ENV['TZ'] = ENV['TZ'], 'UTC+9:45'
      time = DateTime.now
      strf = time.strftime('%FT%T%z')
      assert strf.end_with?('-0945'), "invalid zone for: #{strf} (expected '-0945')"
      date = Time.now.strftime('%F')
      assert strf.start_with?(date), "DateTime '#{strf}' does not start with: '#{date}'"
    ensure
      ENV['TZ'] = old_tz
    end
  end

  def test_now_local
    time = DateTime.now
    zone = Time.new.strftime('%:z')
    assert time.to_s.end_with?(zone), "invalid zone for: #{time.to_s} (expected '#{zone}')"
  end

end
