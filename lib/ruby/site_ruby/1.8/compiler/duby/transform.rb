module Compiler::Duby
  # reload 
  module Java::OrgJrubyAst
    class Node
      def transform(parent)
        # default behavior is to raise, to expose missing nodes
        raise CompileError.new(position, "Unsupported syntax: #{self}")
      end
    end

    class ArgsNode
      def transform(parent)
        Arguments.new(parent) do |args_node|
          arg_list = args.child_nodes.map do |node|
            RequiredArgument.new(args_node, node.name)
            # argument nodes will have type soon
            #RequiredArgument.new(args_node, node.name, node.type)
          end if args
          
          opt_list = opt_args.child_nodes.map do |node|
            OptionalArgument.new(args_node) {|opt_arg| [node.transform(opt_arg)]}
          end if opt_args
          
          rest_arg = RestArgument.new(args_node, rest_arg_node.name) if rest_arg_node
          
          block_arg = BlockArgument.new(args_node, block_arg_node.name) if block_arg_node
          
          [arg_list, opt_list, rest_arg, block_arg]
        end
      end
    end

    class ArrayNode
      def transform(parent)
        Array.new(parent) do |array|
          child_nodes.map {|child| child.transform(array)}
        end
      end
    end

    class BeginNode
      def transform(parent)
        body_node.transform(parent)
      end
    end

    class BlockNode
      def transform(parent)
        Body.new(parent) do |body|
          child_nodes.map {|child| child.transform(body)}
        end
      end
    end

    class ClassNode
      def transform(parent)
        ClassDefinition.new(parent, cpath.name) do |class_def|
          [
            super_node ? super_node.transform(class_def) : nil,
            body_node ? body_node.transform(class_def) : nil
          ]
        end
      end
    end

    class CallNode
      def transform(parent)
        Call.new(parent, name) do |call|
          [
            receiver_node.transform(call),
            args_node ? args_node.child_nodes.map {|arg| arg.transform(call)} : [],
            iter_node ? iter_node.transform(call) : nil
          ]
        end
      end
    end

    class Colon2Node
    end

    class ConstNode
      def transform(parent)
        Constant.new(parent, name)
      end
    end

    class DefnNode
    end

    class DefsNode
    end

    class FCallNode
      def transform(parent)
        FunctionalCall.new(parent, name) do |call|
          [
            args_node ? args_node.child_nodes.map {|arg| arg.transform(call)} : [],
            iter_node ? iter_node.transform(call) : nil
          ]
        end
      end
    end

    class FixnumNode
      def transform(parent)
        Fixnum.new(parent, value)
      end
    end

    class FloatNode
      def transform(parent)
        Float.new(parent, value)
      end
    end

    class HashNode
    end

    class IfNode
      def transform(parent)
        If.new(parent) do |iff|
          [
            condition.transform(iff),
            then_body.transform(iff),
            else_body ? else_body.transform(iff) : nil
          ]
        end
      end
    end

    class InstAsgnNode
      def transform(parent)
        # TODO: first encounter or explicit decl should be a FieldDeclaration
        FieldAssignment.new(parent, name) {|field| [value_node.transform(field)]}
      end
    end

    class InstVarNode
      def transform(parent)
        Field.new(parent, name)
      end
    end

    class LocalAsgnNode
      def transform(parent)
        # TODO: first encounter should be a LocalDeclaration
        LocalAssignment.new(parent, name) {|local| [value_node.transform(local)]}
      end
    end

    class LocalVarNode
      def transform(parent)
        Local.new(parent, name)
      end
    end

    class ModuleNode
      def transform(parent)
        builder.package(cpath.name) {
          body_node.compile(builder)
        }
      end
    end

    class NewlineNode
      def transform(parent)
        actual = next_node.transform(parent)
        actual.newline = true
        actual
      end
    end
    
    class NotNode
      def transform(parent)
        Not.new(parent) {|nott| [condition_node.transform(nott)]}
      end
    end

    class ReturnNode
      def transform(parent)
        value_node.compile(builder)
        builder.areturn
      end
    end

    class RootNode
      def transform(parent)
        # builder is class builder

        if body_node
          body_node.compile(builder)
        end
      end
    end

    class SelfNode
      def transform(parent)
        builder.local("this")
      end
    end

    class StrNode
      def transform(parent)
        builder.ldc value
      end
    end

    class SymbolNode
    end

    class VCallNode
      def transform(parent)
        if builder.static
          builder.invokestatic builder.this, name, builder.static_signature(name, [])
        else
          builder.aload 0

          builder.invokevirtual builder.this, name, builder.instance_signature(name, [])
        end
      end
    end

    class WhileNode
      def transform(parent)
        begin_lbl = builder.label
        end_lbl = builder.label
        cond_lbl = builder.label

        case body_node.type(builder)
        when Jint
          builder.iconst_0
        else
          builder.aconst_null
        end

        if evaluate_at_start
          builder.goto cond_lbl
        end

        begin_lbl.set!
        builder.pop
        body_node.compile(builder)

        cond_lbl.set!
        compile_condition(builder, begin_lbl)
        end_lbl.set!
      end

      def transform(parent)_condition(builder, begin_lbl)
        condition = condition_node
        condition = condition.next_node while NewlineNode === condition

        case condition
        when CallNode
          args = condition.args_node
          receiver_type = condition.receiver_node.type(builder)

          if receiver_type.primitive?
            case condition.name
            when "<"
              raise CompileError.new(position, "Primitive < must have exactly one argument") if !args || args.size != 1

              condition.receiver_node.compile(builder)
              args.get(0).compile(builder)

              case receiver_type
              when Jint
                builder.if_icmplt(begin_lbl)
              else
                raise CompileError.new(position, "Primitive < is only supported for int")
              end
            when ">"
              raise CompileError.new(position, "Primitive > must have exactly one argument") if !args || args.size != 1

              condition.receiver_node.compile(builder)
              args.get(0).compile(builder)

              case receiver_type
              when Jint
                builder.if_icmpgt(begin_lbl)
              else
                raise CompileError.new(position, "Primitive < is only supported for int")
              end
            else
              raise CompileError.new(position, "Conditional not supported: #{condition.inspect}")
            end
          else
            raise CompileError.new(position, "Conditional on non-primitives not supported: #{condition.inspect}")
          end
        else
          raise CompileError.new(position, "Non-call conditional not supported: #{condition.inspect}")
        end
      end
    end
  end
end
