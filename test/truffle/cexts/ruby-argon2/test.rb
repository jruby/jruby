ARGON2_HOME = ARGV[0]
ENV['TEST_CHECKS'] = '1'

require "#{ARGON2_HOME}/test/jruby+truffle/run"
