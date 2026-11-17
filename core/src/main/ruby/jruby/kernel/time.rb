class Time
  def deconstruct_keys(names)
    case names
    when nil
      result = {
        year: year,
        month: month,
        day: day,
        yday: yday,
        wday: wday,
        hour: hour,
        min: min,
        sec: sec,
        subsec: subsec,
        dst: dst?,
        zone: zone
      }
    when Array
      result = {}
      names.each do
        value = case it
                when :year
                  year
                when :month
                  month
                when :day
                  day
                when :yday
                  yday
                when :wday
                  wday
                when :hour
                  hour
                when :min
                  min
                when :sec
                  sec
                when :subsec
                  subsec
                when :dst
                  dst?
                when :zone
                  zone
                end
        result[it] = value if value
      end
    else
      raise TypeError, "wrong argument type #{names.class} (expected Array or nil)"
    end

    result
  end
end
