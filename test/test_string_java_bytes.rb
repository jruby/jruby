require 'java'
require 'test/unit'

class TestStringJavaBytes < Test::Unit::TestCase

  def test_conversions
    # this needed to be modified to clear up the ambiguity surrounding Java's
    # byte being signed; you can only coerce values from -128..127 safely without
    # Java seeing a different value than Ruby. Value can't be lost across the
    # boundary.
    all_byte_values = Array.new(256) {|i| i - 128}

    java_bytes = all_byte_values.to_java(:byte)
    string_from_bytes = String.from_java_bytes(java_bytes)
    assert(string_from_bytes.instance_of?(String))
    assert_equal(string_from_bytes.length, java_bytes.length)
    # different sign for 0..127
    0.upto(127) do |i|
      assert_equal(all_byte_values[i] + 256, string_from_bytes[i])
    end
    # same sign for 128..255
    128.upto(255) do |i|
      assert_equal(all_byte_values[i], string_from_bytes[i])
    end
    
    bytes_from_string = string_from_bytes.to_java_bytes
    assert(bytes_from_string.instance_of?(Java::byte[]))
    assert_equal(bytes_from_string.length, string_from_bytes.length)
    0.upto(255) do |i|
      assert_equal(bytes_from_string[i], java_bytes[i])
    end
    
  end

  def test_exceptions
    assert_raises(TypeError) { String.from_java_bytes(Java::double[1].new) }
    assert_raises(TypeError) { String.from_java_bytes(3.141) }
    # must test underlying call for to_java_bytes
    assert_raises(TypeError) { JavaArrayUtilities.ruby_string_to_bytes(3.141) }
    assert_raises(TypeError) { JavaArrayUtilities.ruby_string_to_bytes( Object.new ) }
  end
end