require 'test/unit'

class TestIO19 < Test::Unit::TestCase

  def test_external_encoding_concated_to_mode
    io = IO.new(2, 'w:UTF-8')
    assert_equal 'UTF-8', io.external_encoding.to_s
  end

  def test_external_and_internal_encoding_concated_to_mode
    io = IO.new(2, 'w:UTF-8:iso-8859-1')
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_external_encoding_as_option
    io = IO.new(2, 'w', {:external_encoding => 'UTF-8'})
    assert_equal 'UTF-8', io.external_encoding.to_s
  end

  def test_internal_encoding_as_option
    io = IO.new(2, 'w', {:internal_encoding => 'UTF-8'})
    assert_equal 'UTF-8', io.internal_encoding.to_s
  end

  def test_encoding_as_option
    io = IO.new(2, 'w', {:encoding => 'UTF-8:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_encoding_option_ignored
    io = IO.new(2, 'w', {:external_encoding => 'UTF-8', :encoding => 'iso-8859-1:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s

    io = IO.new(2, 'w', {:internal_encoding => 'UTF-8', :encoding => 'iso-8859-1:iso-8859-1'})
    assert_equal 'UTF-8', io.internal_encoding.to_s
  end

  def test_mode_as_option
    io = IO.new(2, {:mode => 'w:UTF-8:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_binmode?
    io = IO.new(2, 'wb')
    assert_equal true, io.binmode?
  end
end
