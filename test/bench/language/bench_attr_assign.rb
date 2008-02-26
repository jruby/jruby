require 'benchmark'

class Foo < Array
  attr_accessor :bar

  def control
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
    self; 1; self; 1; self; 1; self; 1; self; 1
  end
  
  def hundred_assigns
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
    self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1; self.bar = 1
  end
  
  def hundred_array_assigns
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
    self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1; self[1] = 1
  end
  
  def hundred_array_assigns2
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
    self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1; self[1,2] = 1
  end
end

foo = Foo.new

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(20) {|bm|
    bm.report("control") { 100000.times { foo.control } }
    bm.report("100 asgns") { 100000.times { foo.hundred_assigns } }
    bm.report("100 array[1] asgns") { 100000.times { foo.hundred_array_assigns } }
    bm.report("100 array[1,2] asgns") { 100000.times { foo.hundred_array_assigns2 } }
  }
}
