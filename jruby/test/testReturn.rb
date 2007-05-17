require 'test/minirunit'
test_check "Test return(s):"

def meth1
  (1..10).each do |val|
    return val
  end
end

test_ok(1 == meth1)

def meth2(&b)
  b
end

res = meth2 { return }

test_exception(LocalJumpError){ res.call }

def meth3
  (1..10).each do |v1|
     ('a'..'z').each do |v2|
        return v2
     end
  end
end

test_ok('a' == meth3)

def meth4
  p = Proc.new { return 99 }
  p.call
  puts "Never get here"
end

test_ok(99 == meth4)

q = Proc.new { return 99 }

def meth5(p)
  p.call
end

test_exception(LocalJumpError) { meth5(q) } 

def meth6
  p = lambda { return 99 }
  test_ok(99 == p.call)
end

meth6

class B
  attr_reader :arr
  def initialize(arr)
    @arr = arr
  end
  def detect (nothing_found = nil)
    z = each { |e| return e if yield(e) }
    # should not get here if return hit
    puts "DOH #{z}"
    nothing_found.call unless nothing_found.nil?
    nil
  end
  def each
    i = 0
    loop do
      break if i == @arr.size
      yield @arr[i]
      i+=1
    end
  end
end


test_ok(2 == B.new([1, 2, 3, 4]).detect {|c| c > 1})

def proc_call(&b)
  b.call
end
def proc_return1
  proc_call{return 42}+1
end
test_ok(proc_return1() == 42)

# Procs (blocks) turn lambda-like when defined as a methods
# (actually define_method instance_eval's them, but lambda is easier for now)
class X
  def self.make_method(sym, &blk)
    define_method(sym, &blk)
  end
  
  make_method :foo do
    return "bar"
  end
end

test_equal("bar", X.new.foo)
