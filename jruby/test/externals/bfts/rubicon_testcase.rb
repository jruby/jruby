require "test/unit/testcase"

# TODO: rename this... drop rubicon lineage.
class RubiconTestCase < Test::Unit::TestCase

  MsWin32 = :gak # TODO: fix
  JRuby = :gak
  $os = :not_gak

  VERSION = defined?(RUBY_VERSION) ? RUBY_VERSION : VERSION
  def ruby_version
    RubiconTestCase::VERSION
  end

  def test_nathanial_talbott_is_my_archenemy
    # do nothing but appease nathanial's inability to envision
    # abstract test classes... stabity stab stab
  end

  # TODO: this is overly complicated and dumb
  def truth_table(method, *result)
    for a in [ false, true ]
      expected = result.shift
      assert_equal(expected, method.call(a))
      assert_equal(expected, method.call(a ? self : nil))
    end
  end
end
