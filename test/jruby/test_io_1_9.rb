require 'test/unit'
require 'stringio'

# Still used by test/org/jruby/embedMultipleScriptsRunner.java
class TestIO19 < Test::Unit::TestCase

  def setup
    @file = "TestIO_19_tmp"
    @stringio = StringIO.new 'abcde'
  end

  def teardown
    File.unlink @file rescue nil
  end

  # JRUBY-5436
  def test_open_with_dash_encoding
    filename = 'test.txt'
    io = File.new(filename, 'w+:US-ASCII:-')
    assert_nil io.internal_encoding
  ensure
    io.close
    File.unlink(filename)
  end

  def test_gets_limit
    File.open(@file, 'w') { |f| f.write 'abcde' }

    File.open(@file) do |f|
      assert_equal 'ab', f.gets(2)
    end
  end

  def test_gets_separator_limit
    File.open(@file, 'w') { |f| f.write 'abcde' }

    File.open(@file) do |f|
      assert_equal 'ab', f.gets('c', 2)
    end
  end

  def test_gets_nil_separator_limit
    File.open(@file, 'w') { |f| f.write 'abcde' }

    File.open(@file) do |f|
      assert_equal 'ab', f.gets(nil, 2)
    end
  end

  def test_stringio_gets_limit
    assert_equal 'ab', @stringio.gets(2)
  end

  def test_stringio_gets_separator_limit
    assert_equal 'ab', @stringio.gets('c', 2)
  end

  def test_stringio_gets_nil_separator_limit
    assert_equal 'ab', @stringio.gets(nil, 2)
  end

end
