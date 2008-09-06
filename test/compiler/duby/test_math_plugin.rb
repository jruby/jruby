require 'test/unit'
require 'duby'
require 'duby/plugin/math'

class TestMathPlugin < Test::Unit::TestCase
  include Duby
  
  def test_plus
    ast = AST.parse("1 + 1")
    typer = Typer::Simple.new :bar
    
    ast.infer(typer)
    assert_nothing_raised {typer.resolve(true)}
  end
  
  def test_minus
    ast = AST.parse("1 - 1")
    typer = Typer::Simple.new :bar
    
    ast.infer(typer)
    assert_nothing_raised {typer.resolve(true)}
  end
  
  def test_times
    ast = AST.parse("1 * 1")
    typer = Typer::Simple.new :bar
    
    ast.infer(typer)
    assert_nothing_raised {typer.resolve(true)}
  end
  
  def test_divide
    ast = AST.parse("1 / 1")
    typer = Typer::Simple.new :bar
    
    ast.infer(typer)
    assert_nothing_raised {typer.resolve(true)}
  end
end