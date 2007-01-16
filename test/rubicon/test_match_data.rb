require 'test/unit'


class TestMatchData < Test::Unit::TestCase

  def setup
    @m = /(.)(.)(\d+)(\d)/.match("THX1138.")
  end
  
  def test_AREF # '[]'
    assert_equal("HX1138",      @m[0])
    assert_equal(%w( H X ),     @m[1, 2])
    assert_equal(%w( H X 113 ), @m[1..3])
    assert_equal(%w( X 113 ),   @m[-3, 2])
  end

  def test_begin
    assert_equal(1, @m.begin(0))
    assert_equal(2, @m.begin(2))
  end

  def test_end
    assert_equal(7, @m.end(0))
    assert_equal(3, @m.end(2))
  end

  def test_length
    assert_equal(5, @m.length)
  end

  def test_offset
    assert_equal([1,7], @m.offset(0))
    assert_equal([6,7], @m.offset(4))
  end

  def test_post_match
    assert_equal(".", @m.post_match)
  end

  def test_pre_match
    assert_equal("T", @m.pre_match)
  end

  def test_size
    assert_equal(5, @m.size)
  end

  def test_string
    assert_equal("THX1138.", @m.string)
    assert(@m.string.frozen?)
  end

  def test_to_a
    assert_equal(%w( HX1138 H X 113 8 ), @m.to_a)
  end

  def test_to_s
    assert_equal("HX1138", @m.to_s)
  end

end
