# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Time
  include Comparable

  def self.now
    Truffle.primitive :time_s_now
    raise PrimitiveFailure, "Time.now primitive failed"
  end

  def self.duplicate(other)
    Truffle.primitive :time_s_dup
    raise ArgumentError, "descriptors reference invalid time"
  end

  def self.specific(sec, nsec, from_gmt, offset)
    Truffle.primitive :time_s_specific
    raise ArgumentError, "descriptors reference invalid time"
  end

  def dup
    self.class.duplicate(self)
  end

  def seconds
    Truffle.primitive :time_seconds
    raise PrimitiveFailure, "Time#second primitive failed"
  end

  def usec
    Truffle.primitive :time_useconds
    raise PrimitiveFailure, "Time#usec primitive failed"
  end

  def to_a
    Truffle.primitive :time_decompose
    raise PrimitiveFailure, "Time#to_a primitive failed"
  end

  def strftime(format)
    Truffle.invoke_primitive :time_strftime, self, StringValue(format)
  end

  MonthValue = {
    'JAN' => 1, 'FEB' => 2, 'MAR' => 3, 'APR' => 4, 'MAY' => 5, 'JUN' => 6,
    'JUL' => 7, 'AUG' => 8, 'SEP' => 9, 'OCT' =>10, 'NOV' =>11, 'DEC' =>12
  }

  def self.at(sec, usec=undefined)
    if undefined.equal?(usec)
      if sec.kind_of?(Time)
        return duplicate(sec)
      elsif sec.kind_of?(Integer)
        return specific(sec, 0, false, nil)
      end
    end

    if sec.kind_of?(Time) && usec.kind_of?(Integer)
      raise TypeError, "can't convert Time into an exact number"
    end

    usec = 0 if undefined.equal?(usec)

    s = Rubinius::Type.coerce_to_exact_num(sec)
    u = Rubinius::Type.coerce_to_exact_num(usec)

    sec       = s.to_i
    nsec_frac = s % 1.0

    sec -= 1 if s < 0 && nsec_frac > 0
    nsec = (nsec_frac * 1_000_000_000 + 0.5).to_i + (u * 1000).to_i

    sec += nsec / 1_000_000_000
    nsec %= 1_000_000_000

    specific(sec, nsec, false, nil)
  end

  def self.from_array(sec, min, hour, mday, month, year, nsec, is_dst, from_gmt, utc_offset)
    Truffle.primitive :time_s_from_array

    if sec.kind_of?(String)
      sec = sec.to_i
    elsif nsec
      sec = Rubinius::Type.coerce_to(sec || 0, Integer, :to_int)
    else
      s = Rubinius::Type.coerce_to_exact_num(sec || 0)

      sec       = s.to_i
      nsec_frac = s % 1.0

      if s < 0 && nsec_frac > 0
        sec -= 1
      end

      nsec = (nsec_frac * 1_000_000_000 + 0.5).to_i
    end

    nsec ||= 0
    sec += nsec / 1_000_000_000
    nsec %= 1_000_000_000

    from_array(sec, min, hour, mday, month, year, nsec, is_dst, from_gmt, utc_offset)
  end

  def self.new(year=undefined, month=nil, day=nil, hour=nil, minute=nil, second=nil, utc_offset=nil)
    if undefined.equal?(year)
      now
    elsif utc_offset == nil
      compose(:local, year, month, day, hour, minute, second)
    else
      compose(Rubinius::Type.coerce_to_utc_offset(utc_offset), year, month, day, hour, minute, second)
    end
  end

  def inspect
    if gmt?
      str = strftime("%Y-%m-%d %H:%M:%S UTC")
    else
      str = strftime("%Y-%m-%d %H:%M:%S %z")
    end

    str.force_encoding Encoding::US_ASCII
  end

  alias_method :to_s, :inspect

  def nsec
    Truffle.primitive :time_nseconds
    raise PrimitiveFailure, "Time#nsec primitive failed"
  end

  def nsec=(nanoseconds)
    Truffle.primitive :time_set_nseconds
    raise PrimitiveFailure, "Time#nsec= primitive failed"
  end
  private :nsec=

  alias_method :tv_nsec, :nsec

  def subsec
    if nsec == 0
      0
    else
      Rational(nsec, 1_000_000_000)
    end
  end

  def sunday?
    wday == 0
  end

  def monday?
    wday == 1
  end

  def tuesday?
    wday == 2
  end

  def wednesday?
    wday == 3
  end

  def thursday?
    wday == 4
  end

  def friday?
    wday == 5
  end

  def saturday?
    wday == 6
  end

  def to_r
    (seconds + subsec).to_r
  end

  def getlocal(offset=nil)
    dup.localtime(offset)
  end

  def eql?(other)
    other.kind_of?(Time) and seconds == other.seconds and nsec == other.nsec
  end

  def <=>(other)
    if other.kind_of? Time
      (seconds <=> other.seconds).nonzero? or (nsec <=> other.nsec)
    else
      r = (other <=> self)
      return nil if r == nil
      return -1 if r > 0
      return  1 if r < 0
      0
    end
  end

  class << self
    def compose_deal_with_year(year)
      year
    end
    private :compose_deal_with_year
  end

  #--
  # TODO: doesn't load ivars
  #++

  def self._load(data)
    raise TypeError, 'marshaled time format differ' unless data.bytesize == 8

    major, minor = data.unpack 'VV'

    if (major & (1 << 31)) == 0 then
      at major, minor
    else
      major &= ~(1 << 31)

      is_gmt =  (major >> 30) & 0x1
      year   = ((major >> 14) & 0xffff) + 1900
      mon    = ((major >> 10) & 0xf) + 1
      mday   =  (major >>  5) & 0x1f
      hour   =  major         & 0x1f

      min   =  (minor >> 26) & 0x3f
      sec   =  (minor >> 20) & 0x3f
      isdst = false

      usec = minor & 0xfffff

      time = gm year, mon, mday, hour, min, sec, usec
      time.localtime if is_gmt.zero?
      time
    end
  end

  private_class_method :_load

  #--
  # TODO: doesn't dump ivars
  #++

  def _dump(limit = nil)
    tm = getgm.to_a

    if (year & 0xffff) != year || year < 1900 then
      raise ArgumentError, "year too big to marshal: #{year}"
    end

    gmt = gmt? ? 1 : 0

    major =  1             << 31 | # 1 bit
             gmt           << 30 | # 1 bit
            (tm[5] - 1900) << 14 | # 16 bits
            (tm[4] - 1)    << 10 | # 4 bits
             tm[3]         <<  5 | # 5 bits
             tm[2]                 # 5 bits
    minor =  tm[1]   << 26 | # 6 bits
             tm[0]   << 20 | # 6 bits
             usec # 20 bits

    [major, minor].pack 'VV'
  end

  private :_dump

  def self.compose(offset, p1, p2=nil, p3=nil, p4=nil, p5=nil, p6=nil, p7=nil,
                   yday=undefined, is_dst=undefined, tz=undefined)
    if undefined.equal?(tz)
      unless undefined.equal?(is_dst)
        raise ArgumentError, "wrong number of arguments (9 for 1..8)"
      end

      y = p1
      m = p2
      d = p3
      hr = p4
      min = p5
      sec = p6
      usec = p7
      is_dst = -1
    else
      y = p6
      m = p5
      d = p4
      hr = p3
      min = p2
      sec = p1
      usec = 0
      is_dst = is_dst ? 1 : 0
    end

    if m.kind_of?(String) or m.respond_to?(:to_str)
      m = StringValue(m)
      m = MonthValue[m.upcase] || m.to_i

      raise ArgumentError, "month argument out of range" unless m
    else
      m = Rubinius::Type.coerce_to(m || 1, Integer, :to_int)
    end

    y   = y.kind_of?(String)   ? y.to_i   : Rubinius::Type.coerce_to(y,        Integer, :to_int)
    d   = d.kind_of?(String)   ? d.to_i   : Rubinius::Type.coerce_to(d   || 1, Integer, :to_int)
    hr  = hr.kind_of?(String)  ? hr.to_i  : Rubinius::Type.coerce_to(hr  || 0, Integer, :to_int)
    min = min.kind_of?(String) ? min.to_i : Rubinius::Type.coerce_to(min || 0, Integer, :to_int)

    nsec = nil

    if usec.kind_of?(String)
      nsec = usec.to_i * 1000
    elsif usec
      nsec = (usec * 1000).to_i
    end

    y = compose_deal_with_year(y)

    case offset
      when :utc
        is_dst = -1
        is_utc = true
        offset = nil
      when :local
        is_utc = false
        offset = nil
      else
        is_dst = -1
        is_utc = false
    end

    from_array(sec, min, hr, d, m, y, nsec, is_dst, is_utc, offset)
  end

  def self.local(*args)
    compose(:local, *args)
  end

  def self.gm(*args)
    compose(:utc, *args)
  end

  def succ
    self + 1
  end

  def asctime
    strftime("%a %b %e %H:%M:%S %Y")
  end

  def sec
    to_a[0]
  end

  def min
    to_a[1]
  end

  def hour
    to_a[2]
  end

  def day
    to_a[3]
  end

  def mon
    to_a[4]
  end

  def year
    to_a[5]
  end

  def wday
    to_a[6]
  end

  def yday
    to_a[7]
  end

  def dst?
    to_a[8]
  end

  def zone
    zone = to_a[9]

    if zone && Encoding.default_internal
      zone.encode Encoding.default_internal
    else
      zone
    end
  end

  alias_method :to_i, :seconds

  def gmt_offset
    Truffle.primitive :time_utc_offset
    raise PrimitiveFailure, "Time#gmt_offset primitive failed"
  end

  def getgm
    dup.gmtime
  end

  def hash
    seconds ^ usec
  end

  class << self
    alias_method :mktime, :local
    alias_method :utc,    :gm
  end

  alias_method :utc?,       :gmt?
  alias_method :month,      :mon
  alias_method :ctime,      :asctime
  alias_method :mday,       :day
  alias_method :to_i,       :seconds
  alias_method :tv_sec,     :seconds
  alias_method :tv_usec,    :usec
  alias_method :utc,        :gmtime
  alias_method :isdst,      :dst?
  alias_method :utc_offset, :gmt_offset
  alias_method :gmtoff,     :gmt_offset
  alias_method :getutc,     :getgm

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
