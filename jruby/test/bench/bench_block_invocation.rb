require 'benchmark'

puts "100k loops yielding a fixnum 10 times to a block that just retrieves dvar"

def foocall(arg)
  arg
end
def foo
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
  yield 1
end
def foo2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
  yield 1,2
end
def foo2_5
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
  yield 1,2,3
end
def foo3
  yield
  yield
  yield
  yield
  yield
  yield
  yield
  yield
  yield
  yield
end
def foo4
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
  foocall(1)
end

TIMES = 5

TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo {|j| j}
      i += 1;
    end
  }
}

puts "100k loops yielding two fixnums 10 times to block accessing one"
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo2 {|j,k| k}
      i += 1;
    end
  }
}

puts "100k loops yielding three fixnums 10 times to block accessing one"
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo2_5 {|j,k,l| k}
      i += 1;
    end
  }
}

puts "100k loops yielding three fixnums 10 times to block splatting and accessing them"
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo2_5 {|*j| j}
      i += 1;
    end
  }
}

puts "100k loops yielding a fixnums 10 times to block with just a fixnum (no vars)"
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo3 {1}
      i += 1;
    end
  }
}

puts "100k loops calling a method with a fixnum that just returns it"
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 100000
      foo4
      i += 1;
    end
  }
}
