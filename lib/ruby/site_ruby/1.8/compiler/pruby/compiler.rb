require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    JObject = java.lang.Object
    
    # reload 
    module Java::OrgJrubyAst
      class Node
        def bytecode(builder)
          # default behavior is to raise, to expose missing nodes
          raise
        end
      end
  
      class ArgsNode
        def bytecode(builder)
          # not implemented
          raise
        end
      end
  
      class ArrayNode
        def bytecode(builder)
          # not implemented
          raise
        end
      end
  
      class BlockNode
        def bytecode(builder)
          child_nodes.each do |node|
            builder.line node.position.start_line
        
            node.bytecode(builder)
          end
        end
      end
  
      class DefnNode
        def bytecode(builder)
          # TODO: no params
          builder.method2(name) do |method|
            body_node.bytecode(method)
          end
        end
      end
  
      class FCallNode
        def bytecode(builder)
          builder.aload 0

          types = []
          args_node.child_nodes.each do |node|
            node.bytecode(builder)
            types << JObject
          end

          builder.invokevirtual builder.this, name, types
        end
      end
  
      class NewlineNode
        def bytecode(builder)
          builder.line position.start_line
          next_node.bytecode(builder)
        end
      end
  
      class RootNode
        def bytecode(builder)
          # builder is class builder
      
          if body_node
            body_node.bytecode(builder)
          end
        end
      end
  
      class StrNode
        def bytecode(builder)
          builder.ldc value
        end
      end
    end
  end
end

if $0 == __FILE__
  n = JRuby.parse(File.read(ARGV[0]))
  compiler = Compiler::ClassBuilder.new(ARGV[0].split(".")[0], ARGV[0])
  n.bytecode(compiler)

  File.open("#{ARGV[0].split(".")[0]}.class", "w") do |file|
    puts "Compiling: #{file.path}"
    file.write(compiler.generate)
  end
end