# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Time

  def to_f
    seconds + nsec * 0.000000001 # Truffle: optimized
  end

  def localtime(offset = nil)
    if offset
      localtime_internal Rubinius::Type.coerce_to_utc_offset(offset)
    else
      localtime_internal
    end

    self
  end

  def +(other)
    raise TypeError, 'time + time?' if other.kind_of?(Time)

    case other = Rubinius::Type.coerce_to_exact_num(other)
    when Integer
      other_sec = other
      other_nsec = 0
    else
      other_sec, nsec_frac = other.divmod(1)
      other_nsec = (nsec_frac * 1_000_000_000).to_i
    end

    # Don't use self.class, MRI doesn't honor subclasses here
    dup_internal(Time).add_internal! other_sec, other_nsec
  end

  def -(other)
    if other.kind_of?(Time)
      return (seconds - other.seconds) + ((nsec - other.nsec) * 0.000000001)
    end

    case other = Rubinius::Type.coerce_to_exact_num(other)
    when Integer
      other_sec = other
      other_nsec = 0
    else
      other_sec, nsec_frac = other.divmod(1)
      other_nsec = (nsec_frac * 1_000_000_000 + 0.5).to_i
    end

    # Don't use self.class, MRI doesn't honor subclasses here
    dup_internal(Time).add_internal! -other_sec, -other_nsec
  end

  def round(places = 0)
    return dup if nsec == 0

    roundable_time = (to_i + subsec.to_r).round(places)

    sec = roundable_time.floor
    nano = ((roundable_time - sec) * 1_000_000_000).floor

    dup_internal(Time).add_internal! sec - seconds, nano - nsec
  end

  def dup
    dup_internal self.class
  end

  def self.duplicate(other)
    other.dup
  end
end
