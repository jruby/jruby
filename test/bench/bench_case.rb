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
