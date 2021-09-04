#!/usr/bin/ruby
#### The Great Computer Language Shootout
#### http://shootout.alioth.debian.org/
#### 
#### Contributed by Robbert Haarman
#### Modified by Ian Osgood

require 'socket'

N = Integer(ARGV[0] || 10)
M = 6400
REPLY_SIZE = 64
REQUEST_SIZE = 1
Host = 'localhost'
Port = 12345

sock = TCPServer.new Host, Port
if fork
	# Parent process
	conn = sock.accept
	reply = 'x' * REPLY_SIZE
	while true
		request = conn.read REQUEST_SIZE
		break if request == nil
		conn.write reply
	end
else
	# Child process
	conn = TCPSocket.new Host, Port
	replies = 0
	bytes = 0
	n = N * M
	request = 'x' * REQUEST_SIZE
	while n > 0
		n = n - 1
		conn.write request
		reply = conn.read REPLY_SIZE
		replies = replies + 1
		bytes = bytes + reply.length
	end
	conn.close
	puts "replies: #{replies}\tbytes: #{bytes}"
end

sock.close
