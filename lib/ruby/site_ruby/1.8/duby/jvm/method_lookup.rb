module Duby
  module JVM
    module MethodLookup
      def find_method(mapped_type, name, mapped_params, meta)
        if name == 'new'
          if meta
            name = "<init>"
            constructor = true
          else
            constructor = false
          end
        end

        begin
          if constructor
            method = mapped_type.constructor(*mapped_params)
          else
            method = mapped_type.java_method(name, *mapped_params)
          end
        rescue NameError
          unless constructor
            log "Failed to locate method #{mapped_type}.#{name}(#{mapped_params})"
            # exact args failed, do a deeper search
            # TODO for now this just tries immediate supertypes, which
            # obviously wouldn't work on primitives; need to implement JLS
            # method selection here
            mapped_params.size.times do |i|
              tmp_args = mapped_params.dup
              while tmp_args[i] != Java::java.lang.Object.java_class
                tmp_args[i] = tmp_args[i].superclass

                log "Trying #{mapped_type}.#{name}(#{tmp_args})"
                if constructor
                  method = mapped_type.constructor(*tmp_args) rescue nil
                else
                  method = mapped_type.java_method(name, *tmp_args) rescue nil
                end

                break if method
              end
              break if method
            end
          end
          unless method
            log "Failed to locate method #{name}(#{mapped_params}) on #{mapped_type}"
            return nil
          end
        end

        log "Found method #{method.declaring_class}.#{name}(#{method.parameter_types}) from #{mapped_type}"
        return method
      end
    end
  end
end