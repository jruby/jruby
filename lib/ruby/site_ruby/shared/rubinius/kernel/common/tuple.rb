##
# The tuple data type.
# A simple storage class. Created to contain a fixed number of elements.
#
# Not designed to be subclassed, as it does not call initialize
# on new instances.

module Rubinius
  class Tuple

    include Enumerable

    def self.[](*args)
      start = args.start
      tot = args.size
      return new(tot).copy_from(args.tuple, start, tot, 0)
    end

    def to_s
      "#<#{self.class}:0x#{object_id.to_s(16)} #{fields} elements>"
    end

    def each
      i = 0
      t = fields
      while i < t
        yield at(i)
        i += 1
      end
      self
    end

    def ==(tup)
      return super unless tup.kind_of?(Tuple)

      t = fields()

      return false unless t == tup.size

      i = 0
      while i < t
        return false unless at(i) == tup.at(i)
        i += 1
      end

      return true
    end

    def +(o)
      t = Tuple.new(size + o.size)
      t.copy_from(self,0,size,0)
      t.copy_from(o,0,o.size,size)
      t
    end

    def inspect
      str = "#<#{self.class}"
      if fields == 0
        str << " empty>"
      else
        str << ": #{join(", ", :inspect)}>"
      end
      return str
    end

    def join(sep, meth=:to_s)
      join_upto(sep, fields, meth)
    end

    def join_upto(sep, count, meth=:to_s)
      str = ""
      return str if count == 0 or empty?

      count = fields if count >= fields
      count -= 1
      i = 0
      while i < count
        str.append at(i).__send__(meth)
        str.append sep.dup
        i += 1
      end

      str.append at(count).__send__(meth)
      return str
    end

    def ===(other)
      return false unless Tuple === other and fields == other.fields
      i = 0
      while i < fields
        return false unless at(i) === other.at(i)
        i += 1
      end
      true
    end

    def to_a
      ary = []
      ary.tuple = dup
      ary.total = fields
      ary.start = 0
      return ary
    end

    def shift
      return self unless fields > 0
      t = Tuple.new(fields-1)
      t.copy_from self, 1, fields-1, 0
      return t
    end

    # Swap elements of the two indexes.
    def swap(a, b)
      temp = at(a)
      put a, at(b)
      put b, temp
    end

    alias_method :size, :fields
    alias_method :length, :fields

    def empty?
      size == 0
    end

    def first
      at(0)
    end

    def last
      at(fields - 1)
    end

    # Marshal support - _dump / _load are deprecated so eventually we should figure
    # out a better way.
    def _dump(depth)
      # TODO use depth
      Marshal.dump to_a
    end

    def self._load(str)
      ary = Marshal.load(str)
      t = new(ary.size)
      ary.each_with_index { |obj, idx| t[idx] = obj }
      return t
    end
  end
end
