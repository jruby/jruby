require 'benchmark'
require 'socket'
iter = 50000
buf_sizes = [ 1, 16, 64, 256, 1024, 2048, 4096, 8192 ]
WPORT = 54321
RPORT = 12345

wserv = TCPServer.new('localhost', WPORT)
rserv = TCPServer.new('localhost', RPORT)

Thread.new {
  len = 4096
  buf = 0.chr * len    
  loop do
    client = wserv.accept
    total = 0
    begin
      loop do
        s = client.sysread(len, buf)
        total += s.length if s
      end      
    rescue Exception => ex
#      puts "sysread failed total=#{total}: #{ex}"
    end
    client.write(0.chr)
    client.close
  end
}

Thread.new {  
  loop do
    len = 4096
    buf = 0.chr * len
    client = rserv.accept
    total = 0
    begin
      loop do
        total += client.syswrite(buf)
      end
    rescue Exception => ex
    end
#    puts "closing client total written=#{total}"      
    client.close
  end
}

(ARGV[0] || 5).to_i.times do
  Benchmark.bm(30) do |x|
    x.report("#{iter}.times { putc(0) }") do
      TCPSocket.open('localhost', WPORT) do |sock|
        iter.times { sock.putc 0 }
        sock.close_write
        sock.read(1)
      end
    end if true
    buf_sizes.each do |size|
      x.report("#{iter}.times { read(#{size}) }") do
        TCPSocket.open('localhost', RPORT) do |sock|
          iter.times do
            len = 0
            while len < size
              s = sock.sysread(size - len)
              len += s.length if s
            end
          end
        end
      end if true
      x.report("#{iter}.times { sysread(#{size}) }") do
        buf = 0.chr * size
        TCPSocket.open('localhost', RPORT) do |sock|
          iter.times do
            len = 0
            while len < size
              s = sock.sysread(size - len)
              len += s.length if s
            end
          end
        end
      end if true
      x.report("#{iter}.times { write(#{size}) }") do
        TCPSocket.open('localhost', WPORT) do |sock|
          buf = 0.chr * size
          iter.times { sock.write buf }
          sock.close_write
          sock.read(1)
        end
      end if true
      x.report("#{iter}.times { syswrite(#{size}) }") do
        TCPSocket.open('localhost', WPORT) do |sock|
          buf = 0.chr * size
          iter.times { sock.syswrite buf }
          sock.close_write
          sock.read(1)
        end
      end if true      
    end
  end
end
