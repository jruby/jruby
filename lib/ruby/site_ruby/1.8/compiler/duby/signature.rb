require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    # reload 
    module Java::OrgJrubyAst
      class CallNode
        def signature(builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          
          recv_java_class = receiver_node.type(builder)
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          
          if (return_type)
            return_class = builder.type(return_type.to_s)
          else
            return_type = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types.map {|type| builder.type(type.to_s)}
          ]
        end
      end
      
      class HashNode
        def signature(builder)
          @declared = true
          arg_types = []
          return_type = Void
          list = list_node.child_nodes.to_a
          list.each_index do |index|
            if index % 2 == 0
              if SymbolNode === list[index] && list[index].name == 'return'
                return_type = list[index + 1].declared_type(builder)
              else
                arg_types << list[index + 1].declared_type(builder)
              end
            end
          end
          return [return_type, *arg_types]
        end
      end
    end
  end
end
