
require '../../test/minirunit.rb'

require 'compiling_visitor.rb'

module JRuby
  include_package 'org.jruby'
end

$runtime = JRuby::Ruby.getDefaultInstance
$compiler = JRuby::Compiler::CompilingVisitor.new

def compile(source)
  tree = $runtime.parse(source, "<cool stuff>")
  $compiler.compile(tree)
end

include JRuby::Compiler::Bytecode

code = compile("x = 10")

test_equal([PushFixnum,
             AssignLocal],
           code.collect {|c| c.class })
test_equal(10, code[0].value)
test_equal(2, code[1].index)

code = compile("puts 'hello'")

test_equal([PushSelf, PushString, Call],
           code.collect {|c| c.class })
test_equal("hello", code[1].value)
test_equal("puts", code[2].name)
test_equal(1, code[2].arity)

code = compile("self")
test_equal([PushSelf], code.collect {|c| c.class })


code = compile("1 + 2")
test_equal([PushFixnum, PushFixnum, Call],
           code.collect {|c| c.class })


code = compile("1 != 2")
test_equal([PushFixnum, PushFixnum, Call, Negate],
           code.collect {|c| c.class })

code = compile("if false; 1; else; 'hello'; end")
test_equal([PushBoolean, IfFalse, PushFixnum,
             Goto, Label, PushString, Label],
           code.collect {|c| c.class})

code = compile("begin; 123; end")
test_equal([PushFixnum], code.collect {|c| c.class})

code = compile("SomeConstant")
test_equal([PushConstant], code.collect {|c| c.class})

code = compile("nil")
test_equal([PushNil], code.collect {|c| c.class})

code = compile("def hello(x); x * 2; end; hello(3)")
test_equal([CreateMethod, PushNil, PushSelf, PushFixnum, Call],
           code.collect {|c| c.class})


module JRubyUtil
  include_package "org.jruby.util"
end

module JRubyCompiler
  include_package 'org.jruby.compiler'
end

$class_counter = 0

def jvm_compile(source)
  interfaces = JavaLang::JString[].new(0)
  classgen = BCEL::ClassGen.new("jruby.CompiledRuby" + $class_counter.to_s,
                                "java.lang.Object",
                                "cool generated code",
                                BCEL::Constants::ACC_PUBLIC,
                                interfaces)
  $class_counter += 1

  code = compile(source)
  code.jvm_compile(classgen, "doStuff")

  classgen.addEmptyConstructor(BCEL::Constants::ACC_PUBLIC)

#  classgen.getJavaClass.dump("/tmp/" + classgen.getClassName) # REMOVE ME

  JRubyCompiler::ByteCodeRuntime.addClass(classgen.getClassName,
                                          classgen.getJavaClass.getBytes)

  result = JRubyUtil::TestHelper.loadAndCall(self,
                                             classgen.getClassName,
                                             classgen.getJavaClass.getBytes,
                                             "doStuff")
  result
end


def test_compiled(expected, source)
  test_equal(expected, jvm_compile(source))
end

def test_compiled_type(expected_type, source)
  test_equal(expected_type, jvm_compile(source).type)
end



test_compiled(nil, "class CL_1; end")
#test_compiled_type(Class, "class CL_2; end; CL_2")

test_compiled(nil, "module ABC; end")
test_compiled(123, "module X; Y = 123; end")
test_compiled_type(Module, "module X; end; X")
test_compiled(123, "module X; Y = 123; end; X::Y")

test_compiled(3, "3")
test_compiled("hello", '"hello"')
test_compiled(true, "true")
test_compiled(false, "false")
test_compiled(3, "PI = 3")
test_compiled(3, "PI = 3; PI")
test_compiled(3, "if true; 1 + 2; else; 'hello'; end")
test_compiled(nil, "if false; 1; end")
test_compiled("hello", "if false; 1 + 2; else; 'hello'; end")
test_compiled(1, "if true; if true; 1; else; 2; end; end")
test_compiled(3, "[1,2,3,4][2]")
test_compiled(123, "begin; 123; end")
test_compiled([1..2, 1...3], "[1..2, 1...3]")
test_compiled(:hello, ":hello")
test_compiled(2, "unless true; 1; else; 2; end")
test_compiled(nil, "def hello(x); x * 2; end")
test_compiled(7, "def abc(); 7; end; abc()")
test_compiled(6, "def hello(x); x * 2; end; hello(3)")


test_compiled(55, <<END
def fib(n)
  if n < 2
    n
  else
    fib(n - 2) + fib(n - 1)
  end
end
fib(10)
END
)

test_print_report
