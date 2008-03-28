require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    # reload 
    module Java::OrgJrubyAst
      class CallNode
        def declared_type(builder)
          if name == "[]"
            # array type, top should be a constant; find the rest
            array = true
            elements = []
          else
            elements = [name]
          end
          
          receiver = receiver_node
          
          loop do
            case receiver
            when ConstNode
              elements << receiver_node.name
              break
            when CallNode
              elements.unshift(receiver.name)
              receiver = receiver.receiver_node
            when SymbolNode
              elements.unshift(receiver.name)
              break
            when VCallNode
              elements.unshift(receiver.name)
              break
            end
          end
          
          # join and load
          class_name = elements.join(".")
          type = builder.type(class_name)
          
          if array
            type.array_class
          else
            type
          end
        end
      end
  
      class Colon2Node
        def declared_type(builder)
          left_node.declared_type(builder).declared_field(name).static_value
        end
      end
      
      class HashNode
        def declare_types(builder)
          @declared = true
          list = list_node.child_nodes.to_a
          list.each_index do |index|
            builder.local(list[index].name, list[index + 1].declared_type(builder)) if index % 2 == 0
          end
        end
      end
      
      class SymbolNode
        def declared_type(builder)
          @type ||= builder.type(name.intern)
        end
      end
    end
  end
end
