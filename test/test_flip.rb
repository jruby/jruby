require 'test/unit'

class FlipTest < Test::Unit::TestCase
  # flip (taken from post and comments at http://redhanded.hobix.com/inspect/hopscotchingArraysWithFlipFlops.html)
  def test_skip_one
    s = true
    a = (1..10).reject { true if (s = !s) .. (s) }
    assert_equal([1, 3, 5, 7, 9], a)
  end
  
  def test_skip_two
    s = true
    a = (1..10).reject { true if (s = !s) .. (s = !s) }
    assert_equal([1, 4, 7, 10], a)
  end
  
  def test_skip_two_threedot
    s = true
    a = (1..10).reject { true if (s = !s) ... (s) }
    assert_equal([1, 4, 7, 10], a)
  end
  
  def test_skip_four_two_and_threedot
    s = true
    a = (1..20).reject { true if (s = !s) .. (s = !s) and (s = !s) ... (s) }
    if RUBY_VERSION =~ /1\.9/
      assert_equal([:s, :a], local_variables)
    else
      assert_equal(['s', 'a'], local_variables)
    end
    assert_equal([1, 5, 9, 13, 17], a)
  end
  
  def test_big_flip
    s = true;
    a = (1..10).inject([]) do |ary, v|
      ary << [] unless (s = !s) .. (s = !s)
      ary.last << v
      ary
    end
    assert_equal([[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]], a)
  end
  
  def test_big_tripledot_flip
    s = true
    a = (1..64).inject([]) do |ary, v|
        unless (s ^= v[2].zero?)...(s ^= !v[1].zero?)
            ary << []
        end
        ary.last << v
        ary
    end
    expected = [[1, 2, 3, 4, 5, 6, 7, 8],
          [9, 10, 11, 12, 13, 14, 15, 16],
          [17, 18, 19, 20, 21, 22, 23, 24],
          [25, 26, 27, 28, 29, 30, 31, 32],
          [33, 34, 35, 36, 37, 38, 39, 40],
          [41, 42, 43, 44, 45, 46, 47, 48],
          [49, 50, 51, 52, 53, 54, 55, 56],
          [57, 58, 59, 60, 61, 62, 63, 64]]
    assert_equal(expected, a)
  end
  
  def test_flip_in_conditional
    assert_equal(0, (true..false) ? 0 : 1)
    assert_equal(1, (false..true) ? 0 : 1)
    assert_equal(0, (5..8) ? 0 : 1)
    assert_equal(0, (Object.new..Object.new) ? 0 : 1)
  end
  
  # JRUBY-2046
  def test_flip_in_conditional_in_eval
    # We need at least 2 vars, or 0,
    # in order to trigger the bug.
    extra_var = nil

    expected = nil
    eval("expected = (true..false) ? 0 : 1")
    assert_equal(0, expected)

    expected = nil
    eval("expected = (false..true) ? 0 : 1")
    assert_equal(1, expected)

    expected = nil
    eval("expected = (5..8) ? 0 : 1")
    assert_equal(0, expected)
    
    expected = nil
    eval("expected = (Object.new..Object.new) ? 0 : 1")
    assert_equal(0, expected)
  end

  # JRUBY-2046
  def test_flip_in_eval_in_method
    def method
      eval("(true..false) ? 1 : 0")
    end
    assert_equal(1, method)
  end

  # JRUBY-2046
  def test_flip_in_class_eval
    assert_equal(1, Object.class.class_eval("(true..false) ? 1 : 0"))
  end

  # JRUBY-2046
  def test_flip_in_instance_eval
    assert_equal(1, Object.new.instance_eval("(true..false) ? 1 : 0"))
  end

  # JRUBY-2046
  def test_flip_in_eval_in_instance_eval
    res = Object.new.instance_eval {
      eval("(true..false) ? 1 : 0")
    }
    assert_equal(1, res)
  end
  
  # JRUBY-2046
  def test_flip_in_eval_in_module
    res = eval("module M; eval('(true..false) ? 1 : 0'); end")
    assert_equal(1, res)
  end
end
