require 'benchmark'

class BenchCase
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
end

def bench_case(bm)
  bc = BenchCase.new
  a = 0
  while a < 1_000_000
    bc.do_case3
    a += 1
  end

  bm.report "1m x5 cases with 1 when" do
    bc.do_bench1
  end

  bm.report "1m x5 cases with 1 3-arg when" do
    bc.do_bench2
  end

  bm.report "1m x5 cases with 10 whens" do
    bc.do_bench3
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_case(bm)} }
end