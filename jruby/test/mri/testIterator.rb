require 'test/minirunit'

test_check "iterator"

test_ok(!iterator?)

def ttt
  test_ok(iterator?)
end
ttt{}

# yield at top level
test_ok(!defined?(yield))

$x = [1, 2, 3, 4]
$y = []

# iterator over array
for i in $x
  $y.push i
end
test_ok($x == $y)

# nested iterator
def tt
  1.upto(10) {|i|
    yield i
  }
end

tt{|i| break if i == 5}

test_ok(i == 5)

def tt2(dummy)
  yield 1
end

def tt3(&block)
  tt2(raise(ArgumentError,""),&block)
end

$x = false
begin
  tt3{}
rescue ArgumentError
  $x = true
rescue Exception
end
test_ok($x)

# iterator break/redo/next/retry
done = true
loop{
  break
  done = false			# should not reach here
}
test_ok(done)

done = false
$bad = false
loop {
  break if done
  done = true
  next
  $bad = true			# should not reach here
}
test_ok(!$bad)

done = false
$bad = false
loop {
  break if done
  done = true
  redo
  $bad = true			# should not reach here
}
test_ok(!$bad)

$x = []
for i in 1 .. 7
  $x.push i
end
test_ok($x.size == 7)
test_ok($x == [1, 2, 3, 4, 5, 6, 7])

$done = false
$x = []
for i in 1 .. 7			# see how retry works in iterator loop
  if i == 4 and not $done
    $done = true
    retry
  end
  $x.push(i)
end
test_ok($x.size == 10)
test_ok($x == [1, 2, 3, 1, 2, 3, 4, 5, 6, 7])

# append method to built-in class
class Array
  def iter_test1
    collect{|e| [e, yield(e)]}.sort{|a,b|a[1]<=>b[1]}
  end
  def iter_test2
    a = collect{|e| [e, yield(e)]}
    a.sort{|a,b|a[1]<=>b[1]}
  end
end
$x = [[1,2],[3,4],[5,6]]
test_ok($x.iter_test1{|x|x} == $x.iter_test2{|x|x})

class IterTest
  def initialize(e); @body = e; end

  def each0(&block); @body.each(&block); end
  def each1(&block); @body.each { |*x| block.call(*x) } end
  def each2(&block); @body.each { |*x| block.call(x) } end
  def each3(&block); @body.each { |x| block.call(*x) } end
  def each4(&block); @body.each { |x| block.call(x) } end
  def each5; @body.each { |*x| yield(*x) } end
  def each6; @body.each { |*x| yield(x) } end
  def each7; @body.each { |x| yield(*x) } end
  def each8; @body.each { |x| yield(x) } end
end

IterTest.new([0]).each0 { |x| $x = x }
test_ok($x == 0)
IterTest.new([1]).each1 { |x| $x = x }
test_ok($x == 1)
IterTest.new([2]).each2 { |x| $x = x }
test_ok($x == [2])
IterTest.new([3]).each3 { |x| $x = x }
test_ok($x == 3)
IterTest.new([4]).each4 { |x| $x = x }
test_ok($x == 4)
IterTest.new([5]).each5 { |x| $x = x }
test_ok($x == 5)
IterTest.new([6]).each6 { |x| $x = x }
test_ok($x == [6])
IterTest.new([7]).each7 { |x| $x = x }
test_ok($x == 7)
IterTest.new([8]).each8 { |x| $x = x }
test_ok($x == 8)

IterTest.new([[0]]).each0 { |x| $x = x }
test_ok($x == [0])
IterTest.new([[1]]).each1 { |x| $x = x }
test_ok($x == [1])
IterTest.new([[2]]).each2 { |x| $x = x }
test_ok($x == [[2]])
IterTest.new([[3]]).each3 { |x| $x = x }
test_ok($x == 3)
IterTest.new([[4]]).each4 { |x| $x = x }
test_ok($x == [4])
IterTest.new([[5]]).each5 { |x| $x = x }
test_ok($x == [5])
IterTest.new([[6]]).each6 { |x| $x = x }
test_ok($x == [[6]])
IterTest.new([[7]]).each7 { |x| $x = x }
test_ok($x == 7)
IterTest.new([[8]]).each8 { |x| $x = x }
test_ok($x == [8])

IterTest.new([[0,0]]).each0 { |x| $x = x }
test_ok($x == [0,0])
IterTest.new([[8,8]]).each8 { |x| $x = x }
test_ok($x == [8,8])


