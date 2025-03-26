class Range
  def bsearch(&cond)
    return to_enum(:bsearch) unless block_given?

    b = self.begin
    e = self.end

    if b.is_a?(Float) || e.is_a?(Float)
      BSearch.float_search(b, e, exclude_end?, &cond)
    else
      BSearch.integer_search(b, e, exclude_end?, &cond)
    end
  end

  class BSearch
    def self.float_search(b, e, excl)
      satisfied = nil

      low = double_as_long(b.nil? ? NEGATIVE_INFINITY : b)
      high = double_as_long(e.nil? ? INFINITY : e)

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

    def self.integer_search(b, e, excl, &cond)
      b_int = b.is_a?(Integer)
      e_int = e.is_a?(Integer)

      if b_int
        if e_int
          return binary_search(b, e, excl, &cond)
        elsif e.nil?
          return to_enum(:bsearch) unless block_given?
          return integer_begin(b, &cond)
        end
      elsif e_int
        if b.nil?
          return to_enum(:bsearch) unless block_given?
          return integer_end(e, &cond)
        end
      end

      raise TypeError, "can't do binary search for #{b.class}"
    end

    private

    def self.binary_search(low, high, excl)
      return to_enum(:bsearch) unless block_given?

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

    def self.integer_begin(b, &cond)
      diff = 1

      while true
        mid = b + diff

        val = mid
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
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

        return binary_search(b, mid, false, &cond) if smaller

        diff *= 2
      end
    end

    def self.integer_end(e, &cond)
      diff = -1

      while true
        mid = e + diff

        val = mid
        begin # inlined bsearch check, keep these in sync
          v = yield val
          case v
          when true
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

        return binary_search(mid, e, false, &cond) unless smaller

        diff *= 2
      end
    end

    def self.double_as_long(double)
      below_zero = double < 0

      double = abs(double) if below_zero

      long = double_to_long_bits(double)

      below_zero ? -long : long
    end

    def self.long_as_double(long)
      below_zero = long < 0

      long = -long if below_zero

      double = long_bits_to_double(long)

      below_zero ? -double : double
    end

    def self.cond_error(v)
      raise TypeError, "wrong argument type #{v.class} (must be numeric, true, false or nil)"
    end
  end
  private_constant :BSearch

  def overlap?(other)
    raise TypeError, "wrong argument type #{other.class} (expected Range)" unless other.is_a?(Range)

    self_begin = self.begin || NEGATIVE_INFINITY
    self_end = self.end || INFINITY
    self_exclude = exclude_end?
    other_begin = other.begin || NEGATIVE_INFINITY
    other_end = other.end || INFINITY
    other_exclude = other.exclude_end?

    !!(__overlap(other_begin, self_end, self_exclude) &&
      __overlap(self_begin, other_end, other_exclude) &&
      __overlap(self_begin, self_end, self_exclude) &&
      __overlap(other_begin, other_end, other_exclude))
  end

  private def __overlap(from, to, excl)
    less = from <=> to
    less = +1 if less == 0 && excl
    less && less <= 0
  end

  INFINITY = Float::INFINITY
  NEGATIVE_INFINITY = -Float::INFINITY
  private_constant :NEGATIVE_INFINITY, :INFINITY
end
