require 'test/unit'

#
# FIXME: This tests should be move to Rubyspecs as soon as specs run with jruby 1.9 compatibility option
#

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
    io = IO.new(2, 'w', {:internal_encoding => 'iso-8859-1'})
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_encoding_as_option
    io = IO.new(2, 'w', {:encoding => 'UTF-8:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_encoding_option_ignored
    io = IO.new(2, 'w', {:external_encoding => 'UTF-8', :encoding => 'iso-8859-1:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s

    io = IO.new(2, 'w', {:internal_encoding => 'iso-8859-1', :encoding => 'iso-8859-1:iso-8859-1'})
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_mode_as_option
    io = IO.new(2, {:mode => 'w:UTF-8:iso-8859-1'})
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_io_should_ignore_internal_encoding
    io = IO.new(2, 'w:UTF-8:UTF-8')
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal nil, io.internal_encoding
  end

  def test_binmode?
    io = IO.new(2, 'wb')
    assert_equal true, io.binmode?
  end

  def test_new_file_with_encoding_in_mode
    io = File.new(__FILE__, 'r:UTF-8:iso-8859-1')
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_new_file_with_encoding_as_option
    io = File.new(__FILE__, 'r', :encoding => 'UTF-8:iso-8859-1')
    assert_equal 'UTF-8', io.external_encoding.to_s
    assert_equal 'ISO-8859-1', io.internal_encoding.to_s
  end

  def test_new_file_with_default_external_encoding
    io = File.new(__FILE__)
    assert_equal 'UTF-8', io.external_encoding.to_s
  end
end
