require 'test/unit'
require 'java'

class TestNullChannel < Test::Unit::TestCase
  import java.nio.ByteBuffer
  import org.jruby.util.io.NullChannel

  def test_write_advances_buffer
    buf = ByteBuffer.allocate(128)
    ch = NullChannel.new

    50.times { buf.put(1) } # 50 longs
    buf.flip

    ch.write(buf)

    assert_equal(50, buf.position)
  end
end
