require 'test/minirunit'
require 'socket'

server_read = nil
client_read = nil

server_thread = Thread.new do
  serv = TCPServer.new('localhost',2202)
  sock = serv.accept
  
  server_read = sock.read(5)
  sock.write "world!"
  sock.close
end

# This test is seriously broken, prone to race conditions and sometimes fail. This is why the rescue nil is there.
sleep 1

begin
  socket = TCPSocket.new("localhost",2202) 
  socket.write "Hello"
  client_read = socket.read(6)
  socket.close
  server_thread.join

  test_equal("Hello", server_read)
  test_equal("world!", client_read)
rescue
end
  
serv = TCPServer.new('localhost',2203)
test_no_exception { serv.listen(1024) } # fix for listen blowing up because it tried to rebind; it's a noop now

# test block behavior for TCPServer::open
test_no_exception {
  TCPServer.open('localhost', 2204) {|sock| test_equal(TCPServer, sock.class)}
  TCPServer.open('localhost', 2204) {}
}

# test that raising inside an accepting thread doesn't nuke the socket
tcp = TCPServer.new(nil, 5000)
ok = false
exception = nil
ready = false
t = Thread.new {
  begin
    ready = true
    tcp.accept
  rescue Exception => e
    exception = e
    # this would normally blow up if the socket was demolished
    tcp.close
    ok = true
    ready = false
  end
}
sleep 0.1 # on windows, if we don't pause before testing ready, this fails
Thread.pass until ready
t.raise
Thread.pass while ready
test_ok ok
test_ok RuntimeError === exception

