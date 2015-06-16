module Rubinius
  class Mirror
    class Range < Mirror
      self.subject = ::Range

      def excl
        Rubinius.invoke_primitive :object_get_ivar, @object, :@excl
      end

      def step_float_iterations_size(first, last, step_size)
        err = (first.abs + last.abs + (last - first).abs) / step_size.abs * Float::EPSILON
        err = 0.5 if err > 0.5

        if excl
          iterations = ((last - first) / step_size - err).floor
          iterations += 1 if iterations * step_size + first < last
        else
          iterations = ((last - first) / step_size + err).floor + 1
        end
      end

      def step_iterations_size(first, last, step_size)
        case first
        when Float
          step_float_iterations_size(first, last, step_size)
        else
          @object.size.nil? ? nil : (@object.size.fdiv(step_size)).ceil
        end
      end

      def validate_step_size(first, last, step_size)
        if step_size.kind_of? Float or first.kind_of? Float or last.kind_of? Float
          # if any are floats they all must be
          begin
            step_size = Float(from = step_size)
            first     = Float(from = first)
            last      = Float(from = last)
          rescue ArgumentError
            raise TypeError, "no implicit conversion to float from #{from.class}"
          end
        else
          step_size = Integer(from = step_size)

          unless step_size.kind_of? Integer
            raise TypeError, "can't convert #{from.class} to Integer (#{from.class}#to_int gives #{step_size.class})"
          end
        end

        if step_size <= 0
          raise ArgumentError, "step can't be negative" if step_size < 0
          raise ArgumentError, "step can't be 0"
        end

        return first, last, step_size
      end

    end
  end
end
