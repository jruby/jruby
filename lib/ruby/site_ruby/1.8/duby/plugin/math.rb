require 'duby/typer'

module Duby
  module Typer
    class MathTyper < BaseTyper
      def name
        "Math"
      end
      
      def method_type(typer, target_type, name, parameter_types)
        return nil unless parameter_types.size == 1
        
        result = case target_type
        when typer.fixnum_type
          case name
          when :-
            case parameter_types[0]
            when typer.fixnum_type
              typer.fixnum_type
            else
              nil
            end
          when :+
            case parameter_types[0]
            when typer.fixnum_type
              typer.fixnum_type
            else
              nil
            end
          when :*
            case parameter_types[0]
            when typer.fixnum_type
              typer.fixnum_type
            else
              nil
            end
          when :/
            case parameter_types[0]
            when typer.fixnum_type
              typer.fixnum_type
            else
              nil
            end
          when :<
            case parameter_types[0]
            when typer.fixnum_type
              typer.boolean_type
            else
              nil
            end
          else
            nil
          end
        else
          nil
        end
        
        if result
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} = #{result}"
        else
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} not found"
        end
        
        result
      end
    end
  end

  typer_plugins << Typer::MathTyper.new
end