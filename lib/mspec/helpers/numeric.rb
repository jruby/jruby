require 'mspec/guards/guard'

class Object
  def nan_value
    0/0.0
  end

  def infinity_value
    1/0.0
  end

  def bignum_value(plus=0)
    0x8000_0000_0000_0000 + plus
  end

  # This is a bit hairy, but we need to be able to write specs that cover the
  # boundary between Fixnum and Bignum for operations like Fixnum#<<. Since
  # this boundary is implementation-dependent, we use these helpers to write
  # specs based on the relationship between values rather than specific
  # values.
  guard = SpecGuard.new

  if guard.standard? or guard.implementation? :topaz
    if guard.wordsize? 32
      def fixnum_max
        (2**30) - 1
      end

      def fixnum_min
        -(2**30)
      end
    elsif guard.wordsize? 64
      def fixnum_max
        (2**62) - 1
      end

      def fixnum_min
        -(2**62)
      end
    end
  elsif guard.implementation? :opal
    def fixnum_max
      Integer::MAX
    end

    def fixnum_min
      Integer::MIN
    end
  elsif guard.implementation? :rubinius
    def fixnum_max
      Fixnum::MAX
    end

    def fixnum_min
      Fixnum::MIN
    end
  elsif guard.implementation?(:jruby) || guard.implementation?(:truffleruby)
    # Values from jruby/test/testFixnumBignumAutoconversion.rb
    def fixnum_max
      9223372036854775807
    end

    def fixnum_min
      -9223372036854775808
    end
  else
    def fixnum_max
      raise "unknown implementation for fixnum_max() helper"
    end

    def fixnum_min
      raise "unknown implementation for fixnum_min() helper"
    end
  end
end
