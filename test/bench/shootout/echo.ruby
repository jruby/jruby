#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: echo.ruby,v 1.1.1.1 2004-05-19 18:09:37 bfulgham Exp $
# http://www.bagley.org/~doug/shootout/

require "socket"

DATA = "Hello there sailor\n"

def echo_client(n, port)
    sock = TCPsocket.open('127.0.0.1', port)
    n.times do
	sock.write(DATA)
	ans = sock.readline
	if ans != DATA then
	    raise sprintf("client: \"%s\" \"%s\"", DATA, ans)
	end
    end
    sock.close
end


def echo_server(n)
    ssock = TCPserver.open('127.0.0.1', 0)
    port = ssock.addr[1]
    if pid = fork then
	# parent is server
	csock = ssock.accept
	n = 0
	while str = csock.gets
	    n += csock.write(str)
	end
	Process.wait
        printf "server processed %d bytes\n", n
    else
	# child is client
	echo_client(n, port)
    end
end

echo_server(Integer(ARGV.shift || 1))
