require 'test/unit'

class TestIO19 < Test::Unit::TestCase

  # JRUBY-5436
  def test_open_with_dash_encoding
    filename = 'test.txt'
    io = File.new(filename, 'w+:US-ASCII:-')
    assert_nil io.internal_encoding
  ensure
    io.close
    File.unlink(filename)
  end
end
