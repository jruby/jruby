require 'duby'
require 'test/unit'
require 'jruby'

class TestAst < Test::Unit::TestCase
  include Duby
  
  def test_args
    new_ast = AST.parse("def foo(a, b = 1, *c, &d); end").body
    arguments = new_ast.arguments
    
    assert_not_nil(arguments)
    inspected = "Arguments\n RequiredArgument(a)\n OptionalArgument(b)\n  LocalAssignment(name = b, scope = MethodDefinition(foo))\n   Fixnum(1)\n RestArgument(c)\n BlockArgument(d)"
    assert_equal(inspected, arguments.inspect)
    
    assert(AST::Arguments === arguments)
    children = arguments.children
    assert_not_nil(children)
    assert_equal(4, children.size)
    assert(Array === children[0])
    assert(AST::RequiredArgument === children[0][0])
    assert_equal("a", children[0][0].name)
    assert_equal(arguments, children[0][0].parent)
    assert(Array === children[1])
    assert(AST::OptionalArgument === children[1][0])
    assert_equal("b", children[1][0].name)
    assert_equal(arguments, children[1][0].parent)
    # TODO: should be LocalDeclaration soon
    assert(AST::LocalAssignment === children[1][0].child)
    assert_equal(children[1][0], children[1][0].child.parent)
    assert(AST::Fixnum, children[1][0].child.value.parent)
    assert_equal(children[1][0].child, children[1][0].child.value.parent)
    assert(AST::RestArgument === children[2])
    assert_equal("c", children[2].name)
    assert_equal(arguments, children[2].parent)
    assert(AST::BlockArgument === children[3])
    assert_equal("d", children[3].name)
    assert_equal(arguments, children[3].parent)
  end
  
  def test_locals
    new_ast = AST.parse("a = 1; a").body
    
    assert_not_nil(new_ast)
    assert(AST::Body === new_ast)
    inspected = "Body\n LocalAssignment(name = a, scope = Script)\n  Fixnum(1)\n Local(name = a, scope = Script)"
    assert_equal(inspected, new_ast.inspect)
    assert(!new_ast.newline)
    
    asgn = new_ast[0]
    var = new_ast[1]
    
    assert(AST::LocalAssignment === asgn)
    assert(asgn.newline)
    assert_equal("a", asgn.name)
    assert(AST::Fixnum === asgn.value)
    assert(!asgn.value.newline)
    assert(AST::Local === var)
    assert(var.newline)
    assert_equal("a", var.name)
  end
  
  def test_fields
    new_ast = AST.parse("@a = 1; @a").body
    
    assert_not_nil(new_ast)
    assert(AST::Body === new_ast)
    inspected = "Body\n FieldAssignment(@a)\n  Fixnum(1)\n Field(@a)"
    assert_equal(inspected, new_ast.inspect)
    assert(!new_ast.newline)
    
    asgn = new_ast[0]
    var = new_ast[1]
    
    assert(AST::FieldAssignment === asgn)
    assert(asgn.newline)
    assert_equal("@a", asgn.name)
    assert(AST::Fixnum === asgn.value)
    assert(!asgn.value.newline)
    assert(AST::Field === var)
    assert(var.newline)
    assert_equal("@a", var.name)
  end
  
  def test_array
    new_ast = AST.parse("[a = 1, 1]").body
    
    assert_not_nil(new_ast)
    assert(AST::Array === new_ast)
    assert_equal("Array\n LocalAssignment(name = a, scope = Script)\n  Fixnum(1)\n Fixnum(1)", new_ast.inspect)
    
    assert(AST::LocalAssignment === new_ast[0])
    assert(AST::Fixnum === new_ast[1])
  end
  
  def test_call
    new_ast = AST.parse("1.foo(1)").body
    
    assert_not_nil(new_ast)
    assert(AST::Call === new_ast)
    assert_equal("Call(foo)\n Fixnum(1)\n Fixnum(1)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert(AST::Fixnum === new_ast.target)
    assert_not_nil(new_ast.parameters)
    assert_equal(1, new_ast.parameters.size)
    assert(AST::Fixnum === new_ast.parameters[0])
  end
  
  def test_fcall
    new_ast = AST.parse("foo(1)").body
    
    assert_not_nil(new_ast)
    assert(AST::FunctionalCall === new_ast)
    assert_equal("FunctionalCall(foo)\n Fixnum(1)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.parameters)
    assert_equal(1, new_ast.parameters.size)
    assert(AST::Fixnum === new_ast.parameters[0])
  end
  
  def test_if
    new_ast = AST.parse("if 1; 2; elsif !3; 4; else; 5; end").body
    
    assert_not_nil(new_ast)
    assert(AST::If === new_ast)
    assert_equal("If\n Condition\n  Fixnum(1)\n Fixnum(2)\n If\n  Condition\n   Not\n    Fixnum(3)\n  Fixnum(4)\n  Fixnum(5)", new_ast.inspect)
    
    assert(AST::Condition === new_ast.condition)
    assert(AST::Fixnum === new_ast.condition.predicate)
    assert(AST::Fixnum === new_ast.body)
    assert(AST::If === new_ast.else)
    assert(AST::Condition === new_ast.else.condition)
    assert(AST::Not === new_ast.else.condition.predicate)
    assert(AST::Fixnum === new_ast.else.body)
    assert(AST::Fixnum === new_ast.else.else)
  end
  
  def test_begin
    new_ast = AST.parse("begin; 1; 2; end").body
    
    assert_not_nil(new_ast)
    assert_equal("Body\n Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(AST::Body === new_ast)
    assert(AST::Fixnum === new_ast[0])
    
    new_ast = AST.parse("begin; 1; end").body
    assert(AST::Fixnum === new_ast)
    
    new_ast = AST.parse("begin; end").body
    assert(AST::Noop === new_ast)
  end
  
  def test_block
    new_ast = AST.parse("1; 2").body
    
    assert_not_nil(new_ast)
    assert_equal("Body\n Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(AST::Body === new_ast)
    assert(AST::Fixnum === new_ast[0])
    
    new_ast = AST.parse("1").body
    assert(AST::Fixnum === new_ast)
  end
  
  def test_fixnum
    new_ast = AST.parse("1").body
    
    assert_not_nil(new_ast)
    assert_equal("Fixnum(1)", new_ast.inspect)
    assert(AST::Fixnum === new_ast)
    assert_equal(1, new_ast.literal)
  end
  
  def test_float
    new_ast = AST.parse("1.0").body
    
    assert_not_nil(new_ast)
    assert_equal("Float(1.0)", new_ast.inspect)
    assert(AST::Float === new_ast)
    assert_equal(1.0, new_ast.literal)
  end
  
  def test_class
    new_ast = AST.parse("class Foo < Bar; 1; 2; end").body
    
    assert_not_nil(new_ast)
    assert_equal("ClassDefinition(Foo)\n Constant(Bar)\n Body\n  Fixnum(1)\n  Fixnum(2)", new_ast.inspect)
    assert(AST::ClassDefinition === new_ast)
    assert_equal("Foo", new_ast.name)
    
    assert(AST::Constant === new_ast.superclass)
    assert(AST::Body === new_ast.body)
    assert(AST::Fixnum === new_ast.body[0])
  end
  
  def test_defn
    new_ast = AST.parse("def foo(a, b); 1; end").body
    
    assert_not_nil(new_ast)
    assert_equal("MethodDefinition(foo)\n {:return=>Type(notype)}\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Fixnum(1)", new_ast.inspect)
    assert(AST::MethodDefinition === new_ast)
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.signature)
    assert_equal(1, new_ast.signature.size)
    assert(AST::TypeReference::NoType === new_ast.signature[:return])
    assert(AST::Arguments === new_ast.arguments)
    assert(AST::Fixnum === new_ast.body)
    
    new_ast = AST.parse("def foo; end").body
    
    assert_not_nil(new_ast)
    assert_equal("MethodDefinition(foo)\n {:return=>Type(notype)}\n Arguments", new_ast.inspect)
    assert_not_nil(new_ast.arguments)
    assert_equal(nil, new_ast.arguments.args)
    assert_equal(nil, new_ast.arguments.opt_args)
    assert_equal(nil, new_ast.arguments.rest_arg)
    assert_equal(nil, new_ast.arguments.block_arg)
    assert_nil(new_ast.body)
  end
  
  def test_defs
    new_ast = AST.parse("def self.foo(a, b); 1; end").body
    
    assert_not_nil(new_ast)
    inspected = "StaticMethodDefinition(foo)\n {:return=>Type(notype)}\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Fixnum(1)"
    assert_equal(inspected, new_ast.inspect)
    assert(AST::StaticMethodDefinition === new_ast)
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.signature)
    assert_equal(1, new_ast.signature.size)
    assert(AST::TypeReference::NoType === new_ast.signature[:return])
    assert(AST::Arguments === new_ast.arguments)
    assert(AST::Fixnum === new_ast.body)
    
    new_ast = AST.parse("def self.foo; end").body
    
    assert_not_nil(new_ast)
    assert_equal("StaticMethodDefinition(foo)\n {:return=>Type(notype)}\n Arguments", new_ast.inspect)
    assert_not_nil(new_ast.arguments)
    assert_equal(nil, new_ast.arguments.args)
    assert_equal(nil, new_ast.arguments.opt_args)
    assert_equal(nil, new_ast.arguments.rest_arg)
    assert_equal(nil, new_ast.arguments.block_arg)
    assert_nil(new_ast.body)
  end
  
  def test_signature
    new_ast = AST.parse("def self.foo(a, b); {a => :foo, b => :bar, :return => :baz}; 1; end").body
    
    assert_not_nil(new_ast.signature)
    inspected = "StaticMethodDefinition(foo)\n {:return=>Type(baz), :a=>Type(foo), :b=>Type(bar)}\n Arguments\n  RequiredArgument(a)\n  RequiredArgument(b)\n Body\n  Noop\n  Fixnum(1)"
    assert_equal(inspected, new_ast.inspect)
    signature = new_ast.signature
    assert_equal(3, signature.size)
    assert(AST::TypeReference === signature[:return])
    assert_equal("baz", signature[:return].name)
    assert(AST::TypeReference === signature[:a])
    assert_equal("foo", signature[:a].name)
    assert(AST::TypeReference === signature[:b])
    assert_equal("bar", signature[:b].name)
  end
  
  def test_type_reference
    signature = JRuby.parse("{a => :foo, b => java.lang.Object, :return => ArrayList}").child_nodes[0].signature(nil)
    
    inspected = "{:return=>Type(ArrayList), :a=>Type(foo), :b=>Type(java.lang.Object)}"
    assert_equal(inspected, signature.inspect)
    assert_equal(3, signature.size)
    assert(AST::TypeReference === signature[:return])
    assert_equal("ArrayList", signature[:return].name)
    assert(AST::TypeReference === signature[:a])
    assert_equal("foo", signature[:a].name)
    assert(AST::TypeReference === signature[:b])
    assert_equal("java.lang.Object", signature[:b].name)
  end
  
  def test_return
    new_ast = AST.parse("return 1").body
    
    assert_not_nil(new_ast)
    inspected = "Return\n Fixnum(1)"
    assert_equal(inspected, new_ast.inspect)
    assert(AST::Return === new_ast)
    assert(AST::Fixnum === new_ast.value)
  end
  
  def test_vcall
    new_ast = AST.parse("foo").body
    
    assert_not_nil(new_ast)
    assert(AST::FunctionalCall === new_ast)
    assert_equal("FunctionalCall(foo)", new_ast.inspect)
    
    assert_equal("foo", new_ast.name)
    assert_not_nil(new_ast.parameters)
    assert_equal(0, new_ast.parameters.size)
  end
  
  def test_while
    new_ast = AST.parse("while 1; 2; end").body
    
    assert_not_nil(new_ast)
    assert(AST::Loop === new_ast)
    assert_equal("Loop(check_first = true, negative = false)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(new_ast.check_first?)
    assert(!new_ast.negative?)
    assert(AST::Condition === new_ast.condition)
    assert(AST::Fixnum === new_ast.condition.predicate)
    assert(AST::Fixnum === new_ast.body)
    
    new_ast = AST.parse("begin; 2; end while 1").body
    
    assert_not_nil(new_ast)
    assert(AST::Loop === new_ast)
    assert_equal("Loop(check_first = false, negative = false)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(!new_ast.check_first?)
    assert(!new_ast.negative?)
    assert(AST::Condition === new_ast.condition)
    assert(AST::Fixnum === new_ast.condition.predicate)
    assert(AST::Fixnum === new_ast.body)
  end
  
  def test_until
    new_ast = AST.parse("until 1; 2; end").body
    
    assert_not_nil(new_ast)
    assert(AST::Loop === new_ast)
    assert_equal("Loop(check_first = true, negative = true)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(new_ast.check_first?)
    assert(new_ast.negative?)
    assert(AST::Condition === new_ast.condition)
    assert(AST::Fixnum === new_ast.condition.predicate)
    assert(AST::Fixnum === new_ast.body)
    
    new_ast = AST.parse("begin; 2; end until 1").body
    
    assert_not_nil(new_ast)
    assert(AST::Loop === new_ast)
    assert_equal("Loop(check_first = false, negative = true)\n Condition\n  Fixnum(1)\n Fixnum(2)", new_ast.inspect)
    assert(!new_ast.check_first?)
    assert(new_ast.negative?)
    assert(AST::Condition === new_ast.condition)
    assert(AST::Fixnum === new_ast.condition.predicate)
    assert(AST::Fixnum === new_ast.body)
  end
  
  def test_string
    new_ast = AST.parse("'foo'").body
    
    assert_not_nil(new_ast)
    assert(AST::String === new_ast)
    assert_equal("String(\"foo\")", new_ast.inspect)
    assert_equal("foo", new_ast.literal)
  end
  
  def test_root
    new_ast = AST.parse("1").body
    
    assert_not_nil(new_ast)
    assert(AST::Fixnum === new_ast)
  end
  
  def test_boolean
    new_ast1 = AST.parse("true").body
    new_ast2 = AST.parse("false").body
    
    assert_not_nil(new_ast1)
    assert_not_nil(new_ast2)
    assert(AST::Boolean === new_ast1)
    assert(AST::Boolean === new_ast2)
    assert(new_ast1.literal)
    assert(!new_ast2.literal)
  end
end