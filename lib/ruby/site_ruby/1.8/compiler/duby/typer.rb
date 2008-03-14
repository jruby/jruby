require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    # reload 
    module Java::OrgJrubyAst
      class BeginNode
        def type(builder)
          body_node.type(builder)
        end
      end
  
      class BlockNode
        # Type of a block is the type of its final element
        def type(builder)
          if child_nodes.size == 0
            JObject
          else
            @type ||= child_nodes.get(child_nodes.size - 1).type(builder)
          end
        end
      end
      
      class CallNode
        def type(builder)
          @return_type ||= begin
            recv_type = receiver_node.type(builder)
            
            # if we already have an exact class, use it
            if recv_type.array?
              if name == "length"
                Jint
              else
                recv_type = recv_type.component_type
              end
            elsif JavaClass === recv_type
              recv_type
            else
              # otherwise, find the target method and get its return type
              recv_java_class = recv_type
              arg_types = []
              args_node.child_nodes.each do |node|
                arg_types << node.type(builder)
              end if args_node
              declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
              return_type = declared_method.return_type

              builder.type(return_type.to_s)
            end
          end
        end
      end
      
      class ConstNode
        def type(builder)
          @type ||= builder.type(name)
        end
      end
  
      class FCallNode
        def special_signature(recv_type, builder)
          arg_types = []
          args_node.child_nodes.each do |node|
            arg_types << node.type(builder)
          end if args_node
          recv_java_class = recv_type
          declared_method = recv_java_class.declared_method_smart(mapped_name(builder), *arg_types)
          return_type = declared_method.return_type
          if (return_type)
            return_class = return_type
          else
            return_class = Void
          end
          
          return [
            return_class,
            *declared_method.parameter_types
          ]
        end
        
        def type(builder)
          @type ||= begin
            arg_types = []
            args_node.child_nodes.each do |node|
              arg_types << node.type(builder)
            end if args_node
            if builder.static
              signature = builder.static_signature(name, arg_types)
            else
              signature = builder.instance_signature(name, arg_types)
            end
            raise CompileError.new(position, "Signature not found for call #{name}") unless signature

            signature[0]
          end
        end
      end
      
      class FixnumNode
        def type(builder)
          Jint
        end
      end
      
      class FloatNode
        def type(builder)
          Jfloat
        end
      end
      
      class InstVarNode
        def type(builder)
          @type ||= builder.field_type(mapped_name(builder))
        end
      end
      
      class InstAsgnNode
        def type(builder)
          builder.field(mapped_name(builder), value_node.type(builder))
          @type ||= builder.field_type(mapped_name(builder))
        end
      end
      
      class LocalAsgnNode
        def type(builder)
          @type ||= builder.local_type(name)
        end
      end
      
      class LocalVarNode
        def type(builder)
          @type ||= builder.local_type(name)
        end
      end
      
      class ModuleNode
      end
  
      class NewlineNode
        def type(builder)
          @type ||= next_node.type(builder)
        end
      end
  
      class StrNode
        def type(builder)
          JString
        end
      end
  
      class VCallNode
        def type(builder)
          @type ||= if builder.static
            builder.static_signature(name, [])[0]
          else
            builder.instance_signature(name, [])[0]
          end
        end
      end
    end
  end
end
