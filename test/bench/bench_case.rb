require 'benchmark'

def do_case1
  case 1;
  when 1;
  end
  case 1;
  when 1;
  end
  case 1;
  when 1;
  end
  case 1;
  when 1;
  end
  case 1;
  when 1;
  end
end

def do_bench1
  a = 0
  while a < 1_000_000
    do_case1
    a += 1
  end
end

def do_case2
  case 1;
  when 3,2,1;
  end
  case 1;
  when 3,2,1;
  end
  case 1;
  when 3,2,1;
  end
  case 1;
  when 3,2,1;
  end
  case 1;
  when 3,2,1;
  end
end

def do_bench2
  a = 0
  while a < 1_000_000
    do_case2
    a += 1
  end
end

def do_case3
  case 1
  when 10
  when 9
  when 8
  when 7
  when 6
  when 5
  when 4
  when 3
  when 2
  when 1
  end
  case 1
  when 10
  when 9
  when 8
  when 7
  when 6
  when 5
  when 4
  when 3
  when 2
  when 1
  end
  case 1
  when 10
  when 9
  when 8
  when 7
  when 6
  when 5
  when 4
  when 3
  when 2
  when 1
  end
  case 1
  when 10
  when 9
  when 8
  when 7
  when 6
  when 5
  when 4
  when 3
  when 2
  when 1
  end
  case 1
  when 10
  when 9
  when 8
  when 7
  when 6
  when 5
  when 4
  when 3
  when 2
  when 1
  end
end

def do_bench3
  a = 0
  while a < 1_000_000
    do_case3
    a += 1
  end
end

puts "1m calls to a method with five case statements with a case argument and a single when"
5.times {
  puts Benchmark.measure {
    do_bench1
  }
}

puts "1m calls to a method with five case statements with a case argument and a 3-arg when"
5.times {
  puts Benchmark.measure {
    do_bench2
  }
}

puts "1m calls to a method with five case statements with a case argument and 10 whens (last matches)"
5.times {
  puts Benchmark.measure {
    do_bench3
  }
}
