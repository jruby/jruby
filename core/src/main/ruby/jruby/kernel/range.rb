class Range
  def bsearch(&cond)
    BSearch.new.bsearch(self, &cond)
  end

  class BSearch
    java_import java.lang.Double
    java_import java.lang.Math

    def initialize
      @satisfied = nil
      @smaller = false
    end

    def bsearch(range, &cond)
      b = range.begin
      e = range.end
      excl = range.exclude_end?

      return to_enum(:bsearch) unless cond

      if b.is_a?(Float) || e.is_a?(Float)
        return float_search(b, e, excl, &cond)

      else
        if b.is_a?(Integer) && e.is_a?(Integer)
          return integer_search(b, e, excl, &cond)

        elsif b.is_a?(Integer) && e.nil?
          diff = 1
          while true
            mid = b + diff
            ret = check(mid, &cond)
            return ret if ret
            if @smaller
              return integer_search(b, mid, false, &cond)
            end
            diff *= 2
          end

        elsif b.nil? && e.is_a?(Integer)
          diff = -1
          while true
            mid = b + diff
            ret = check(mid, &cond)
            return ret if ret
            unless @smaller
              return integer_search(mid, e, false, &cond)
            end
            diff *= 2
          end

        else
          raise TypeError, "can't do binary search for #{b.class}"
        end
      end
    end

    def float_search(b, e, excl, &cond)
      low = double_as_long(b.nil? ? -(Float::INFINITY) : b)
      high = double_as_long(e.nil? ? Float::INFINITY : e)

      if excl
        high -= 1
      end
      org_high = high
      while low < high
        mid = if (high < 0) == (low < 0)
                low + ((high - low) / 2)
              elsif low < -high
                -((-1 - low - high) / 2 + 1)
              else
                (low + high) / 2
              end

        ret = check(long_as_double(mid), &cond)
        return ret if ret
        if @smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        ret = check(long_as_double(low), &cond)
        return ret if ret
        return nil unless @smaller
      end

      return @satisfied
    end

    def check(val)
      v = yield val
      if v == true
        @satisfied = val
        @smaller = true
      elsif v == false || v == nil
        @smaller = false
      elsif v.is_a?(Numeric)
        cmp = v <=> 0
        return val if cmp == 0
        @smaller = cmp < 0
      else
        raise TypeError, "wrong argument type #{v.class} (must be numeric, true, false or nil)"
      end
      false
    end

    def integer_search(low, high, excl, &cond)
      high -= 1 if excl

      org_high = high

      while (low <=> high) < 0
        mid = (high + low) / 2
        ret = check(mid, &cond)
        return ret if ret
        if @smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        ret = check(low, &cond)
        return ret if ret
        return nil unless @smaller
      end

      return @satisfied
    end

    def double_as_long(double)
      val = Double.doubleToLongBits(Math.abs(double))
      double < 0 ? -val : val
    end

    def long_as_double(long)
      if long < 0
        long = -long
        -(Double.longBitsToDouble(long))
      else
        Double.longBitsToDouble(long)
      end
    end
  end
  private_constant :BSearch
end
