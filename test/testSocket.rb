require 'minirunit'

test_check "Test Socket"

require 'socket'

test_equal("127.0.0.1", TCPSocket.getaddress("localhost"))

namearray = TCPSocket.gethostbyname("localhost")
test_equal(4, namearray.size)
test_equal(Socket::AF_INET, namearray[2])
test_equal("127.0.0.1", namearray[3])

server = TCPServer.new(7707)

cs = TCPSocket.new("localhost", 7707)
sent = cs.send("hello", 0)
test_equal("hello".size, sent)

ss = server.accept

received = ss.recv(2)
test_equal("he", received)

received << ss.recv(10)
test_equal("hello", received)

cs.close
ss.close

server.close
