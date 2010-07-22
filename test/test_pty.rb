require 'test/unit'

class LoadPtyTest < Test::Unit::TestCase
  # JRUBY-4962
  def test_require_pty
    assert_nothing_raised { require 'pty' }
  end
end