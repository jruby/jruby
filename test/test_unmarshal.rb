require 'test/unit'
require 'stringio'
require 'tempfile'

# Test for issue JIRA-2506
# Fails with an EOF error in JRuby 1.1.1, works in MRI 1.8.6
# Author: steen.lehmann@gmail.com

class TestUnmarshal < Test::Unit::TestCase

  def testUnmarshal
    dump = ''
    dump << Marshal.dump("hey")
    dump << Marshal.dump("there")

    result = "none"
    StringIO.open(dump) do |f|
      result = Marshal.load(f)
      assert_equal "hey", result, "first string unmarshalled"      
      result = Marshal.load(f)
    end
    assert_equal "there", result, "second string unmarshalled"
  rescue EOFError
    flunk "Unmarshalling failed with EOF error at " + result + " string."
  end

  def test_fixnum_unbuffered
    # need to be big enough for reading unbuffered bytes from ChannelStream.
    obj = Array.new(2000, 60803)
    dump = Marshal.dump(obj)
    piper, pipew = IO.pipe
    pipew << dump
    pipew.close
    Marshal.load(piper).each do |e|
      assert_equal(60803, e, 'JRUBY-5064')
    end
    piper.close
  end

  # TYPE_IVAR from built-in class
  class C
    def _dump(depth)
      "foo"
    end

    def self._load(str)
      new
    end
  end

  def test_ivar_in_built_in_class
    (o = "").instance_variable_set("@ivar", C.new)
    assert_nothing_raised do
      Marshal.load(Marshal.dump(o))
    end
  end

  # JRUBY-5123: nested TYPE_IVAR from _dump
  class D
    def initialize(ivar = nil)
      @ivar = ivar
    end

    def _dump(depth)
      str = ""
      str.instance_variable_set("@ivar", @ivar)
      str
    end

    def self._load(str)
      new(str.instance_variable_get("@ivar"))
    end
  end

  def test_ivar_through_s_dump
    o = D.new(D.new)
    assert_nothing_raised do
      Marshal.load(Marshal.dump(o))
    end
  end

  # JRUBY-5002: Stuck when loading marshalled data > 32000 bytes from IO stream
  def test_unmarshal_giant_string
    data = ("a" * 100) * 1000
    tf = Tempfile.new("test_unmarshal_giant_string")
    tf.write(Marshal.dump(data))

    assert_equal(data, Marshal.load(File.read(tf.path)))
  ensure
    tf.close!
  end
end
