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

  def test_julian
    date = Date.new(2000, 1, 1)
    assert_equal true, date.gregorian?
    assert_equal false, date.julian?

    date = Date.new(1000, 1, 1)
    assert_equal true, date.julian?
  end

end
