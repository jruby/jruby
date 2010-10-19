require 'test/unit'
require 'digest'

class TestDigest2 < Test::Unit::TestCase

  # JRUBY-4083
  def test_sha2
    assert_equal(Digest::SHA256.new, Digest::SHA2.new)
  end

  # JRUBY-5141: fails on 1.9 mode only
  def test_equality
    a = Digest::MD5.new
    b = Digest::MD5.new
    assert_equal(true, (a == b))
    assert_equal(false, (a != b))
    assert_equal(false, (a.eql?(b)))
  end
end
