require 'java'
require 'test/unit'

class TestStringJavaBytes < Test::Unit::TestCase

  def test_conversions
    # values 0-255
    all_byte_values = Array.new(256) {|i| i}

    java_bytes = all_byte_values.to_java(:byte)
    string_from_bytes = String.from_java_bytes(java_bytes)
    assert(string_from_bytes.instance_of?(String))
    assert_equal(string_from_bytes.length, java_bytes.length)
    0.upto(255) do |i|
      assert_equal(all_byte_values[i], string_from_bytes[i])
      # this won't work, because JavaArray#aref returns the signed value
      #assert_equal(java_bytes[i], string_from_bytes[i])
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