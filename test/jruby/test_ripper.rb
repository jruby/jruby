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

  def test_command_args
    assert_equal :command, extract("p m do; end", :on_method_add_block).dig(1, 0)
  end

  def test_heredoc
    assert_equal [:on_string_content, [:@tstring_content, "x\n", [2, 0]]], extract("<<EOS\nx\nEOS\n", :on_string_content)
    assert_equal [:on_string_content, [:string_embexpr, [[:vcall, [:@ident, "o", [2, 2]]]]], [:@tstring_content, "x\n", [2, 4]]], extract("<<EOS\n\#{o}x\nEOS\n", :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, "A ", [2, 2]], [:string_embexpr, [[:vcall, [:@ident, "a", [2, 6]]]]], [:@tstring_content, "\n", [2, 8]], [:@tstring_content, "", [3, 2]], [:string_embexpr, [[:vcall, [:@ident, "b", [3, 4]]]]], [:@tstring_content, "\n", [3, 6]]], extract("<<~EOS\n  A \#{a}\n  \#{b}\nEOS\n", :on_string_content)
  end

  def test_dyn_const_lhs
    assert_equal nil, Ripper.sexp("def m;C=s;end")
  end

  def test_literal_whitespace
    assert_equal [:on_string_content, [:@tstring_content, "\n", [1, 2]]], extract("%{\n}", :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, " ", [1, 2]]], extract("%[ ]", :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, "\t", [1, 2]]], extract("%(\t)", :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, "\r", [1, 2]]], extract("%|\r|", :on_string_content)
  end

  def test_invalid_gvar
    assert_equal [:on_string_content, [:@tstring_content, '# comment', [1, 1]]], extract('"# comment"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '#', [1, 1]]], extract('"#"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '##', [1, 1]]], extract('"##"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '#${', [1, 1]]], extract('"#${"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '#', [1, 1]], [:@tstring_content, '#${', [1, 2]]], extract('"##${"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, ' ', [1, 1]], [:@tstring_content, '#${', [1, 2]]], extract('" #${"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '#${ ', [1, 1]]], extract('"#${ "', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '# #', [1, 1]], [:@tstring_content, '#${', [1, 4]]], extract('"# ##${"', :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, "\#$}\n", [2, 0]]], extract("<<E\n\#$}\nE\n", :on_string_content)
    assert_equal [:on_string_content, [:@tstring_content, '# #', [2, 0]], [:@tstring_content, "\#${\n", [2, 3]]], extract("<<E\n\# \#\#${\nE\n", :on_string_content)
  end
end
