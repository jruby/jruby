require 'java'
require 'test/unit'

# Split-constructor (Ruby initialize on a Java subclass) super forwarding behaviour
class TestJavaCtor < Test::Unit::TestCase
  # --- arity enforcement ---
  # Direct super forwarding and terminal literal super fast paths must still enforce Ruby arity;
  # with a wrong arg count construction falls back to the split interpreter which raises ArgumentError
  # (see ExitableInterpreterContext#directSuperForwardable / #terminalLiteralSuperForwardable)

  class ZSuperRest < java.util.ArrayList
    def initialize(first, *rest)
      super
    end
  end

  def test_zsuper_with_rest_enforces_required_arity
    assert_raise(ArgumentError) { ZSuperRest.new }
    assert_equal 0, ZSuperRest.new(10).size
    # cached plan after a successful construction must still enforce arity
    assert_raise(ArgumentError) { ZSuperRest.new }
  end

  class FixedForward < java.util.ArrayList
    def initialize(a)
      super(a)
    end
  end

  def test_fixed_args_forwarding_enforces_arity
    assert_equal 0, FixedForward.new(10).size
    assert_raise(ArgumentError) { FixedForward.new }
    assert_raise(ArgumentError) { FixedForward.new(1, 2) }
  end

  class LiteralSuper < java.util.ArrayList
    def initialize
      super(7)
    end
  end

  def test_terminal_literal_super_enforces_arity
    assert_equal 0, LiteralSuper.new.size
    # cached literal-super terminator must not swallow extra args
    assert_raise(ArgumentError) { LiteralSuper.new(1) }
  end

  class NoArgsSuper < java.util.ArrayList
    def initialize
      super()
    end
  end

  def test_no_args_super_enforces_arity
    assert_equal 0, NoArgsSuper.new.size
    assert_raise(ArgumentError) { NoArgsSuper.new(1) }
  end

  class RestOnly < java.util.ArrayList
    def initialize(*args)
      super(*args)
    end
  end

  def test_rest_only_accepts_any_arity
    assert_equal 0, RestOnly.new.size
    assert_equal 0, RestOnly.new(10).size
  end

end
