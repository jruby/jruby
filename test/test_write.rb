require 'test/unit'

class TestIOWrite < Test::Unit::TestCase
  def test_write
    a, b = IO.pipe
    begin
      a.close
      assert_raises(Errno::EPIPE) do
        b.puts("hi")
        b.close
      end
    ensure
      a.close if !a.closed?
      b.close if !b.closed?
    end
  end
end
