require 'test/unit'
require 'digest'

class TestDigest2 < Test::Unit::TestCase

  # JRUBY-4083
  def test_sha2
    assert_equal(Digest::SHA256.new, Digest::SHA2.new)
  end

end
