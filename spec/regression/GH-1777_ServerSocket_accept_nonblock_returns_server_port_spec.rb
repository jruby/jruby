require 'socket'

#https://github.com/jruby/jruby/issues/1777
describe 'ServerSocket#accept_nonblock' do
  it 'returns the client address' do
    server_port = 2**15 + rand(2**15)
    puts 'server running on %d' % server_port
    addrinfos = Socket.getaddrinfo('localhost', server_port, nil, Socket::SOCK_STREAM)
    _, port, _, ip, address_family, socket_type = addrinfos.shift
    sockaddr = Socket.sockaddr_in(port, ip)
    client_socket = Socket.new(address_family, socket_type, 0)
    server_socket = ServerSocket.new(address_family, socket_type, 0)
    server_socket.bind(sockaddr, 5)
    begin
      client_socket.connect_nonblock(sockaddr)
    rescue Errno::EINPROGRESS, Errno::EALREADY
    end
    IO.select([server_socket])
    client_socket, client_sockaddr = server_socket.accept_nonblock
    port = Socket.unpack_sockaddr_in(client_sockaddr).first
    expect(port).to eq(client_socket.remote_address.ip_port)
    expect(port).not_to eq(server_port)
  end
end if RUBY_VERSION > '1.9'
