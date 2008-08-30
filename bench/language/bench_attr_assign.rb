require 'benchmark'

class BenchAttrAssign < Array
  attr_accessor :bar

  def control
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
  end
  
  def ten_assigns
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
  end
  
  def ten_array_assigns
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
  end
  
  def ten_array_assigns2
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
  end
  
  def ten_masgn_assigns
    self.bar,self.bar,self.bar,self.bar,self.bar,self.bar,self.bar,self.bar,self.bar,self.bar =
      1,2,3,4,5,6,7,8,9,0
  end
  
  def ten_masgn_array_assigns
    self[1],self[1],self[1],self[1],self[1],self[1],self[1],self[1],self[1],self[1] =
      1,2,3,4,5,6,7,8,9,0
  end
  
  def ten_masgn_array_assigns2
    self[1,2],self[1,2],self[1,2],self[1,2],self[1,2],self[1,2],self[1,2],self[1,2],self[1,2],self[1,2] =
      1,2,3,4,5,6,7,8,9,0
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
  end
end


def bench_attr_assign(bm)
  foo = BenchAttrAssign.new
  
  # NOTE: The performance of both array[1,2]= versions is heavily dependent on
  # the performance of Array#[] for three arguments. The substantially slower
  # performance here is largely due to the requirements of that method.
  
  bm.report("control") { 1000000.times { foo.control } }
  bm.report("1m attr asgns") { 1000000.times { foo.ten_assigns } }
  bm.report("1m array[1] asgns") { 1000000.times { foo.ten_array_assigns } }
  bm.report("1m array[1,2] asgns") { 1000000.times { foo.ten_array_assigns2 } }
  bm.report("1m attr masgns") { 1000000.times { foo.ten_masgn_assigns } }
  bm.report("1m array[1] masgns") { 1000000.times { foo.ten_masgn_array_assigns } }
  bm.report("1m array[1,2] masgns") { 1000000.times { foo.ten_masgn_array_assigns2 } }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(30) {|bm| bench_attr_assign(bm)} }
end