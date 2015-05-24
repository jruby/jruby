require 'socket'
require 'io/nonblock'

describe 'Writing zero bytes to a nonblocking socket' do
  it 'returns 0 bytes and does not raise EAGAIN' do
    begin
      s = TCPSocket.new('google.com', 80)
      s.nonblock
      expect(s.syswrite('')).to eq 0
    ensure
      s.close
    end
  end
end
