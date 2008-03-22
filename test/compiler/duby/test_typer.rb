require 'test/unit'
require 'compiler/duby'
require 'compiler/duby/typer2'

class TestTyper < Test::Unit::TestCase
  include Compiler::Duby
  
  def test_fixnum
    ast = AST.parse("1")
    
    assert_equal(AST::TypeReference.new(:fixnum), ast.infer_type(SimpleTyper.new(:bar)))
  end
  
  def test_float
    ast = AST.parse("1.0")
    
    assert_equal(AST::TypeReference.new(:float), ast.infer_type(SimpleTyper.new(:bar)))
  end
  
  def test_string
    ast = AST.parse("'foo'")
    
    assert_equal(AST::TypeReference.new(:string), ast.infer_type(SimpleTyper.new(:bar)))
  end
  
  def test_body
    ast1 = AST.parse("'foo'; 1.0; 1")
    ast2 = AST.parse("begin; end")
    
    assert_equal(AST::TypeReference.new(:fixnum), ast1.infer_type(SimpleTyper.new(:bar)))
    assert_equal(nil, ast2.infer_type(SimpleTyper.new(:bar)))
  end
  
  def test_local
    ast1 = AST.parse("a = 1; a")
    typer = SimpleTyper.new :bar
    
    ast1.infer_type(typer)
    
    assert_equal(AST::TypeReference.new(:fixnum), typer.local_type(ast1, :a))
    assert_equal(AST::TypeReference.new(:fixnum), ast1.body.children[0].inferred_type)
    assert_equal(AST::TypeReference.new(:fixnum), ast1.body.children[1].inferred_type)
    
    ast2 = AST.parse("b = a = 1")
    ast2.infer_type(typer)
    
    assert_equal(AST::TypeReference.new(:fixnum), typer.local_type(ast2, :b))
    assert_equal(AST::TypeReference.new(:fixnum), ast2.body.children[0].inferred_type)
  end
  
  def test_signature
    ["def foo", "def self.foo"].each do |def_foo|
      ast1 = AST.parse("#{def_foo}(a); {a => :string}; end")
      typer = SimpleTyper.new :bar

      ast1.infer_type(typer)
      
      assert_raise(InferenceError) {typer.resolve_deferred(true)}
      assert_nothing_raised {typer.resolve_deferred}

      assert_equal(typer.default_type, typer.method_type(typer.self_type, :foo, [typer.string_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, :a))
      assert_equal(typer.default_type, ast1.body.inferred_type)
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); 1 end")
      typer = SimpleTyper.new :bar

      ast1.infer_type(typer)

      assert_equal(typer.fixnum_type, typer.method_type(typer.self_type, :foo, [typer.default_type]))
      assert_equal(typer.default_type, typer.local_type(ast1.body, :a))
      assert_equal(typer.fixnum_type, ast1.body.inferred_type)
      assert_equal(typer.default_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {a => :string}; a; end")
      typer = SimpleTyper.new :bar

      ast1.infer_type(typer)

      assert_equal(typer.string_type, typer.method_type(typer.self_type, :foo, [typer.string_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, :a))
      assert_equal(typer.string_type, ast1.body.inferred_type)
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {:return => :string}; end")
      typer = SimpleTyper.new :bar

      ast1.infer_type(typer)
      
      assert_raise(InferenceError) {typer.resolve_deferred(true)}

      ast1 = AST.parse("#{def_foo}(a); a = 'foo'; end")
      typer = SimpleTyper.new :bar

      ast1.infer_type(typer)

      assert_equal(typer.string_type, typer.method_type(typer.self_type, :foo, [typer.default_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, :a))
      assert_equal(typer.string_type, ast1.body.inferred_type)
      assert_equal(typer.default_type, ast1.body.arguments.args[0].inferred_type)
      
      typer.resolve_deferred
      
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)
    end
  end
  
  def test_call
    ast = AST.parse("1.foo(2)").body
    typer = SimpleTyper.new :bar
    
    typer.learn_method_type(typer.fixnum_type, :foo, [typer.fixnum_type], typer.string_type)
    assert_equal(typer.string_type, typer.method_type(typer.fixnum_type, :foo, [typer.fixnum_type]))
    
    ast.infer_type(typer)
    
    assert_equal(typer.string_type, ast.inferred_type)
    
    ast = AST.parse("def bar(a, b); {a => :fixnum, b => :string}; 1.0; end; def baz; bar(1, 'x'); end").body
    
    ast.infer_type(typer)
    
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :bar, [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :baz, []))
    assert_equal(typer.float_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # Reverse the order, ensure deferred inference succeeds
    ast = AST.parse("def baz; bar(1, 'x'); end; def bar(a, b); {a => :fixnum, b => :string}; 1.0; end").body
    typer = SimpleTyper.new(:bar)
    
    ast.infer_type(typer)
    
    assert_equal(typer.default_type, typer.method_type(typer.self_type, :baz, []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :bar, [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.default_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # allow resolution to run
    assert_nothing_raised {typer.resolve_deferred}
    
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :baz, []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :bar, [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.float_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # modify bar call to have bogus types, ensure resolution fails
    ast = AST.parse("def baz; bar(1, 1); end; def bar(a, b); {a => :fixnum, b => :string}; 1.0; end").body
    typer = SimpleTyper.new(:bar)
    
    ast.infer_type(typer)
    
    assert_equal(typer.default_type, typer.method_type(typer.self_type, :baz, []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, :bar, [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.default_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # allow resolution to run and produce error
    assert_raise(InferenceError) {typer.resolve_deferred(true)}
    inspected = "[FunctionalCall(bar)\n Fixnum(1)\n Fixnum(1), MethodDefinition(baz)\n {:return=>TypeReference(notype, array = false)}\n Arguments\n FunctionalCall(bar)\n  Fixnum(1)\n  Fixnum(1)]"
    assert_equal(inspected, typer.deferred_nodes.inspect)
  end
end