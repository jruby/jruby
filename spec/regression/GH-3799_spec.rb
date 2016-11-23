require 'rspec'
require 'socket'
require 'io/nonblock'

describe "syswrite called on unix socket in nonblocking mode" do
  # Based on https://github.com/puma/puma/blob/3f66b3d7d4413f843e4e541c4d282238318c4cd2/lib/puma/server.rb#L903
  def fast_write(io, str)
    n = 0
    while true
      begin
        n = io.syswrite str
      rescue Errno::EAGAIN, Errno::EWOULDBLOCK
        retry
      end

      return if n == str.bytesize
      str = str.byteslice(n..-1)
    end
  end

  it "should not fail with 'SystemCallError: Unknown error -'" do
    begin
      # On my machine error appears when more than two writes needed.
      # Buffer size equals to 8192, so input size should be
      # bigger thatn 8192 * 2 + 1
      input = ' ' * 16385

      s1, s2 = UNIXSocket.pair
      s1.nonblock = true
      s2.nonblock = true

      t = Thread.new { s2.read(input.bytesize).bytesize }

      expect { fast_write(s1, input) }.not_to raise_error
      expect(t.join(1).value).to eq(16385)
    ensure
      s1.close
      s2.close
    end
  end
end
