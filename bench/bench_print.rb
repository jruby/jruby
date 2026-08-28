require 'benchmark/ips'

def do_print0(io)
  io.print
end

def do_print1(io)
  io.print ?x
end

def do_print2(io)
  io.print ?x, ?x
end

def do_print3(io)
  io.print ?x, ?x, ?x
end

def do_print4(io)
  io.print ?x, ?x, ?x, ?x
end

Benchmark.ips do |bm|
  io = File.open(IO::NULL, 'w')

  bm.report("print no arg") do |i|
    while i > 0
      i-=1
      do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io);do_print0(io)
    end
  end

  bm.report("print one arg") do |i|
    while i > 0
      i-=1
      do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io);do_print1(io)
    end
  end

  bm.report("print two arg") do |i|
    while i > 0
      i-=1
      do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io);do_print2(io)
    end
  end

  bm.report("print three arg") do |i|
    while i > 0
      i-=1
      do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io);do_print3(io)
    end
  end

  bm.report("print four arg") do |i|
    while i > 0
      i-=1
      do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io);do_print4(io)
    end
  end
end