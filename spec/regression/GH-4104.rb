require 'rspec'
require 'socket'
require 'resolv'

describe "recv from unreachable destination" do
  it "fails with proper error" do
    socket = UDPSocket.new(Socket::AF_INET)
    socket.do_not_reverse_lookup = true
    Resolv::DNS.bind_random_port(socket)
    socket.connect('0.0.0.0', 53)
    socket.send('some_data', 0)
    socket.wait_readable(1)
    expect { socket.recv(512) }.to raise_error(Errno::ECONNREFUSED)
  end
end
