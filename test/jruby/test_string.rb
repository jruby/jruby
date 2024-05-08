# encoding: UTF-8
require 'test/unit'
require 'stringio'

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
    str = +'it_aj-ty_i-ty_it'
    str.sub!(/hello/, '')
    str.gsub!(/\-|_/, '-')
    assert_equal ['-ty-', '-ty-'], str.scan(pat = +'-ty-')
    pat[2] = 'i'
    $~.inspect # failed with a NPE or might have recycled previous $~ pattern
    assert_equal /\-ty\-/, $~.regexp
    assert_equal 1, $~.size
    assert_equal str, $~.string
    assert $~.string.frozen?
  end

  def test_regexp_match
    (+'').sub!(/foo/, '')
    # assert ! $~.nil?
    /bar/.match(nil)
    assert $~.nil?
  end

  def test_regexp_source_string
    regexp = Regexp.new(str = +'StrinG')
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

  # GH-5203
  def test_string_buffer_sharing_in_stringio
    zero = 0
    strio = StringIO.new "123\nabcdefghijklmnopqrstuvxyz\n#{zero}\n"
    ary = []; strio.each_line { |line| ary << line }
    strio.rewind
    strio.write('456')
    assert_equal ["123\n", "abcdefghijklmnopqrstuvxyz\n", "0\n"], ary
    assert_equal "456\nabcdefghijklmnopqrstuvxyz\n0\n", strio.string
  end

  # GH-5203
  def test_string_buffer_sharing_in_stringio_from_regex
    strio = StringIO.new
    strio << "<Region>hello</Region>"
    strio.rewind

    str = strio.read
    match = str.match("<Region>([a-zA-Z]+)</Region>")[1]

    strio.seek 0
    strio.truncate 0
    strio << '1234567890'

    assert_equal str, "<Region>hello</Region>"
    assert_equal match, "hello"
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

  public

  def test_scan_error
    string = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
    assert_equal [], 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz'.scan('d....r........')

    ('a'..'z').to_a.each do |c1|
      ('a'..'z').to_a.each do |c2|
        string.downcase.scan("#{c1}....#{c2}........") # does not blow with ArrayIndexOutOfBoundsException
      end
    end
  end

  def test_split_wchar_with_null_byte
    [
     Encoding::UTF_16BE, Encoding::UTF_16LE,
     Encoding::UTF_32BE, Encoding::UTF_32LE,
    ].each do |enc|
      s = "AA\0BB\0CC".encode(enc)
      assert_equal(["AA", "BB", "CC"].map {|c| c.encode(enc)},
                   s.split("\0".encode(enc)),
                   "with #{enc.name}")
    end
  end

  def test_split_two_code_unit_wchar
    [
     Encoding::UTF_16BE, Encoding::UTF_16LE,
     Encoding::UTF_32BE, Encoding::UTF_32LE,
    ].each do |enc|
      s = "ab\u{10437},\u{10437}de\u{10437}".encode(enc)
      assert_equal(["ab\u{10437}", "\u{10437}de\u{10437}"].map {|c| c.encode(enc)},
                   s.split(",".encode(enc)),
                   "with #{enc.name}")
    end
  end

  def test_split_wchar_with_two_code_unit_delimiter
    [
     Encoding::UTF_16BE, Encoding::UTF_16LE,
     Encoding::UTF_32BE, Encoding::UTF_32LE,
    ].each do |enc|
      s = "ab\u{10437},\u{10437}de\u{10437}".encode(enc)
      assert_equal(["ab", ",", "de"].map {|c| c.encode(enc)},
                   s.split("\u{10437}".encode(enc)),
                   "with #{enc.name}")
    end
  end

end

class TestStringPrintf < Test::Unit::TestCase

  ##### binary (%b) #####
  def test_binary
    assert_equal("101", "%b" % 5)
    assert_equal("101", "%b" % "5")
    assert_equal("1011010111100110001000001111010010000000000101", "%b" % 50000000000005)
    assert_equal("101111000001010000111111101001001110001001010000111010110011000100010111110110010101010110100000000000000000000000000000000000101", "%b" % 500000000000000000000000000000000000005)
    assert_equal(" 101", "% b" % 5)
    assert_equal("-101", "% b" % -5)
    assert_equal(" -101", "% 5b" % -5)
    assert_equal("101", "%1b" % 5)
    assert_equal("00101", "%.5b" % 5)
    assert_equal("00101", "%05b" % 5)
    #assert_equal("..1011", "%05b" % -5)
    assert_equal("..1011", "%5b" % -5)
    assert_equal("101", "%b" % 5.5)
    assert_equal("0b101", "%#b" % 5)
    assert_equal("0b..1011", "%#b" % -5)
    assert_equal("+101", "%+b" % 5)
    assert_equal("101  ", "%-5b" % 5)
    assert_raises(TypeError) { "%b" % {'A' => 1} }
    assert_raises(ArgumentError) { "%b" % "a" }
    assert_raises(TypeError) { "%b" % true }
    assert_raises(TypeError) { "%b" % [[1, 2]] }
  end

  ##### char (%c) #####
  def test_char
    assert_equal("A", "%c" % 65)
    assert_equal("ŭ", "%c" % 365)
    assert_raises(ArgumentError) {"%c" % -165}
    assert_equal("A", "% c" % 65)
    assert_equal("A", "%0c" % 65)
    assert_equal("A", "%.5c" % 65)
    assert_equal("A", "%#c" % 65)
    assert_equal("A", "%+c" % 65)
    assert_equal("    A", "%5c" % 65)
    assert_equal("    A", "%05c" % 65)
    assert_equal("A    ", "%-5c" % 65)
    assert_equal("A", "%c" % 65.8)
    assert_raises(TypeError) {"%c" % true}
    assert_raises(TypeError) {"%c" % nil}
    assert_raises(TypeError) {"%c" % [[1, 2]]}
    assert_raises(RangeError) {"%c" % 500000000000000000000000000000000000005}
  end

  ##### inspect (%p) #####
  def test_inspect
    assert_equal('"howdy"', "%p" % 'howdy')
    assert_equal(":howdy", "%p" % :howdy)
    assert_equal("[1, 2]", "%p" % [[1,2]])
    assert_equal("  nil", "%5p" % nil)
  end

  def test_strange_printf
    opponent = '41181 jpa:awh'.scan("jpa")[0]
    assert_equal('jpa', sprintf("%s", opponent))
  end
end
