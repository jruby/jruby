# encoding: UTF-8
require 'test/unit'

class TestString < Test::Unit::TestCase

  # JRUBY-4987
  def test_paragraph
    # raises ArrayIndexOutOfBoundsException in 1.5.1
    assert_equal ["foo\n"], "foo\n".lines('').to_a
  end

  # Test fix for JRUBY-1215
  def test_invalid_float_from_string
    for string in [
      "1a",
      "a1",
      "1.0a",
      "10a",
      "10.1a",
      "0.10a",
      "1.1e1a",
      "1.1e10a",
      "\3 1",
      "1 \3",
    ]
      assert_raises(ArgumentError) { Float(string) }
    end
  end

  # Test fix for JRUBY-1215-related (unreported) bug
  def test_invalid_integer_from_string
    for string in [
      "a1",
      "1a",
      "10a",
      "\3 1",
      "1 \3",
    ]
      assert_raises(ArgumentError) { Integer(string) }
    end
  end

  def test_scan_string_pattern_match
    str = 'it_aj-ty_i-ty_it'
    str.sub!(/hello/, '')
    str.gsub!(/\-|_/, '-')
    assert_equal ['-ty-', '-ty-'], str.scan(pat = '-ty-')
    pat[2] = 'i'
    $~.inspect # failed with a NPE or might have recycled previous $~ pattern
    assert_equal /\-ty\-/, $~.regexp
    assert_equal 1, $~.size
    assert_equal str, $~.string
    assert $~.string.frozen?
  end

  def test_regexp_match
    ''.sub!(/foo/, '')
    # assert ! $~.nil?
    /bar/.match(nil)
    assert $~.nil?
  end

  def test_regexp_source_string
    regexp = Regexp.new(str = 'StrinG')
    assert regexp.eql?(/StrinG/)
    str[0] = 's'
    assert_equal 'StrinG', regexp.source
    regexp.source.replace ''
    assert_equal 'StrinG', regexp.source
    assert_equal /strinG/, regexp = Regexp.new(str)
    assert_equal 'strinG', regexp.source
    assert_equal 'strinG', /strinG/.source
    str.sub!('G', 'g')
    assert_equal /string/, regexp = Regexp.new(str)
    assert_equal 'string', regexp.source
    regexp.source.gsub!('s', 'z')
    assert_equal 'string', regexp.source
    assert_equal 'string', Regexp.new('string').source
  end

  EOL = "\r\n"

  def test_sub_utf8
    do_sub "a" + EOL + EOL + "a", 6, 3, 1  # 1byte + 2byte + 2byte + 1byte
    do_sub "a" + EOL + EOL + "あ", 6, 3, 1
    do_sub "あ" + EOL + EOL + "a", 6, 3, 1
    do_sub "あ" + EOL + EOL + "あ", 6, 3, 1
  end

  def test_count
    assert_equal(1, "abc\u{3042 3044 3046}".count("\u3042"))
    assert_equal(1, "abc\u{3042 3044 3046}".count("\u3044"))
    assert_equal(2, "abc\u{3042 3044 3046}".count("abc\u3044", 'bc'))
    assert_equal(0, "abc\u{3042 3044 3046}".count("\u3042", "\u3044", "\u3046"))
    assert_equal(1, "abc\u{3042 3044 3046}".count("c\u3044\u3042", "\u3042\u3042\u3044", "\u3042"))
    assert_equal(2, "abc\u{3042 3044 3046}".count("^\u3042", "^\u3044", "^\u3046", "^c"))
  end

  private

  def do_sub buf, e1, e2, e3
    assert_equal e1, buf.size

    head = ''

    #from cgi.rb..
    buf = buf.sub(/\A((?:.|\n)*?#{EOL})#{EOL}/n) do
      head = $1.dup
      ""
    end
    # ..cgi.rb

    assert_equal e2,  head.size
    assert_equal e3,  buf.size
  end

  public

  def test_try_squeeze
    ' '.squeeze
    try ' ', :squeeze # ArrayIndexOutOfBoundsException
  end

  private

  def try(obj, *a, &b) # ~ AS 4.2
    if a.empty? && block_given?
      if b.arity == 0
        obj.instance_eval(&b)
      else
        yield obj
      end
    else
      obj.public_send(*a, &b)
    end
  end

end
