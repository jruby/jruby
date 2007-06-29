require 'benchmark'

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

puts "1m loops yielding a fixnum 10 times to a block that just retrieves dvar"
def test1
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo {|j| j}
      i += 1;
    end
  }
}
end
test1

puts "1m loops yielding two fixnums 10 times to block accessing one"
def test2
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo2 {|j,k| k}
      i += 1;
    end
  }
}
end
test2

puts "1m loops yielding three fixnums 10 times to block accessing one"
def test3
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo2_5 {|j,k,l| k}
      i += 1;
    end
  }
}
end
test3

puts "1m loops yielding three fixnums 10 times to block splatting and accessing them"
def test4
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo2_5 {|*j| j}
      i += 1;
    end
  }
}
end
test4

puts "1m loops yielding a fixnums 10 times to block with just a fixnum (no vars)"
def test5
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo3 {1}
      i += 1;
    end
  }
}
end
test5

puts "1m loops calling a method with a fixnum that just returns it"
def test6
TIMES.times {
  puts Benchmark.measure {
    a = 5; 
    i = 0;
    while i < 1000000
      foo4
      i += 1;
    end
  }
}
end
test6
