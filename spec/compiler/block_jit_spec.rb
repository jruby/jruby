require 'jruby'
require 'rspec'

# The JIT compiles a block on its own when the block gets hot before the
# method around it, as with a loop in a method that only runs once. The
# general compiler specs compile whole scripts, so they don't cover that.
describe "JRuby's block JIT" do
  # Compiles every block in src on its own and loads the class, as
  # BlockJITTask does, and returns how many blocks were compiled.
  def compile_each_block(src)
    runtime = JRuby.runtime
    closures = []
    collect = lambda do |scope|
      scope.builtInterpreterContext if scope.is_a?(org.jruby.ir.IRMethod)
      closures << scope if scope.is_a?(org.jruby.ir.IRClosure)
      scope.getLexicalScopes.each { |child| collect.call(child) }
    end
    collect.call(JRuby.compile_ir(src, "block_jit_spec.rb"))

    closures.each do |closure|
      visitor = org.jruby.ir.targets.JVMVisitor.newForJIT(runtime)
      bytes = visitor.compileToBytecode(closure, org.jruby.ir.targets.JVMVisitorMethodContext.new)
      visitor.defineFromBytecode(closure, bytes, org.jruby.util.OneShotClassLoader.new(runtime.getJRubyClassLoader))
    end

    closures.size
  end

  # See https://github.com/jruby/jruby/issues/9826
  it "compiles a for loop body that contains a block" do
    expect(compile_each_block("def m; for i in 1..2; proc { i }.call; end; end")).to eq 2
    expect(compile_each_block("def m; for i in 1..2; [i].each { |j| j }; end; end")).to eq 2
    expect(compile_each_block("def m; for i in 1..2; -> { i }.call; end; end")).to eq 2
    expect(compile_each_block("def m; for i in 1..2; for j in 1..2; end; end; end")).to eq 2
    expect(compile_each_block("[1].each { for i in 1..2; proc { i }; end }")).to eq 3
  end
end
