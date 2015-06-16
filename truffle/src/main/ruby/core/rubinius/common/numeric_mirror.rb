module Rubinius
  class Mirror
    class Numeric < Mirror
      self.subject = ::Numeric

      def step_float_size(value, limit, step, asc)
        if (asc && value > limit) || (!asc && value < limit)
          return 0
        end

        if step.infinite?
          1
        else
          err = (value.abs + limit.abs + (limit - value).abs) / step.abs * Float::EPSILON
          if err.finite?
            err = 0.5 if err > 0.5
            ((limit - value) / step + err).floor + 1
          else
            0
          end
        end
      end

      def step_size(limit, step)
        values = step_fetch_args(limit, step)
        value = values[0]
        limit = values[1]
        step = values[2]
        asc = values[3]
        is_float = values[4]

        if is_float
          # Ported from MRI

          step_float_size(value, limit, step, asc)

        else
          if (asc && value > limit) || (!asc && value < limit)
            0
          else
            ((value - limit).abs + 1).fdiv(step.abs).ceil
          end
        end
      end

      def step_fetch_args(limit, step)
        raise ArgumentError, "step cannot be 0" if step == 0

        value = @object
        asc = step > 0
        if value.kind_of? Float or limit.kind_of? Float or step.kind_of? Float
          return FloatValue(value), FloatValue(limit), FloatValue(step), asc, true
        else
          return value, limit, step, asc, false
        end
      end

    end
  end
end
