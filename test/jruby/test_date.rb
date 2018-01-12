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

end
