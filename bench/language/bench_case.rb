require 'benchmark'

class BenchCase
  def do_case1
    case 1; when 1; end
    case 1; when 1; end
    case 1; when 1; end
    case 1; when 1; end
    case 1; when 1; end
  end

  def do_case2
    case 1; when 3,2,1; end
    case 1; when 3,2,1; end
    case 1; when 3,2,1; end
    case 1; when 3,2,1; end
    case 1; when 3,2,1; end
  end

  def do_case3
    case 1; when 10; when 9; when 8; when 7; when 6; when 5; when 4; when 3; when 2; when 1; end
    case 1; when 10; when 9; when 8; when 7; when 6; when 5; when 4; when 3; when 2; when 1; end
    case 1; when 10; when 9; when 8; when 7; when 6; when 5; when 4; when 3; when 2; when 1; end
    case 1; when 10; when 9; when 8; when 7; when 6; when 5; when 4; when 3; when 2; when 1; end
    case 1; when 10; when 9; when 8; when 7; when 6; when 5; when 4; when 3; when 2; when 1; end
  end

  def do_case_single_char_str
    case 'j'; when 'a'; when 'b'; when 'c'; when 'd'; when 'e'; when 'f'; when 'g'; when 'h'; when 'i'; when 'j'; end
    case 'j'; when 'a'; when 'b'; when 'c'; when 'd'; when 'e'; when 'f'; when 'g'; when 'h'; when 'i'; when 'j'; end
    case 'j'; when 'a'; when 'b'; when 'c'; when 'd'; when 'e'; when 'f'; when 'g'; when 'h'; when 'i'; when 'j'; end
    case 'j'; when 'a'; when 'b'; when 'c'; when 'd'; when 'e'; when 'f'; when 'g'; when 'h'; when 'i'; when 'j'; end
    case 'j'; when 'a'; when 'b'; when 'c'; when 'd'; when 'e'; when 'f'; when 'g'; when 'h'; when 'i'; when 'j'; end
  end

  def do_case_multi_char_str
    case 'jx'; when 'ax'; when 'bx'; when 'cx'; when 'dx'; when 'ex'; when 'fx'; when 'gx'; when 'hx'; when 'ix'; when 'jx'; end
    case 'jx'; when 'ax'; when 'bx'; when 'cx'; when 'dx'; when 'ex'; when 'fx'; when 'gx'; when 'hx'; when 'ix'; when 'jx'; end
    case 'jx'; when 'ax'; when 'bx'; when 'cx'; when 'dx'; when 'ex'; when 'fx'; when 'gx'; when 'hx'; when 'ix'; when 'jx'; end
    case 'jx'; when 'ax'; when 'bx'; when 'cx'; when 'dx'; when 'ex'; when 'fx'; when 'gx'; when 'hx'; when 'ix'; when 'jx'; end
    case 'jx'; when 'ax'; when 'bx'; when 'cx'; when 'dx'; when 'ex'; when 'fx'; when 'gx'; when 'hx'; when 'ix'; when 'jx'; end
  end

  def do_case_nil_false_true_else
    case 1; when nil; when false; when true; else; end
    case 1; when nil; when false; when true; else; end
    case 1; when nil; when false; when true; else; end
    case 1; when nil; when false; when true; else; end
    case 1; when nil; when false; when true; else; end
  end

  def do_case_single_char_symbol
    case :j; when :a; when :b; when :c; when :d; when :e; when :f; when :g; when :h; when :i; when :j; end
    case :j; when :a; when :b; when :c; when :d; when :e; when :f; when :g; when :h; when :i; when :j; end
    case :j; when :a; when :b; when :c; when :d; when :e; when :f; when :g; when :h; when :i; when :j; end
    case :j; when :a; when :b; when :c; when :d; when :e; when :f; when :g; when :h; when :i; when :j; end
    case :j; when :a; when :b; when :c; when :d; when :e; when :f; when :g; when :h; when :i; when :j; end
  end

  def do_case_multi_char_symbol
    case :jx; when :ax; when :bx; when :cx; when :dx; when :ex; when :fx; when :gx; when :hx; when :ix; when :jx; end
    case :jx; when :ax; when :bx; when :cx; when :dx; when :ex; when :fx; when :gx; when :hx; when :ix; when :jx; end
    case :jx; when :ax; when :bx; when :cx; when :dx; when :ex; when :fx; when :gx; when :hx; when :ix; when :jx; end
    case :jx; when :ax; when :bx; when :cx; when :dx; when :ex; when :fx; when :gx; when :hx; when :ix; when :jx; end
    case :jx; when :ax; when :bx; when :cx; when :dx; when :ex; when :fx; when :gx; when :hx; when :ix; when :jx; end
  end
end

def bench_case(bm)
  bc = BenchCase.new

  bm.report "1m x5 cases, 1 fixnum when" do
    a = 0
    while a < 1_000_000
      bc.do_case1
      a += 1
    end
  end

  bm.report "1m x5 cases, 1 3-arg fixnum when" do
    a = 0
    while a < 1_000_000
      bc.do_case2
      a += 1
    end
  end

  bm.report "1m x5 cases, 10 fixnum whens" do
    a = 0
    while a < 1_000_000
      bc.do_case3
      a += 1
    end
  end

  bm.report "1m x5 cases, 10 one-char str whens" do
    a = 0
    while a < 1_000_000
      bc.do_case_single_char_str
      a += 1
    end
  end

  bm.report "1m x5 cases, 10 multi-char str whens" do
    a = 0
    while a < 1_000_000
      bc.do_case_multi_char_str
      a += 1
    end
  end

  bm.report "1m x5 cases, 10 one-char sym whens" do
    a = 0
    while a < 1_000_000
      bc.do_case_single_char_symbol
      a += 1
    end
  end

  bm.report "1m x5 cases, 10 multi-char sym whens" do
    a = 0
    while a < 1_000_000
      bc.do_case_multi_char_symbol
      a += 1
    end
  end

  bm.report "1m x5 cases, nil-false-true-else" do
    a = 0
    while a < 1_000_000
      bc.do_case_nil_false_true_else
      a += 1
    end
  end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_case(bm)} }
end
