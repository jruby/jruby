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

  # JRUBY-5147
  def test_initialize_args
    assert_raises(ArgumentError) do
      Digest::MD5.new('abc')
    end
    # but SHA2 accepts an arg.
    assert_equal 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      Digest::SHA2.new(256).hexdigest
    assert_equal '38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b',
      Digest::SHA2.new(384).hexdigest
    assert_equal 'cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e',
      Digest::SHA2.new(512).hexdigest
  end
end
