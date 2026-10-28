# encoding: UTF-8
require 'test/unit'
require 'test/jruby/test_helper'

# GH-9591
#
# BuildCompoundStringInstr#simplifyInstr folds a compound string into a single
# literal. It used to assign CR_VALID without scanning the bytes. An ASCII-only
# string must be CR_7BIT, otherwise String#hash disagrees with String#eql? and
# uniq/Hash/Set split byte-identical strings.
#
# The interpreter builds these strings at run time and scans them, so the bug
# only appeared once a method was compiled. Each test therefore runs in a child
# process with the compiler forced on.
class TestCompoundStringCodeRange < Test::Unit::TestCase
  include TestHelper

  FORCE_COMPILE = { "jruby.compile.mode" => "FORCE" }

  def test_folded_integer_interpolation_is_ascii_only
    assert_equal 'true', run_compiled('print "abc#{1}def".ascii_only?')
  end

  def test_folded_symbol_interpolation_is_ascii_only
    assert_equal 'true', run_compiled('print "abc#{:sym}def".ascii_only?')
  end

  def test_folded_float_interpolation_is_ascii_only
    assert_equal 'true', run_compiled('print "abc#{1.5}def".ascii_only?')
  end

  def test_dedented_heredoc_is_ascii_only
    script = 'value = <<~EOS' "\n" \
             '  hello world' "\n" \
             'EOS' "\n" \
             'print value.ascii_only?'
    assert_equal 'true', run_compiled(script)
  end

  def test_folded_string_hash_agrees_with_eql
    script = 'folded = "abc#{1}def"' "\n" \
             'plain = "abc1def"' "\n" \
             'print [folded.eql?(plain), folded.hash == plain.hash, [folded, plain].uniq.size].inspect'
    assert_equal '[true, true, 1]', run_compiled(script)
  end

  # The fix must not mark real non-ASCII text as 7-bit.
  def test_folded_non_ascii_string_is_not_ascii_only
    script = 'value = "café#{1}!"' "\n" \
             'print [value.ascii_only?, value.valid_encoding?].inspect'
    assert_equal '[false, true]', run_compiled(script)
  end

  private

  def run_compiled(script)
    with_temp_script(script) { |f| jruby(f.path, FORCE_COMPILE) }
  end
end
