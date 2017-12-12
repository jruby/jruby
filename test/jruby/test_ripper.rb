# encoding: utf-8

require 'test/unit'
require 'ripper'

class TestJRubyRipper < Test::Unit::TestCase
  class ExtractRipper < Ripper::SexpBuilderPP
    def attach(method, &block)
      class << self; self; end.class_eval do
        m = method
        define_method(m) do |*args|
          yield(m, *args)
        end
      end
      self
    end
  end

  def extract(source, method)
    ret = nil
    ExtractRipper.new(source).attach(method) { |*args| ret ||= args }.parse
    ret
  end

  def test_invalid_bytecode
    assert_equal nil, Ripper.sexp("\xae")
    assert_equal nil, Ripper.sexp("\xaeb")
    assert_equal nil, Ripper.sexp("a\xae")
    assert_equal nil, Ripper.sexp("a\xae = 0")
  end

  def test_opt_bv_decl
    assert_equal nil, extract("p{||}", :on_block_var).last
    assert_equal false, extract("p{|a|}", :on_block_var).last
  end

  def test_block_local_var_ref
    assert_equal [:on_var_ref, [:@ident, "a", [1, 5]]], extract("p{|a|a}", :on_var_ref)
  end

  def test_var_ref
    assert_equal [:on_var_ref, [:@ident, "a", [1, 9]]], extract("p{|(a,b)|a}", :on_var_ref)
    assert_equal [:on_var_ref, [:@ident, "a", [1, 7]]], extract("p{|a=1|a}", :on_var_ref)
    assert_equal [:on_var_ref, [:@ident, "a", [1, 9]]], extract("p{|a=1+1|a}", :on_var_ref)
    assert_equal [:on_var_ref, [:@ident, "a", [1, 6]]], extract("p{a=1;a}", :on_var_ref)
    assert_equal [:on_var_ref, [:@ident, "a", [1, 6]]], extract("p{|&a|a}", :on_var_ref)
  end
end
