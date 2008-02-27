require 'test/unit'
require 'timeout'
require 'benchmark'

class TestTimeout < Test::Unit::TestCase
  def test_timeout_for_loop
    n = 10000000
    assert_raises(Timeout::Error) do
      Timeout::timeout(1) { for i in 0..n do; (i + i % (i+1)) % (i + 10) ; end }
    end
  end
end
