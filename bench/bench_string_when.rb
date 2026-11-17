10.times do
  t = Time.now
  i = 0; while i < 100_000_000
    case RUBY_ENGINE
    when "foo"
    when "bar"
    when "baz"
    when "quux"
    when "jruby"
    end
    i += 1
  end
  puts Time.now - t
end