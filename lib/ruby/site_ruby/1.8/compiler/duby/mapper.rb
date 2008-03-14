require 'compiler/builder'
require 'jruby'

module Compiler
  module PrimitiveRuby
    # reload 
    module Java::OrgJrubyAst
      class CallNode
        def mapped_name(builder)
          # TODO move to a utility somewhere for smart name mappings
          # TODO or at least make it a table...
          mapped_name = name
          case receiver_node.type(builder)
          when JString
            case name
            when "+"
              mapped_name = "concat"
            # This doesn't work yet, because format is a static method on String
#            when "%"
#              mapped_name = "format"
            end
          else
            case name
            when "new"
              mapped_name = "<init>"
            end
          end
          
          mapped_name
        end
      end
      
      class DefnNode
        def mapped_name(builder)
          case name
          when "initialize"
            "<init>"
          else
            name
          end
        end
      end
  
      class FCallNode
        def mapped_name(builder)
          if name == "puts"
            "println"
          end
        end
      end
      
      class InstVarNode
        def mapped_name(builder)
          name[1..-1]
        end
      end
      
      class InstAsgnNode
        def mapped_name(builder)
          name[1..-1]
        end
      end
  
      class VCallNode
        def mapped_name(builder)
          # TODO map names for the local type?
          name
        end
      end
    end
  end
end
