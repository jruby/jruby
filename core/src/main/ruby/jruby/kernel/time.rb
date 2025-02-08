class Time
  # From stdlib time library, see https://github.com/jruby/jruby/issues/8476
  unless method_defined?(:xmlschema)
    #
    # Returns a string which represents the time as a dateTime defined by XML
    # Schema:
    #
    #   CCYY-MM-DDThh:mm:ssTZD
    #   CCYY-MM-DDThh:mm:ss.sssTZD
    #
    # where TZD is Z or [+-]hh:mm.
    #
    # If self is a UTC time, Z is used as TZD.  [+-]hh:mm is used otherwise.
    #
    # +fraction_digits+ specifies a number of digits to use for fractional
    # seconds.  Its default value is 0.
    #
    #     require 'time'
    #
    #     t = Time.now
    #     t.iso8601  # => "2011-10-05T22:26:12-04:00"
    #
    # You must require 'time' to use this method.
    #
    def xmlschema(fraction_digits=0)
      fraction_digits = fraction_digits.to_i
      s = strftime("%FT%T")
      if fraction_digits > 0
        s << strftime(".%#{fraction_digits}N")
      end
      s << (utc? ? 'Z' : strftime("%:z"))
    end
  end
  alias iso8601 xmlschema unless method_defined?(:iso8601)
end