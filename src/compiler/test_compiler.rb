
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

test_print_report
