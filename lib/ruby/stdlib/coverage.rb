# Load built-in coverage library
JRuby::Util.load_ext("org.jruby.ext.coverage.CoverageLibrary")

require 'jruby'

module Coverage
  def self.line_stub(file)
    lines = File.foreach(file).map {nil}
    root_node = JRuby.parse(File.read(file))

    visitor = org.jruby.ast.visitor.NodeVisitor.impl do |name, node|
      if node.newline?
        lines[node.line] = 0
      end

      node.child_nodes.each {|child| child && child.accept(visitor)}
    end

    root_node.accept visitor

    lines
  end
end
