require 'jruby'
require 'test/minirunit'

node = JRuby.parse("a = 5; a", "EVAL")

cls = nil

test_no_exception {
  context = org.jruby.compiler.StandardASMCompiler.new(node);
  org.jruby.compiler.NodeCompilerFactory.getCompiler(node).compile(node, context)

  cls = context.loadClass
}

result = cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self)

test_equal(5, result)