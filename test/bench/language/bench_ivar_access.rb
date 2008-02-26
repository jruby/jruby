require 'benchmark'

module IvarBench
  attr_accessor :a

  def benchmark_ivar_get
    i = 0
    while i < 100000
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      @a; @a; @a; @a; @a; @a; @a; @a; @a; @a;          
      i += 1
    end
  end

  def benchmark_ivar_set
    i = 0
    while i < 100000
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1; @a = 1
      i += 1
    end
  end

  def benchmark_attr_get
    i = 0
    while i < 100000
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      a; a; a; a; a; a; a; a; a; a;
      i += 1
    end
  end

  def benchmark_attr_set
    i = 0
    while i < 100000
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;self.a = 1;
      i += 1
    end
  end
end

class IvarOne
  include IvarBench
  
  def initialize
    @a = 1
  end
end

class IvarTwo
  include IvarBench
  
  def initialize
    @dummy1 = 1
    @a = 1
  end
end

class IvarFour
  include IvarBench
  
  def initialize
    @dummy1 = 1
    @dummy2 = 1
    @dummy3 = 1
    @a = 1
  end
end

class IvarEight
  include IvarBench
  
  def initialize
    @dummy1 = 1
    @dummy2 = 1
    @dummy3 = 1
    @dummy4 = 1
    @dummy5 = 1
    @dummy6 = 1
    @dummy7 = 1
    @a = 1
  end
end

class IvarSixteen
  include IvarBench
  
  def initialize
    @dummy1 = 1
    @dummy2 = 1
    @dummy3 = 1
    @dummy4 = 1
    @dummy5 = 1
    @dummy6 = 1
    @dummy7 = 1
    @dummy8 = 1
    @dummy9 = 1
    @dummy10 = 1
    @dummy11 = 1
    @dummy12 = 1
    @dummy13 = 1
    @dummy14 = 1
    @dummy15 = 1
    @a = 1
  end
end

def bench_ivar_access(bm)
  bm.report("100k * 100 ivar gets, 1 ivar") do
    b = IvarOne.new; b.benchmark_ivar_get
  end

  bm.report("100k * 100 ivar sets, 1 ivar") do
    b = IvarOne.new; b.benchmark_ivar_set
  end

  bm.report("100k * 100 attr gets, 1 ivar") do
    b = IvarOne.new; b.benchmark_attr_get
  end

  bm.report("100k * 100 attr sets, 1 ivar") do
    b = IvarOne.new; b.benchmark_attr_set
  end

  bm.report("100k * 100 ivar gets, 2 ivar") do
    b = IvarTwo.new; b.benchmark_ivar_get
  end

  bm.report("100k * 100 ivar sets, 2 ivar") do
    b = IvarTwo.new; b.benchmark_ivar_set
  end

  bm.report("100k * 100 attr gets, 2 ivar") do
    b = IvarTwo.new; b.benchmark_attr_get
  end

  bm.report("100k * 100 attr sets, 2 ivar") do
    b = IvarTwo.new; b.benchmark_attr_set
  end

  bm.report("100k * 100 ivar gets, 4 ivar") do
    b = IvarFour.new; b.benchmark_ivar_get
  end

  bm.report("100k * 100 ivar sets, 4 ivar") do
    b = IvarFour.new; b.benchmark_ivar_set
  end

  bm.report("100k * 100 attr gets, 4 ivar") do
    b = IvarFour.new; b.benchmark_attr_get
  end

  bm.report("100k * 100 attr sets, 4 ivar") do
    b = IvarFour.new; b.benchmark_attr_set
  end

  bm.report("100k * 100 ivar gets, 8 ivar") do
    b = IvarEight.new; b.benchmark_ivar_get
  end

  bm.report("100k * 100 ivar sets, 8 ivar") do
    b = IvarEight.new; b.benchmark_ivar_set
  end

  bm.report("100k * 100 attr gets, 8 ivar") do
    b = IvarEight.new; b.benchmark_attr_get
  end

  bm.report("100k * 100 attr sets, 8 ivar") do
    b = IvarEight.new; b.benchmark_attr_set
  end

  bm.report("100k * 100 ivar gets, 16 ivar") do
    b = IvarSixteen.new; b.benchmark_ivar_get
  end

  bm.report("100k * 100 ivar sets, 16 ivar") do
    b = IvarSixteen.new; b.benchmark_ivar_set
  end

  bm.report("100k * 100 attr gets, 16 ivar") do
    b = IvarSixteen.new; b.benchmark_attr_get
  end

  bm.report("100k * 100 attr sets, 16 ivar") do
    b = IvarSixteen.new; b.benchmark_attr_set
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_ivar_access(bm)} }
end
