require 'test/unit'
require 'duby'

class TestTyper < Test::Unit::TestCase
  include Duby
  
  def test_fixnum
    ast = AST.parse("1")
    
    assert_equal(AST::TypeReference.new("fixnum"), ast.infer(Typer::Simple.new(:bar)))
  end
  
  def test_float
    ast = AST.parse("1.0")
    
    assert_equal(AST::TypeReference.new("float"), ast.infer(Typer::Simple.new(:bar)))
  end
  
  def test_string
    ast = AST.parse("'foo'")
    
    assert_equal(AST::TypeReference.new("string"), ast.infer(Typer::Simple.new(:bar)))
  end
  
  def test_boolean
    ast1 = AST.parse("true")
    ast2 = AST.parse("false")
    
    assert_equal(AST::TypeReference.new("boolean"), ast1.infer(Typer::Simple.new(:bar)))
    assert_equal(AST::TypeReference.new("boolean"), ast2.infer(Typer::Simple.new(:bar)))
  end
  
  def test_body
    ast1 = AST.parse("'foo'; 1.0; 1")
    ast2 = AST.parse("begin; end")
    
    assert_equal(AST::TypeReference.new("fixnum"), ast1.infer(Typer::Simple.new(:bar)))
    assert_equal(nil, ast2.infer(Typer::Simple.new(:bar)))
  end
  
  def test_local
    ast1 = AST.parse("a = 1; a")
    typer = Typer::Simple.new :bar
    
    ast1.infer(typer)
    
    assert_equal(AST::TypeReference.new("fixnum"), typer.local_type(ast1, 'a'))
    assert_equal(AST::TypeReference.new("fixnum"), ast1.body.children[0].inferred_type)
    assert_equal(AST::TypeReference.new("fixnum"), ast1.body.children[1].inferred_type)
    
    ast2 = AST.parse("b = a = 1")
    ast2.infer(typer)
    
    assert_equal(AST::TypeReference.new("fixnum"), typer.local_type(ast2, 'a'))
    assert_equal(AST::TypeReference.new("fixnum"), ast2.body.children[0].inferred_type)
  end
  
  def test_signature
    ["def foo", "def self.foo"].each do |def_foo|
      ast1 = AST.parse("#{def_foo}(a); {a => :string}; end")
      typer = Typer::Simple.new :bar

      ast1.infer(typer)
      
      assert_raise(Typer::InferenceError) {typer.resolve(true)}
      assert_nothing_raised {typer.resolve}

      assert_equal(typer.default_type, typer.method_type(typer.self_type, 'foo', [typer.string_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, 'a'))
      assert_equal(typer.default_type, ast1.body.inferred_type)
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); 1 end")
      typer = Typer::Simple.new :bar

      ast1.infer(typer)

      assert_equal(typer.fixnum_type, typer.method_type(typer.self_type, 'foo', [typer.default_type]))
      assert_equal(typer.default_type, typer.local_type(ast1.body, 'a'))
      assert_equal(typer.fixnum_type, ast1.body.inferred_type)
      assert_equal(typer.default_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {a => :string}; a; end")
      typer = Typer::Simple.new :bar

      ast1.infer(typer)

      assert_equal(typer.string_type, typer.method_type(typer.self_type, 'foo', [typer.string_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, 'a'))
      assert_equal(typer.string_type, ast1.body.inferred_type)
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {:return => :string}; end")
      typer = Typer::Simple.new :bar

      ast1.infer(typer)
      
      assert_raise(Typer::InferenceError) {typer.resolve(true)}

      ast1 = AST.parse("#{def_foo}(a); a = 'foo'; end")
      typer = Typer::Simple.new :bar

      ast1.infer(typer)

      assert_equal(typer.string_type, typer.method_type(typer.self_type, 'foo', [typer.default_type]))
      assert_equal(typer.string_type, typer.local_type(ast1.body, 'a'))
      assert_equal(typer.string_type, ast1.body.inferred_type)
      assert_equal(typer.default_type, ast1.body.arguments.args[0].inferred_type)
      
      typer.resolve
      
      assert_equal(typer.string_type, ast1.body.arguments.args[0].inferred_type)
    end
  end
  
  def test_call
    ast = AST.parse("1.foo(2)").body
    typer = Typer::Simple.new "bar"
    
    typer.learn_method_type(typer.fixnum_type, "foo", [typer.fixnum_type], typer.string_type)
    assert_equal(typer.string_type, typer.method_type(typer.fixnum_type, "foo", [typer.fixnum_type]))
    
    ast.infer(typer)
    
    assert_equal(typer.string_type, ast.inferred_type)
    
    ast = AST.parse("def bar(a, b); {a => :fixnum, b => :string}; 1.0; end; def baz; bar(1, 'x'); end").body
    
    ast.infer(typer)
    
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "bar", [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "baz", []))
    assert_equal(typer.float_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # Reverse the order, ensure deferred inference succeeds
    ast = AST.parse("def baz; bar(1, 'x'); end; def bar(a, b); {a => :fixnum, b => :string}; 1.0; end").body
    typer = Typer::Simple.new("bar")
    
    ast.infer(typer)
    
    assert_equal(typer.default_type, typer.method_type(typer.self_type, "baz", []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "bar", [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.default_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # allow resolution to run
    assert_nothing_raised {typer.resolve}
    
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "baz", []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "bar", [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.float_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # modify bar call to have bogus types, ensure resolution fails
    ast = AST.parse("def baz; bar(1, 1); end; def bar(a, b); {a => :fixnum, b => :string}; 1.0; end").body
    typer = Typer::Simple.new("bar")
    
    ast.infer(typer)
    
    assert_equal(typer.default_type, typer.method_type(typer.self_type, "baz", []))
    assert_equal(typer.float_type, typer.method_type(typer.self_type, "bar", [typer.fixnum_type, typer.string_type]))
    assert_equal(typer.default_type, ast.children[0].inferred_type)
    assert_equal(typer.float_type, ast.children[1].inferred_type)
    
    # allow resolution to run and produce error
    assert_raise(Typer::InferenceError) {typer.resolve(true)}
    inspected = "[FunctionalCall(bar)\n Fixnum(1)\n Fixnum(1), MethodDefinition(baz)\n {:return=>Type(notype)}\n Arguments\n FunctionalCall(bar)\n  Fixnum(1)\n  Fixnum(1)]"
    assert_equal(inspected, typer.deferred_nodes.inspect)
  end
  
  def test_if
    ast = AST.parse("if 1; 1.0; else; ''; end").body
    typer = Typer::Simple.new("bar")
    
    # incompatible body types
    assert_raise(Typer::InferenceError) {ast.infer(typer)}
    
    ast = AST.parse("if 1; 1.0; else; 2.0; end").body
    
    assert_nothing_raised {ast.infer(typer)}
    
    assert_equal(typer.fixnum_type, ast.condition.inferred_type)
    assert_equal(typer.float_type, ast.body.inferred_type)
    assert_equal(typer.float_type, ast.else.inferred_type)
    
    ast = AST.parse("if foo; bar; else; baz; end").body
    
    assert_nothing_raised {ast.infer(typer)}
    
    assert_equal(typer.default_type, ast.condition.inferred_type)
    assert_equal(typer.default_type, ast.body.inferred_type)
    assert_equal(typer.default_type, ast.else.inferred_type)
    
    # unresolved types for the foo, bar, and baz calls
    assert_raise(Typer::InferenceError) {typer.resolve(true)}
    
    ast2 = AST.parse("def foo; 1; end; def bar; 1.0; end")
    
    ast2.infer(typer)
    ast.infer(typer)
    
    # unresolved types for the baz call
    assert_raise(Typer::InferenceError) {typer.resolve(true)}
    
    assert_equal(typer.fixnum_type, ast.condition.inferred_type)
    assert_equal(typer.float_type, ast.body.inferred_type)
    assert_equal(typer.default_type, ast.else.inferred_type)
    
    ast2 = AST.parse("def baz; 2.0; end")
    
    ast2.infer(typer)
    ast.infer(typer)
    
    assert_nothing_raised {typer.resolve(true)}
    
    assert_equal(typer.fixnum_type, ast.condition.inferred_type)
    assert_equal(typer.float_type, ast.body.inferred_type)
    assert_equal(typer.float_type, ast.else.inferred_type)
  end
  
  def test_class
    ast = AST.parse("class Foo; def foo; 1; end; def baz; foo; end; end")
    cls = ast.body
    foo = cls.body[0]
    baz = cls.body[1]
    
    typer = Typer::Simple.new("script")
    ast.infer(typer)
    
    assert_nothing_raised {typer.resolve(true)}
    
    assert_not_nil(typer.known_types["Foo"])
    assert(AST::TypeDefinition === typer.known_types["Foo"])
    assert_equal(typer.known_types["Foo"], cls.inferred_type)
  end
end