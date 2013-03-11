# Copyright (c) 2009 Marc-Andre Lafortune
# 
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
# 
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
class Range
  def bsearch
    return to_enum(:bsearch) unless block_given?
    from = self.begin
    to   = self.end
    unless from.is_a?(Numeric) && to.is_a?(Numeric)
      raise TypeError, "can't do binary search for #{from.class}"
    end

    midpoint = nil
    if from.is_a?(Integer) && to.is_a?(Integer)
      convert = Proc.new{ midpoint }
    else
      map = Proc.new do |pk, unpk, nb|
        result, = [nb.abs].pack(pk).unpack(unpk)
        nb < 0 ? -result : result
      end
      from = map['D', 'q', to.to_f]
      to   = map['D', 'q', to.to_f]
      convert = Proc.new{ map['q', 'D', midpoint] }
    end
    to -= 1 if exclude_end?
    satisfied = nil
    while from <= to do
      midpoint = (from + to).div(2)
      result = yield(cur = convert.call)
      case result
      when Numeric
        return cur if result == 0
        result = result < 0
      when true
        satisfied = cur
      when nil, false
        # nothing to do
      else
        raise TypeError, "wrong argument type #{result.class} (must be numeric, true, false or nil)"
      end

      if result
        to = midpoint - 1
      else
        from = midpoint + 1
      end
    end
    satisfied
  end
end