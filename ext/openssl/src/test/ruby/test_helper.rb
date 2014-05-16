require 'java'

$CLASSPATH << 'pkg/classes'
$CLASSPATH << Dir['lib/bcprov-*.jar'].first
$CLASSPATH << Dir['lib/bcpkix-*.jar'].first

begin
  gem 'mocha'
rescue LoadError => e
  warn "#{e} to run all tests please `gem install mocha'"
end

require 'test/unit'

begin
  if defined? MiniTest
    require "mocha/mini_test"
  else
    require "mocha/test_unit"
  end
rescue LoadError
end