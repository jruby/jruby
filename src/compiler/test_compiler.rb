
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

test_print_report


# ------------------------------------------------------


interfaces = JavaLang::JString[].new(0)
classgen = BCEL::ClassGen.new("CompiledRuby",
                              "java.lang.Object",
                              "cool generated code",
                              BCEL::Constants::ACC_PUBLIC,
                              interfaces)

arg_types = BCEL::Type[].new(2)
arg_types[0] = BCEL::ObjectType.new("org.jruby.Ruby")
arg_types[1] = BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject")

arg_names = JavaLang::JString[].new(2)
arg_names[0] = "runtime"
arg_names[1] = "self"

instructions = BCEL::InstructionList.new

methodgen = BCEL::MethodGen.new(BCEL::Constants::ACC_PUBLIC | BCEL::Constants::ACC_STATIC,
                                BCEL::ObjectType.new("org.jruby.runtime.builtin.IRubyObject"),
                                arg_types,
                                arg_names,
                                "doStuff",
                                "CompiledRuby",
                                instructions,
                                classgen.getConstantPool)

code = compile("if false; 1 + 2; else; 'hello'; end")
code.jvm_compile(methodgen)

# Add a return manually
instructions.append(BCEL::ARETURN.new)

methodgen.setMaxStack
classgen.addMethod(methodgen.getMethod)
classgen.addEmptyConstructor(BCEL::Constants::ACC_PUBLIC)

classgen.getJavaClass.dump("/tmp/CompiledRuby.class")
