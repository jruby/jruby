require 'test/unit'
require 'java'
require 'jruby'
require 'jruby/serialization'
require 'fileutils'
java_import java.io.ObjectOutputStream
java_import java.io.FileOutputStream
java_import java.io.FileInputStream


class TestJrubyObjectInputStream < Test::Unit::TestCase
  def test_serialize_and_deserialize_java_object
    str = java.lang.String.new("hi")
    out_stream = ObjectOutputStream.new(FileOutputStream.new("store"))
    out_stream.write_object(str)
    out_stream.close
    in_stream = JRubyObjectInputStream.new(FileInputStream.new("store"))

    str2 = nil
    assert_nothing_raised do
      str2 = in_stream.read_object
    end
    in_stream.close
    FileUtils.rm_f "store"
    assert_equal str,str2
  end
end

    
