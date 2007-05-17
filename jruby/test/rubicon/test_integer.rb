require 'test/unit'


class TestInteger < Test::Unit::TestCase

  def test_chr
    a = " " * 256
    0.upto(255) { |i| a[i] = i }

    0.upto(255) { |i| assert_equal(a[i,1], i.chr) }
  end

  def test_downto
    count = 0
    0.downto(1) { count += 1 }
    assert_equal(0, count)

    count = 0
    0.downto(0) { count += 1 }
    assert_equal(1, count)

    count = 0
    9.downto(0) { |i| count += i }
    assert_equal(45, count)

    count = 0
    0.downto(-9) { |i| count += i }
    assert_equal(-45, count)
  end

  def test_integer?
    assert(1.integer?)
    assert((10**50).integer?)
    assert(!1.0.integer?)
  end

  def test_next
    for i in -10..10 do assert_equal(i+1, i.next) end
    for i in [-(10**40), -(2**38), 10**40, 2**38 ]
      assert_equal(i+1, i.next) 
    end
  end

  def test_step
    vals = [1, 4, 7, 10 ]
    1.step(10, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [1, 4, 7, 10 ]
    1.step(12, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [10, 7, 4, 1 ]
    10.step(1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [10, 7, 4, 1 ]
    10.step(-1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [ 1 ]
    1.step(1, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [ 1 ]
    1.step(1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [  ]
    1.step(0, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)
  end

  def test_succ
    for i in -10..10 do assert_equal(i+1, i.succ) end
    for i in [-(10**40), -(2**38), 10**40, 2**38 ]
      assert_equal(i+1, i.succ) 
    end
  end

  def test_times
    count = 0
    (-1).times { count += 1 }
    assert_equal(0, count)

    count = 0
    0.times { count += 1 }
    assert_equal(0, count)

    count = 0
    10.times { |i| count += i }
    assert_equal(45, count)
  end

  def test_upto
    count = 0
    0.upto(-1)   { count += 1 }
    assert_equal(0, count)

    count = 0
    0.upto(0)    { count += 1 }
    assert_equal(1, count)

    count = 0
    0.upto(9)    { |i| count += i }
    assert_equal(45, count)

    count = 0
    (-9).upto(0) { |i| count += i }
    assert_equal(-45, count)
  end

  def test_s_induced_from
    assert_equal(1, Integer.induced_from(1))
    assert_equal(1, Integer.induced_from(1.0))
  end

end
