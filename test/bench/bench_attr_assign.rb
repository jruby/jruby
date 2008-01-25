require 'benchmark'

class Foo
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
end

foo = Foo.new

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(20) {|bm|
    bm.report("control") { 10000.times { foo.control } }
    bm.report("hundred assigns") { 10000.times { foo.hundred_assigns } }
  }
}