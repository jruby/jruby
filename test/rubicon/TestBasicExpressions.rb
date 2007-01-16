$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'

class TestBasicExpressions < Rubicon::TestCase

  # ------------------------------------------------------ Alias
  class Alias0
    def foo; "foo" end
  end
  class Alias1<Alias0
    alias bar foo
    def foo; "foo+" + super end
  end
  class Alias2<Alias1
    alias baz foo
    undef foo
  end

  def testAlias1
    x = Alias2.new
    assert_equal("foo", x.bar)
    assert_equal("foo+foo", x.baz)

    # test_check for cache
    assert_equal("foo+foo", x.baz)
  end

  class Alias3<Alias2
    def foo
      defined? super
    end
    def bar
      defined? super
    end
    def quux
      defined? super
    end
  end

  def testAlias2
    x = Alias3.new
    assert(!x.foo)
    assert_not_nil(x.bar)
    assert(!x.quux)
  end

  # ------------------------------------------------------ defined?

  def definedHelper
    return !defined?(yield)
  end

  def testDefined?
    $x = 123
    assert_not_nil(defined?($x))
    assert_equal('global-variable', defined?($x))

    foo=5
    assert_not_nil(defined?(foo))

    assert_not_nil(defined?(Array))
    assert_not_nil(defined?(Object.new))
    assert(!defined?(Object.print))	# private method
    assert_not_nil(defined?(1 == 2))		# operator expression

    assert_not_nil(definedHelper)		# not iterator
    assert_not_nil(!definedHelper {})		# not iterator
  end

end

# Run these tests if invoked directly

Rubicon::handleTests(TestBasicExpressions) if $0 == __FILE__
