# encoding: utf-8

require 'test/unit'
require 'ripper'

class TestJRubyRipper < Test::Unit::TestCase
  def test_invalid_bytecode
    assert_equal nil, Ripper.sexp("\xae")
    assert_equal nil, Ripper.sexp("\xaeb")
    assert_equal nil, Ripper.sexp("a\xae")
    assert_equal nil, Ripper.sexp("a\xae = 0")
  end
end
