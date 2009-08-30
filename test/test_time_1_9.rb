require 'test/unit'

class TestTime19 < Test::Unit::TestCase
  def setup
    @time = Time.local(2001, 2, 3, 4, 5, 6)
  end

  def test_equal
    assert_equal(false, Time.now == nil)
  end

  def test_format_upcase
    assert_equal('FEB', @time.strftime('%^h'))
  end

  def test_format_upcase_and_padding
    assert_equal('  FEB', @time.strftime('%^_5h'))
  end

  def test_format_upcase_and_zero_padding
    assert_equal('00FEB', @time.strftime('%0^5h'))
  end

  def test_format_zero_padding
    assert_equal('0004', @time.strftime('%04H'))
  end

  def test_no_padding
    assert_equal('FEB', @time.strftime('%0-^5h'))
  end

  def test_no_padding_at_all
    assert_equal('FEB', @time.strftime('%_-^5h'))
  end

  def test_format_just_date_modifiers
    assert_equal('FEBa', @time.strftime('%^ha'))
  end

  def test_format_strftime
    expected = {
      "%10h" => '       Feb',
      "%^10h" => '       FEB',
      "%_10h" => '       Feb',
      "%_010h" => '0000000Feb',
      "%0_10h" => '       Feb',
      "%0_-10h" => 'Feb',
      "%0-_10h" => 'Feb'
    }

    ["%10h","%^10h","%_10h","%_010h","%0_10h","%0_-10h","%0-_10h"].each do |f|
      assert_equal expected[f], @time.strftime(f)
    end
  end

end