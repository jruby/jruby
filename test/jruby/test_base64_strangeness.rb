
require 'test/unit'

# JRUBY-1188
class TestBase64Strangeness < Test::Unit::TestCase
  def test_base64_strangeness
    require 'base64'
    assert Class.public_instance_methods.include?('encode64')
    assert !Class.private_instance_methods.include?('encode64')
  end
end
