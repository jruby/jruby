require 'benchmark'

module IvarBench
  attr_accessor :a

  def benchmark_ivar_get
    i = 0
    while i < 100000
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;a = @a;          
      i += 1
    end
  end

  def benchmark_attr_get
    i = 0
    while i < 100000
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
      _a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;_a = a;
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

puts "100k loop of 100 ivar accesses and assign to local; one ivar"
5.times { puts Benchmark.measure { b = IvarOne.new; b.benchmark_ivar_get } }

puts "100k loop of 100 attr accesses and assign to local; one ivar"
5.times { puts Benchmark.measure { b = IvarOne.new; b.benchmark_attr_get } }

puts "100k loop of 100 ivar accesses and assign to local; two ivars"
5.times { puts Benchmark.measure { b = IvarTwo.new; b.benchmark_ivar_get } }

puts "100k loop of 100 attr accesses and assign to local; two ivars"
5.times { puts Benchmark.measure { b = IvarTwo.new; b.benchmark_attr_get } }

puts "100k loop of 100 ivar accesses and assign to local; four ivars"
5.times { puts Benchmark.measure { b = IvarFour.new; b.benchmark_ivar_get } }

puts "100k loop of 100 attr accesses and assign to local; four ivars"
5.times { puts Benchmark.measure { b = IvarFour.new; b.benchmark_attr_get } }

puts "100k loop of 100 ivar accesses and assign to local; eight ivars"
5.times { puts Benchmark.measure { b = IvarEight.new; b.benchmark_ivar_get } }

puts "100k loop of 100 attr accesses and assign to local; eight ivars"
5.times { puts Benchmark.measure { b = IvarEight.new; b.benchmark_attr_get } }

puts "100k loop of 100 ivar accesses and assign to local; sixteen ivars"
5.times { puts Benchmark.measure { b = IvarSixteen.new; b.benchmark_ivar_get } }

puts "100k loop of 100 attr accesses and assign to local; sixteen ivars"
5.times { puts Benchmark.measure { b = IvarSixteen.new; b.benchmark_attr_get } }
