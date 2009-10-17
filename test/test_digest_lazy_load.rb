require 'digest'
require 'test/unit'

class TestDigestLazyLoad < Test::Unit::TestCase
  def test_lazy_md5 
    assert !Digest.const_defined?(:MD5)
    assert_not_nil Digest::MD5.new
    assert Digest.const_defined?(:MD5)
  end

  def test_lazy_sha1 
    assert !Digest.const_defined?(:SHA1)
    assert_not_nil Digest::SHA1.new
    assert Digest.const_defined?(:SHA1)
  end

  def test_lazy_sha2
    assert !Digest.const_defined?(:SHA256)
    assert !Digest.const_defined?(:SHA384)
    assert !Digest.const_defined?(:SHA512)
    assert_not_nil Digest::SHA256.new
    assert Digest.const_defined?(:SHA256)
    assert Digest.const_defined?(:SHA384)
    assert Digest.const_defined?(:SHA512)
  end

end
