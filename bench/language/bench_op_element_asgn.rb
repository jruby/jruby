require 'benchmark'

class BenchOpElementAsgn < Array
  attr_accessor :bar
  
  def initialize
    @bar = 1
    self[0] = false
    self[1] = true
    self[2] = 1
  end
  
  def control_or
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
    self[0] || false; self[0] || false; self[0] || false; self[0] || false; self[0] || false
  end

  def control_and
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
    self[1] && true; self[1] && true; self[1] && true; self[1] && true; self[1] && true
  end

  def control_plus
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
    self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1; self[2] + 1
  end
  
  def hundred_element_or_assigns
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
    self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false; self[0] ||= false
  end
  
  def hundred_element_and_assigns
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
    self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true; self[1] &&= true
  end
  
  def hundred_element_plus_assigns
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
    self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1; self[2] += 1
  end
end

def bench_op_element_asgn(bm)
  foo = BenchOpElementAsgn.new

  bm.report("control a[b] || c") { 100000.times { foo.control_or } }
  bm.report("control a[b] && c") { 100000.times { foo.control_and } }
  bm.report("control a[b] + c") { 100000.times { foo.control_plus } }
  bm.report("100 a[b] ||= c") { 100000.times { foo.hundred_element_or_assigns } }
  bm.report("100 a[b] &&= c") { 100000.times { foo.hundred_element_and_assigns } }
  bm.report("100 a[b] += c") { 100000.times { foo.hundred_element_plus_assigns } }
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_op_element_asgn(bm)} }
end
