require 'test/unit'
require 'compiler/duby'
require 'compiler/duby/typer2'

class TestTyper < Test::Unit::TestCase
  include Compiler::Duby
  
  class MockTyper
    include Compiler::Duby
    
    def default_type
      AST::TypeReference.new :notype
    end
    def fixnum_type
      AST::TypeReference.new :fixnum
    end
    def float_type
      AST::TypeReference.new :float
    end
    def string_type
      AST::TypeReference.new :string
    end
    def learn_local_type(name, type)
      name = name.intern unless Symbol === name
      
      local_types[name] = type
    end
    def local_type(name)
      name = name.intern unless Symbol === name
      
      local_types[name]
    end
    def local_types
      @local_types ||= {}
    end
    def learn_method_type(name, type)
      name = name.intern unless Symbol === name
      
      method_types[name] = type
    end
    def method_type(name)
      name = name.intern unless Symbol === name
      
      method_types[name]
    end
    def method_types
      @method_types ||= {}
    end
  end
  
  def test_fixnum
    ast = AST.parse("1")
    
    assert_equal(AST::TypeReference.new(:fixnum), ast.infer_type(MockTyper.new))
  end
  
  def test_float
    ast = AST.parse("1.0")
    
    assert_equal(AST::TypeReference.new(:float), ast.infer_type(MockTyper.new))
  end
  
  def test_string
    ast = AST.parse("'foo'")
    
    assert_equal(AST::TypeReference.new(:string), ast.infer_type(MockTyper.new))
  end
  
  def test_body
    ast1 = AST.parse("'foo'; 1.0; 1")
    ast2 = AST.parse("begin; end")
    
    assert_equal(AST::TypeReference.new(:fixnum), ast1.infer_type(MockTyper.new))
    assert_equal(AST::TypeReference.new(:notype), ast2.infer_type(MockTyper.new))
  end
  
  def test_local
    ast1 = AST.parse("a = 1; a")
    typer = MockTyper.new
    
    ast1.infer_type(typer)
    
    assert_equal(AST::TypeReference.new(:fixnum), typer.local_type(:a))
    assert_equal(AST::TypeReference.new(:fixnum), ast1.children[0].inferred_type)
    assert_equal(AST::TypeReference.new(:fixnum), ast1.children[1].inferred_type)
    
    ast2 = AST.parse("b = a = 1")
    ast2.infer_type(typer)
    
    assert_equal(AST::TypeReference.new(:fixnum), typer.local_type(:b))
    assert_equal(AST::TypeReference.new(:fixnum), ast2.children[0].inferred_type)
  end
  
  def test_signature
    ["def foo", "def self.foo"].each do |def_foo|
      ast1 = AST.parse("#{def_foo}(a); {a => :string}; end")
      typer = MockTyper.new

      ast1.infer_type(typer)

      assert_equal(AST::TypeReference.new(:notype), typer.method_type(:foo))
      assert_equal(AST::TypeReference.new(:string), typer.local_type(:a))
      assert_equal(AST::TypeReference.new(:notype), ast1.inferred_type)
      assert_equal(AST::TypeReference.new(:string), ast1.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); 1 end")
      typer = MockTyper.new

      ast1.infer_type(typer)

      assert_equal(AST::TypeReference.new(:fixnum), typer.method_type(:foo))
      assert_equal(AST::TypeReference.new(:notype), typer.local_type(:a))
      assert_equal(AST::TypeReference.new(:fixnum), ast1.inferred_type)
      assert_equal(AST::TypeReference.new(:notype), ast1.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {a => :string}; a; end")
      typer = MockTyper.new

      ast1.infer_type(typer)

      assert_equal(AST::TypeReference.new(:string), typer.method_type(:foo))
      assert_equal(AST::TypeReference.new(:string), typer.local_type(:a))
      assert_equal(AST::TypeReference.new(:string), ast1.inferred_type)
      assert_equal(AST::TypeReference.new(:string), ast1.arguments.args[0].inferred_type)

      ast1 = AST.parse("#{def_foo}(a); {:return => :string}; end")
      typer = MockTyper.new

      assert_raise(AST::InferenceError) {ast1.infer_type(typer)}

      ast1 = AST.parse("#{def_foo}(a); 'foo'; end")
      typer = MockTyper.new

      ast1.infer_type(typer)

      assert_equal(AST::TypeReference.new(:string), typer.method_type(:foo))
      assert_equal(AST::TypeReference.new(:notype), typer.local_type(:a))
      assert_equal(AST::TypeReference.new(:string), ast1.inferred_type)
      assert_equal(AST::TypeReference.new(:notype), ast1.arguments.args[0].inferred_type)
    end
  end
end