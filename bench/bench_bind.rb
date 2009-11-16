require 'benchmark'
require 'socket'

include Socket::Constants

(ARGV[0] || 5).to_i.times do
  Benchmark.bm(35) do |bm|
    bm.report("10K Socket#bind for SOCK_STREAM") {
      10_000.times {
        socket = Socket.new(AF_INET, SOCK_STREAM, 0)
        sockaddr = Socket.pack_sockaddr_in(4001, 'localhost')
        socket.bind(sockaddr)
        socket.close
      }
    }
    bm.report("10K Socket#bind for SOCK_DGRAM") {
      10_000.times {
        socket = Socket.new(AF_INET, SOCK_DGRAM, 0)
        sockaddr = Socket.pack_sockaddr_in(4001, 'localhost')
        socket.bind(sockaddr)
        socket.close
      }
    }
  end
end
