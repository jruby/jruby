class Range
  def bsearch(&block)
    BSearch.new(self, block).bsearch
  end

  class BSearch
    java_import java.lang.Double
    java_import java.lang.Math

    def initialize(range, cond)
      @begin = range.begin
      @end = range.end
      @excl = range.exclude_end?
      @cond = cond
      @satisfied = nil
      @smaller = false
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

    def bsearch()
      return to_enum(:bsearch) unless @cond

      beg = @begin
      endd = @end

      if beg.is_a?(Float) || endd.is_a?(Float)
        low = double_as_long(beg.nil? ? -(Float::INFINITY) : beg)
        high = double_as_long(endd.nil? ? Float::INFINITY : endd)
        return fast_search(low, high) {|i| long_as_double(i)}
      elsif beg.is_a?(Integer) && endd.is_a?(Integer)
        return integer_search(beg, endd, @excl)
      elsif beg.is_a?(Integer) && endd.nil?
        diff = 1
        while true
          mid = beg + diff
          ret = check(mid)
          return ret if ret
          if @smaller
            return integer_search(beg, mid, false)
          end
          diff *= 2
        end
      elsif beg.nil? && endd.is_a?(Integer)
        diff = -1
        while true
          mid = beg + diff
          ret = check(mid)
          return ret if ret
          unless @smaller
            return integer_search(mid, endd, false)
          end
          diff *= 2
        end
      else
        raise TypeError, "can't do binary search for #{beg.class}"
      end
    end

    def integer_search(low, high, excl)
      high -= 1 if excl

      org_high = high

      while (low <=> high) < 0
        mid = (high + low) / 2
        ret = check(mid)
        return ret if ret
        if @smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        ret = check(low)
        return ret if ret
        return nil unless @smaller
      end

      @satisfied
    end

    def check(val)
      v = @cond.call val
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

    def fast_search(low, high)
      if @excl
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

        ret = check(yield(mid))
        return ret if ret
        if @smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        ret = check(yield(low))
        return ret if ret
        return nil unless @smaller
      end

      @satisfied
    end
  end
  private_constant :BSearch
end
