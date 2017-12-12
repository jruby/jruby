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
end
