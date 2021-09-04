require 'test/unit'

class TestBase64 < Test::Unit::TestCase
  # JRUBY-1188
  def test_base64_strangeness
    require 'base64'
    assert !Class.private_instance_methods.include?(:encode64)
  end

  # be strict GH-2551
  def test_base64_stictness
    require 'base64'
    assert_raise(ArgumentError) { Base64.strict_decode64("AA==AAAA") }
  end
end
