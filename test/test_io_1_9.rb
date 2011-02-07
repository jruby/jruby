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

  # JRUBY-5400
  def test_ungec_with_string
    rd, wr = IO.pipe
    rd.binmode
    wr.binmode

    wr.write("foobar")
    c = rd.getc
    rd.ungetc(c)
    assert_equal 'foo', rd.readpartial(3)
    assert_equal 'bar', rd.readpartial(3)
  ensure
    rd.close unless rd.closed?
    wr.close unless wr.closed?
  end

  def test_ungec_with_invalid_characters
    rd, wr = IO.pipe
    rd.binmode
    wr.binmode

    wr.write("∂φ/∂x = gaîté")
    c = rd.getc
    rd.ungetc(c)
    assert_equal "\xC3\x88\x82", rd.readpartial(3)
    assert_equal "\xCF\x86/", rd.readpartial(3)
  ensure
    rd.close unless rd.closed?
    wr.close unless wr.closed?
  end

  def test_ungetc_ignores_empty_strings
    rd, wr = IO.pipe
    rd.binmode
    wr.binmode

    wr.write("foobar")
    c = rd.getc
    rd.ungetc('')
    assert_equal 'oob', rd.readpartial(3)
  end
end
