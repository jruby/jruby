require 'test/unit'

class TestDupCloneFreeze < Test::Unit::TestCase

  def test_freeze_dup_not_frozen
    assert(!Module.new.freeze.dup.frozen?)
    assert(!Class.new.freeze.dup.frozen?)
    assert(!Object.new.freeze.dup.frozen?)
    assert(!String.new.freeze.dup.frozen?)
    assert(![].freeze.dup.frozen?)
    assert(!{}.freeze.dup.frozen?)
    assert(!//.freeze.dup.frozen?)
  end

  def test_freeze_clone_still_frozen
    assert(Module.new.freeze.clone.frozen?)
    assert(Class.new.freeze.clone.frozen?)
    assert(Object.new.freeze.clone.frozen?)
    assert(String.new.freeze.clone.frozen?)
    assert([].freeze.clone.frozen?)
    assert({}.freeze.clone.frozen?)
    assert(//.freeze.clone.frozen?)
  end
  
  class Foo
    attr_reader :a
    def initialize
      @a = 2
    end
  	protected

	def initialize_copy(from)
      @a = 1
    end
  end
  
  def test_clone_protected_initialize_copy
    b = Foo.new.clone
    assert(1, b.a)
  end
end