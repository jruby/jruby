require 'test/unit'

class TestFlip < Test::Unit::TestCase
  def test_flip2_while
    result = []
    i = 0
    while i < 5
      result << (i if (i==1)..(i==3))
      i += 1
    end
    assert_equal([nil,1,2,3,nil], result)
  end

  def test_flip2_for
    result = []
    for i in (0...5)
      result << (i if (i==1)..(i==3))
    end
    assert_equal([nil,1,2,3,nil], result)
  end

  def test_flip2_proc
    sub = proc {|x| x if (x==1)..(x==3)}
    assert_nil(sub.call(0))
    assert_equal(1, sub.call(1))
    assert_equal(2, sub.call(2))
    assert_equal(3, sub.call(3))
    assert_nil(sub.call(3))
    assert_nil(sub.call(4))
  end

  def test_flip2_eval
    sub = eval("proc {|x| x if (x==1)..(x==3)}")
    assert_nil(sub.call(0))
    assert_equal(1, sub.call(1))
    assert_equal(2, sub.call(2))
    assert_equal(3, sub.call(3))
    assert_nil(sub.call(3))
    assert_nil(sub.call(4))
  end

  def test_flip2_string
    sub = proc {|x| "#{x if (x==1)..(x==3)}"}
    assert_equal("", sub.call(0))
    assert_equal("1", sub.call(1))
    assert_equal("2", sub.call(2))
    assert_equal("3", sub.call(3))
    assert_equal("", sub.call(3))
    assert_equal("", sub.call(4))
  end

  def test_flip2_thread
    sub = []
    2.times do |i|
      Thread.start {
        sub << proc do |x|
          x if (x==1)...(x==3)
        end
      }.join
    end
    assert_nil(sub[0].call(0))
    assert_equal(1, sub[0].call(1))
    assert_nil(sub[1].call(2))
    assert_nil(sub[1].call(3))
    assert_equal(1, sub[1].call(1))
    assert_equal(3, sub[0].call(3))
    assert_nil(sub[0].call(3))
    assert_equal(3, sub[1].call(3))
  end
end