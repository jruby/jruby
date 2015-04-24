# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'socket'

server = TCPServer.new('localhost', 8080)

loop do
  socket = server.accept

  begin
    request = socket.gets

    socket.print "HTTP/1.1 200 OK\r\n" +
                 "Content-Type: text/plain\r\n" +
                 "Content-Length: #{request.bytesize}\r\n" +
                 "Connection: close\r\n\r\n"
    socket.print request
    socket.print "\n"
  ensure
    socket.close
  end
end
