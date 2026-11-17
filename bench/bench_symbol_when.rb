100``.times do
  sym = RUBY_ENGINE.to_sym
  t = Time.now
  i = 0; while i < 100_000_000
    case sym
    when :foo
    when :bar
    when :baz
    when :quux
    when :jruby
    end
    i += 1
  end
  puts Time.now - t
end