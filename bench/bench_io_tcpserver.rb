require 'benchmark'
require 'socket'
iter = 50000
buf_sizes = [ 1024, 2048, 4096, 8192 ]
PORT = 54321

serv = TCPServer.new('localhost', PORT)
Thread.new {
  loop do
    len = 4096
    buf = 0.chr * len
    client = serv.accept
      while client.read(len)
      end
      client.write(0.chr)
      client.close
  end
}
(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |x|
    buf_sizes.each do |size|
      x.report("#{iter}.times { socket.write(#{size}) }") do
        sock = TCPSocket.new('localhost', PORT)
        buf = 0.chr * size
        iter.times { sock.write buf }
        sock.close_write
        sock.read(1)
      end
    end
  end
end
