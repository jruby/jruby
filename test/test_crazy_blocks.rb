require 'test/unit'

class TestCrazyBlocks < Test::Unit::TestCase
  def foo(a)
    p = proc { a.each {|x| yield x } }
    1.times { p.call }
  end

  def bar(&b)
    [[1,2],[3,4]].each(&b)
  end

  def baz
    bar {|a| foo(a) { |x| yield x } }
  end

  def test_crazy
    a = []
    p = proc {|y| a << y}
    baz {|x| p.call x}
    assert_equal [1,2,3,4], a
  end

  def hello(&b)
    1.times { b.call }
  end

  def test_crazy2
    a = []
    hello {
      p = proc {|y| a << y}
      baz{|x| p.call x}
    }
    assert_equal [1,2,3,4], a
  end

  def test_crazy3
    a = []
    p = proc {|y| a << y}
    self.class.send(:define_method, :goodbye) {
      hello {
        baz {|x| p.call(x)}
      }
    }
    goodbye
    assert_equal [1,2,3,4], a
  end

  def test_crazy4
    a = []
    p = proc {|x| a << x}
    hello {
      p2 = proc {|$x| eval "p.call $x", p}
      baz {|x| eval "p2.call x"}
    }
    assert_equal [1,2,3,4], a
  end
end	
