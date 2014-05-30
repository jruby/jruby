def with_timezone(name, offset = nil, daylight_saving_zone = "")
  zone = name.dup

  if offset
    # TZ convention is backwards
    offset = -offset

    zone << offset.to_s
    zone << ":00:00"
  end
  zone << daylight_saving_zone

  old = ENV["TZ"]
  ENV["TZ"] = zone

  begin
    yield
  ensure
    ENV["TZ"] = old
  end
end
