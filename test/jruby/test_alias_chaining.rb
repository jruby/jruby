require 'test/unit'
require 'jruby'

# Aliasing an alias used to wrap it instead of pointing where it pointed, so aliasing a
# method away and back grew a chain that never shrank until the call ran out of stack.
class TestAliasChaining < Test::Unit::TestCase
  ALIAS_METHOD = org.jruby.internal.runtime.methods.AliasMethod

  def test_aliasing_a_method_away_and_back_does_not_grow_a_chain
    klass = Class.new { def value = "ok" }

    5.times do
      klass.module_eval do
        alias_method :saved, :value
        remove_method :value
        alias_method :value, :saved
        remove_method :saved
      end
    end

    assert_equal 1, alias_depth(klass, "value")
    assert_equal "ok", klass.new.value
  end

  private

  def alias_depth(klass, name)
    method = JRuby.reference(klass).search_method(name)
    depth = 0
    while method.is_a?(ALIAS_METHOD)
      depth += 1
      method = method.entry.method
    end
    depth
  end
end
