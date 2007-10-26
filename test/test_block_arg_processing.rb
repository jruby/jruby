require 'test/unit'

class TestVarArgBlock < Test::Unit::TestCase
  def blockyield(arg)
    yield arg
  end
  
  def blockarg(arg, &b)
    b.call(arg)
  end
  
  def test_vararg_blocks
    Proc.new { |*element| assert_equal [["a"]], element }.call( ["a"] )
    Proc.new { |*element| assert_equal ["a"], element }.call( "a" )
    proc { |*element| assert_equal [["a"]], element }.call( ["a"] )
    proc { |*element| assert_equal ["a"], element }.call( "a" )
    lambda { |*element| assert_equal [["a"]], element }.call( ["a"] )
    lambda { |*element| assert_equal ["a"], element }.call( "a" )
    blockyield(["a"]) { |*element| assert_equal [["a"]], element }
    blockyield("a") { |*element| assert_equal ["a"], element }
    blockyield(["a"], &Proc.new { |*element| assert_equal [["a"]], element })
    blockyield("a", &Proc.new { |*element| assert_equal ["a"], element })
    blockyield(["a"], &proc { |*element| assert_equal [["a"]], element })
    blockyield("a", &proc { |*element| assert_equal ["a"], element })
    blockyield(["a"], &lambda { |*element| assert_equal [["a"]], element })
    blockyield("a", &lambda { |*element| assert_equal ["a"], element })
    blockarg(["a"]) { |*element| assert_equal [["a"]], element }
    blockarg("a") { |*element| assert_equal ["a"], element }
    blockarg(["a"], &Proc.new { |*element| assert_equal [["a"]], element })
    blockarg("a", &Proc.new { |*element| assert_equal ["a"], element })
    blockarg(["a"], &proc { |*element| assert_equal [["a"]], element })
    blockarg("a", &proc { |*element| assert_equal ["a"], element })
    blockarg(["a"], &lambda { |*element| assert_equal [["a"]], element })
    blockarg("a", &lambda { |*element| assert_equal ["a"], element })
  end
  
  def test_requiredarg_blocks
    Proc.new { |element| assert_equal ["a"], element }.call( ["a"] )
    Proc.new { |element| assert_equal "a", element }.call( "a" )
    proc { |element| assert_equal ["a"], element }.call( ["a"] )
    proc { |element| assert_equal "a", element }.call( "a" )
    lambda { |element| assert_equal ["a"], element }.call( ["a"] )
    lambda { |element| assert_equal "a", element }.call( "a" )
    blockyield(["a"]) { |element| assert_equal ["a"], element }
    blockyield("a") { |element| assert_equal "a", element }
    blockyield(["a"], &Proc.new { |element| assert_equal ["a"], element })
    blockyield("a", &Proc.new { |element| assert_equal "a", element })
    blockyield(["a"], &proc { |element| assert_equal ["a"], element })
    blockyield("a", &proc { |element| assert_equal "a", element })
    blockyield(["a"], &lambda { |element| assert_equal ["a"], element })
    blockyield("a", &lambda { |element| assert_equal "a", element })
    blockarg(["a"]) { |element| assert_equal ["a"], element }
    blockarg("a") { |element| assert_equal "a", element }
    blockarg(["a"], &Proc.new { |element| assert_equal ["a"], element })
    blockarg("a", &Proc.new { |element| assert_equal "a", element })
    blockarg(["a"], &proc { |element| assert_equal ["a"], element })
    blockarg("a", &proc { |element| assert_equal "a", element })
    blockarg(["a"], &lambda { |element| assert_equal ["a"], element })
    blockarg("a", &lambda { |element| assert_equal "a", element })
  end
  
  def test_requiredargs_blocks
    Proc.new { |element, a| assert_equal "a", element }.call( ["a"] )
    Proc.new { |element, a| assert_equal "a", element }.call( "a" )
    assert_raises(ArgumentError) {
      proc { |element, a| assert_equal ["a"], element }.call( ["a"] )
    }
    assert_raises(ArgumentError) {
      proc { |element, a| assert_equal "a", element }.call( "a" )
    }
    assert_raises(ArgumentError) {
      lambda { |element, a| assert_equal ["a"], element }.call( ["a"] )
    }
    assert_raises(ArgumentError) {
      lambda { |element, a| assert_equal "a", element }.call( "a" )
    }
    blockyield(["a"]) { |element, a| assert_equal "a", element }
    blockyield("a") { |element, a| assert_equal "a", element }
    blockyield(["a"], &Proc.new { |element, a| assert_equal "a", element })
    blockyield("a", &Proc.new { |element, a| assert_equal "a", element })
    blockyield(["a"], &proc { |element, a| assert_equal "a", element })
    blockyield("a", &proc { |element, a| assert_equal "a", element })
    blockyield(["a"], &lambda { |element, a| assert_equal "a", element })
    blockyield("a", &lambda { |element, a| assert_equal "a", element })
    blockarg(["a"]) { |element, a| assert_equal "a", element }
    blockarg("a") { |element, a| assert_equal "a", element }
    blockarg(["a"], &Proc.new { |element, a| assert_equal "a", element })
    blockarg("a", &Proc.new { |element, a| assert_equal "a", element })
    assert_raises(ArgumentError) {
      blockarg(["a"], &proc { |element, a| assert_equal ["a"], element })
    }
    assert_raises(ArgumentError) {
      blockarg("a", &proc { |element, a| assert_equal "a", element })
    }
    assert_raises(ArgumentError) {
      blockarg(["a"], &lambda { |element, a| assert_equal ["a"], element })
    }
    assert_raises(ArgumentError) {
      blockarg("a", &lambda { |element, a| assert_equal "a", element })
    }
  end
  
  def check_all_definemethods(obj)
    results = obj.foo1 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo2 "a"
    assert_equal(results[0], results[1])
    results = obj.foo3 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo4 "a"
    assert_equal(results[0], results[1])
    results = obj.foo5 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo6 "a"
    assert_equal(results[0], results[1])
    results = obj.foo7 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo8 "a"
    assert_equal(results[0], results[1])
    results = obj.foo9 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo10 "a"
    assert_equal(results[0], results[1])
    results = obj.foo11 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo12 "a"
    assert_equal(results[0], results[1])
    results = obj.foo13 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo14 "a"
    assert_equal(results[0], results[1])
  end
  
  def check_requiredargs_definemethods(obj)
    results = obj.foo1 ["a"]
    assert_equal(results[0], results[1])
    results = obj.foo2 "a"
    assert_equal(results[0], results[1])
    assert_raises(ArgumentError) { results = obj.foo3 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo4 "a" }
    assert_raises(ArgumentError) { results = obj.foo5 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo6 "a" }
    assert_raises(ArgumentError) { results = obj.foo7 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo8 "a" }
    assert_raises(ArgumentError) { results = obj.foo9 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo10 "a" }
    assert_raises(ArgumentError) { results = obj.foo11 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo12 "a" }
    assert_raises(ArgumentError) { results = obj.foo13 ["a"] }
    assert_raises(ArgumentError) { results = obj.foo14 "a" }
  end
  
  def test_definemethods
    obj = Object.new
    
    class << obj
      define_method :foo1, Proc.new { |*element| [[["a"]], element] }
      define_method :foo2, Proc.new { |*element| [["a"], element] }
      define_method :foo3, proc { |*element| [[["a"]], element] }
      define_method :foo4, proc { |*element| [["a"], element] }
      define_method :foo5, lambda { |*element| [[["a"]], element] }
      define_method :foo6, lambda { |*element| [["a"], element] }
      define_method(:foo7) { |*element| [[["a"]], element] }
      define_method(:foo8) { |*element| [["a"], element] }
      define_method :foo9, &Proc.new { |*element| [[["a"]], element] }
      define_method :foo10, &Proc.new { |*element| [["a"], element] }
      define_method :foo11, &proc { |*element| [[["a"]], element] }
      define_method :foo12, &proc { |*element| [["a"], element] }
      define_method :foo13, &lambda { |*element| [[["a"]], element] }
      define_method :foo14, &lambda { |*element| [["a"], element] }
    end
    
    check_all_definemethods(obj)
    
    class << obj
      define_method :foo1, Proc.new { |element| [["a"], element] }
      define_method :foo2, Proc.new { |element| ["a", element] }
      define_method :foo3, proc { |element| [["a"], element] }
      define_method :foo4, proc { |element| ["a", element] }
      define_method :foo5, lambda { |element| [["a"], element] }
      define_method :foo6, lambda { |element| ["a", element] }
      define_method(:foo7) { |element| [["a"], element] }
      define_method(:foo8) { |element| ["a", element] }
      define_method :foo9, &Proc.new { |element| [["a"], element] }
      define_method :foo10, &Proc.new { |element| ["a", element] }
      define_method :foo11, &proc { |element| [["a"], element] }
      define_method :foo12, &proc { |element| ["a", element] }
      define_method :foo13, &lambda { |element| [["a"], element] }
      define_method :foo14, &lambda { |element| ["a", element] }
    end
    
    check_all_definemethods(obj)
    
    class << obj
      define_method :foo1, Proc.new { |element, a| ["a", element] }
      define_method :foo2, Proc.new { |element, a| ["a", element] }
      define_method :foo3, proc { |element, a| [["a"], element] }
      define_method :foo4, proc { |element, a| ["a", element] }
      define_method :foo5, lambda { |element, a| [["a"], element] }
      define_method :foo6, lambda { |element, a| ["a", element] }
      define_method(:foo7) { |element, a| [["a"], element] }
      define_method(:foo8) { |element, a| ["a", element] }
      define_method :foo9, &Proc.new { |element, a| [["a"], element] }
      define_method :foo10, &Proc.new { |element, a| ["a", element] }
      define_method :foo11, &proc { |element, a| [["a"], element] }
      define_method :foo12, &proc { |element, a| ["a", element] }
      define_method :foo13, &lambda { |element, a| [["a"], element] }
      define_method :foo14, &lambda { |element, a| ["a", element] }
    end
    
    check_requiredargs_definemethods(obj)
  end
end

