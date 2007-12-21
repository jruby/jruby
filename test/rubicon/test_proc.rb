require 'test/unit'


class TestProc < Test::Unit::TestCase

  def procFrom
    Proc.new
  end

  def test_AREF # '[]'
    a = Proc.new { |x| "hello #{x}" }
    assert_equal("hello there", a["there"])
  end

  def test_arity
    tests = [
      [Proc.new {          }, -1],
      [Proc.new { |x,y|    },  2],
      [Proc.new { |x,y,z|  },  3],
      [Proc.new { |*z|     }, -1],
      [Proc.new { |x,*z|   }, -2],
      [Proc.new { |x,y,*z| }, -3],
    ]

      tests <<
        [Proc.new { ||       }, 0] <<
        [Proc.new { |x|      }, 1]

	expected = []
	actual = tests.map do |proc, exp_arity| 
		expected << exp_arity
		proc.arity
	end
	assert_equal(expected, actual)
  end

  def test_call
    a = Proc.new { |x| "hello #{x}" }
    assert_equal("hello there", a.call("there"))
  end

  def test_s_new
    a = procFrom { "hello" }
    assert_equal("hello", a.call)
    a = Proc.new { "there" }
    assert_equal("there", a.call)
  end
  
  def test_to_s
    a = Proc.new {}
    assert(a.to_s[__FILE__ + ":50"])
  end

end
