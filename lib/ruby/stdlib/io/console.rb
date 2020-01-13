if RUBY_ENGINE == 'jruby'
  require 'io/console/jruby'
else
  require 'io/console.so'
end