
require 'test/unit'

# JRUBY-1188
class TestBase64Strangeness < Test::Unit::TestCase
  def test_base64_strangeness
    require 'base64'
    assert Class.public_instance_methods.include?('encode64')
    assert !Class.private_instance_methods.include?('encode64')
  end
end

# be strict GH-2551
class TestBase64Strangeness < Test::Unit::TestCase
  def test_base64_stictness
    require 'base64'
    assert_raise(ArgumentError) { Base64.strict_decode64("AA==AAAA") }
  end
end if RUBY_VERSION >= '1.9'
