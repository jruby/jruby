require 'benchmark'
require 'socket'
iter = 50000
buf_sizes = [ 1024, 2048 ]
PORT = 54321

serv = TCPServer.new('localhost', PORT)
Thread.new {
  loop do
    buf = 0.chr * 4096
    client = serv.accept
      while s = client.read(buf.length, buf)
      end
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
        sock.close
      end
    end
  end
end
