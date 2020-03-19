class Range
  def bsearch(&cond)
    BSearch.bsearch(self, &cond)
  end

  class BSearch
    java_import java.lang.Double
    java_import java.lang.Math

    def self.bsearch(range, &cond)
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

            val = mid
            begin # inlined bsearch check, keep these in sync
              v = yield val
              case v
              when true
                satisfied = val
                smaller = true
              when false, nil
                smaller = false
              when Numeric
                cmp = v <=> 0
                return val if cmp == 0
                smaller = cmp < 0
              else
                cond_error(v)
              end
            end

            if smaller
              return integer_search(b, mid, false, &cond)
            end
            diff *= 2
          end

        elsif b.nil? && e.is_a?(Integer)
          diff = -1

          while true
            mid = b + diff

            val = mid
            begin # inlined bsearch check, keep these in sync
              v = yield val
              case v
              when true
                satisfied = val
                smaller = true
              when false, nil
                smaller = false
              when Numeric
                cmp = v <=> 0
                return val if cmp == 0
                smaller = cmp < 0
              else
                cond_error(v)
              end
            end

            unless smaller
              return integer_search(mid, e, false, &cond)
            end
            
            diff *= 2
          end

        else
          raise TypeError, "can't do binary search for #{b.class}"
        end
      end
    end

    private

    def self.float_search(b, e, excl)
      satisfied = nil

      low = double_as_long(b.nil? ? -(Float::INFINITY) : b)
      high = double_as_long(e.nil? ? Float::INFINITY : e)

      high -= 1 if excl

      org_high = high

      while low < high
        mid = if (high < 0) == (low < 0)
                low + ((high - low) / 2)
              elsif low < -high
                -((-1 - low - high) / 2 + 1)
              else
                (low + high) / 2
              end

        val = long_as_double(mid)
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
            satisfied = val
            smaller = true
          when false, nil
            smaller = false
          when Numeric
            cmp = v <=> 0
            return val if cmp == 0
            smaller = cmp < 0
          else
            cond_error(v)
          end
        end

        if smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        val = long_as_double(low)
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
            satisfied = val
            smaller = true
          when false, nil
            smaller = false
          when Numeric
            cmp = v <=> 0
            return val if cmp == 0
            smaller = cmp < 0
          else
            cond_error(v)
          end
        end

        return nil unless smaller
      end

      satisfied
    end

    def self.integer_search(low, high, excl)
      satisfied = nil

      high -= 1 if excl

      org_high = high

      while (low <=> high) < 0
        mid = (high + low) / 2

        val = mid
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
            satisfied = val
            smaller = true
          when false, nil
            smaller = false
          when Numeric
            cmp = v <=> 0
            return val if cmp == 0
            smaller = cmp < 0
          else
            cond_error(v)
          end
        end

        if smaller
          high = mid
        else
          low = mid + 1
        end
      end

      if low == org_high
        val = low
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
            satisfied = val
            smaller = true
          when false, nil
            smaller = false
          when Numeric
            cmp = v <=> 0
            return val if cmp == 0
            smaller = cmp < 0
          else
            cond_error(v)
          end
        end
        
        return nil unless smaller
      end

      satisfied
    end

    def self.double_as_long(double)
      val = Double.doubleToLongBits(Math.abs(double))
      double < 0 ? -val : val
    end

    def self.long_as_double(long)
      if long < 0
        long = -long
        -(Double.longBitsToDouble(long))
      else
        Double.longBitsToDouble(long)
      end
    end

    def self.cond_error(v)
      raise TypeError, "wrong argument type #{v.class} (must be numeric, true, false or nil)"
    end
  end
  private_constant :BSearch
end
