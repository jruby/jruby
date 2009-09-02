require 'test/unit'

class TestEncoding < Test::Unit::TestCase


  def test_default_external_encoding
    Encoding.default_external = 'UTF-8'
    assert_equal 'UTF-8', Encoding.default_external.to_s
  end

  def test_default_internal_encoding
    Encoding.default_internal = 'ISO-8859-1'
    assert_equal 'ISO-8859-1', Encoding.default_internal.to_s
  end
end
