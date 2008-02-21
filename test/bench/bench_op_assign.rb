require 'benchmark'

class Foo
  attr_accessor :op_or
  attr_accessor :op_and
  attr_accessor :op_plus
  
  def initialize
    self.op_or = false
    self.op_and = true
    self.op_plus = 1
  end

  def control_or
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
    self; false || false; self; false || false; self; false || false; self; false || false; self; false || false
  end

  def control_and
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
    self; true && true; self; true && true; self; true && true; self; true && true; self; true && true
  end

  def control_plus
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
    self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1; self; 1 + 1
  end
  
  def hundred_op_or_assigns
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
    self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false; self.op_or ||= false
  end
  
  def hundred_op_and_assigns
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
    self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true; self.op_and &&= true
  end
  
  def hundred_op_plus_assigns
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
    self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1; self.op_plus += 1
  end
end

foo = Foo.new

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(20) {|bm|
    bm.report("control ||") { 100000.times { foo.control_or } }
    bm.report("control &&") { 100000.times { foo.control_and } }
    bm.report("control +") { 100000.times { foo.control_plus } }
    bm.report("100 op || asgns") { 100000.times { foo.hundred_op_or_assigns } }
    bm.report("100 or && asgns") { 100000.times { foo.hundred_op_and_assigns } }
    bm.report("100 op + asgns") { 100000.times { foo.hundred_op_plus_assigns } }
  }
}