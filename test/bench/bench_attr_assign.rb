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
end

foo = Foo.new

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(20) {|bm|
    bm.report("control") { 10000.times { foo.control } }
    bm.report("100 asgns") { 10000.times { foo.hundred_assigns } }
    bm.report("100 array asgns") { 10000.times { foo.hundred_array_assigns } }
  }
}