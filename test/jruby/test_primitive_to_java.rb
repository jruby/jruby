# encoding: UTF-8
require 'test/unit'

class TestPrimitiveToJava < Test::Unit::TestCase

  def setup; super; require 'java' end

  def test_primitive_conversion
    t = Time.now
    date = t.to_java(java.util.Date)

    assert_equal(t.to_i, date.time / 1000, "Ruby time #{t} not converted to java date correctly: #{date}")
  end

  def test_char_conversion
    str = 'a'
    char = str.to_java(:char)
    assert_instance_of Java::JavaLang::Character, char

    str = ' '
    char = str.to_java(Java::char)
    assert_equal 32, char.charValue

    str = '0'
    char = str.to_java(java.lang.Character)
    assert_equal 48.to_java(:char), char

    assert_equal 228, 'ä'.to_java(:char).charValue

    assert_equal 'už'.ord, 'už'.to_java('java.lang.Character').charValue
    assert_raises(ArgumentError) { ''.to_java(:char) } # like ''.ord
  end

end
