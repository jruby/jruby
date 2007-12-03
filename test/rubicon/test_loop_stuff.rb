require 'test/unit'

class TestLoopStuff < Test::Unit::TestCase

  FILE = "while_tmp"

  def setup
    File.open(FILE, "w") do |tmp|
      assert(tmp.kind_of?(File))
      tmp.puts "tvi925"
      tmp.puts "tvi920"
      tmp.puts "vt100"
      tmp.puts "Amiga"
      tmp.puts "paper"
    end
  end

  def teardown
    File.unlink(FILE)
  end

  def testBreak
    File.open(FILE) do |tmp|
      while line = tmp.gets()
        break if /vt100/ =~ line
      end
      assert(!tmp.eof?)
      assert_not_nil(/vt100/ =~ line)
    end
  end
  
  def testBreakWithInterval
    File.open("while_tmp") do |tmp|
      while tmp.gets()
        break if 3
        assert($_ !~ /[vt100|Amiga|paper]/)
      end
    end
  end

  def testNext
    File.open(FILE) do |tmp|
      while line = tmp.gets()
        next if /vt100/ =~ line
        assert(/vt100/ !~ line)
      end
      assert(tmp.eof?)
      assert(/vt100/ !~ line)
    end
  end

  def testRedoWithWhile
    File.open(FILE) do |tmp|
      while tmp.gets()
        line = $_
        $_ = gsub(/vt100/, 'VT100')
        if $_ != line
          gsub!('VT100', 'Vt100')
          redo;
        end
        assert(/vt100/ !~ $_)
        assert(/VT100/ !~ $_)
      end
      assert(tmp.eof?)
    end
  end

  def testRedoWithFor
    sum = 0
    for i in 1..10
      sum += i
      i -= 1
      if i > 0
        redo
      end
    end
    assert_equal(220, sum)
  end

  def testUntil
    i = 0
    until i>4
      i+=1
    end
    assert(i>4)
  end


  ##########
  # Iterators
  #

  def testNoIterator
    assert(!iterator?)
  end

  def iteratorHelper
    iterator?
  end

  def testIterator
    assert(iteratorHelper {})
  end

  def testTopLevelYield
    assert(!defined?(yield))
  end

  def iterateOverArray
    x = [1, 2, 3, 4]
    y = []
    for i in x
      y.push i
    end
    assert_equal(x, y)
  end

  
  def nestedIteratorHelper
    1.upto(10) do |i|
      yield i
    end
  end

  def testNestedIterator
    i = 0
    nestedIteratorHelper { |i| break if i == 5 }
    assert_equal(5, i)
  end

  if false
    # what is this actually testing???
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
    
  end


  # iterator break/redo/next/retry
  
  def testIteratorBreak
    done = true
    loop do
      break
      done = false			# should not reach here
    end
    assert(done)
  end

  def testIteratorNext
    done = false
    loop do
      break if done
      done = true
      next
      fail("shouldn't read here")
    end
    assert(true)
  end

  def testIteratorRedo
    done = false
    loop do
      break if done
      done = true
      redo
      fail("should not reach here")
    end
    assert(true)
  end

  # CON: I've disabled retry in methods and blocks because of JRUBY-1629 and
  # JRUBY-1522 for now; unfortunately retry is looking like something we can't
  # reasonably support. 

#  def testIteratorRetry
#    done = false
#    x = []
#    for i in 1 .. 7			# see how retry works in iterator loop
#      if i == 4 and not done
#        done = true
#        retry
#      end
#      x.push(i)
#    end
#    assert_equal(10, x.size)
#    assert_equal([1, 2, 3, 1, 2, 3, 4, 5, 6, 7], x)
#  end


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

  def testClassFullOfIterators
    x = nil
    IterTest.new([0]).each0 { |x| x = x }
    assert_equal(0, x)
    IterTest.new([1]).each1 { |x| x = x }
    assert_equal(1, x)
    IterTest.new([2]).each2 { |x| x = x }
    assert_equal([2], x)
    IterTest.new([3]).each3 { |x| x = x }
    assert_equal(3, x)
    IterTest.new([4]).each4 { |x| x = x }
    assert_equal(4, x)
    IterTest.new([5]).each5 { |x| x = x }
    assert_equal(5, x)
    IterTest.new([6]).each6 { |x| x = x }
    assert_equal([6], x)
    IterTest.new([7]).each7 { |x| x = x }
    assert_equal(7, x)
    IterTest.new([8]).each8 { |x| x = x }
    assert_equal(8, x)
    
    IterTest.new([[0]]).each0 { |x| x = x }
    assert_equal([0], x)
    IterTest.new([[1]]).each1 { |x| x = x }
    assert_equal([1], x)
    IterTest.new([[2]]).each2 { |x| x = x }
    assert_equal([[2]], x)
    IterTest.new([[3]]).each3 { |x| x = x }
    assert_equal(3, x)
    IterTest.new([[4]]).each4 { |x| x = x }
    assert_equal([4], x)
    IterTest.new([[5]]).each5 { |x| x = x }
    assert_equal([5], x)
    IterTest.new([[6]]).each6 { |x| x = x }
    assert_equal([[6]], x)
    IterTest.new([[7]]).each7 { |x| x = x }
    assert_equal(7, x)
    IterTest.new([[8]]).each8 { |x| x = x }
    assert_equal([8], x)
    
    IterTest.new([[0,0]]).each0 { |x| x = x }
    assert_equal([0, 0], x)
    IterTest.new([[8,8]]).each8 { |x| x = x }
    assert_equal([8, 8], x)
  end

=begin This fails in MRI...
  def testAppendIteratorToBuiltin
    x = [[1,2],[3,4],[5,6]]
    assert_equal(x.iter_test1{|x|x}, x.iter_test2{|x|x})
  end
=end
end

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
