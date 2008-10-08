require 'rbconfig'

WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

if (WINDOWS)
  @devnull = 'NUL:'
else
  @devnull = '/dev/null'
end

def quiet( &block )
  io = [STDOUT.dup, STDERR.dup]
  STDOUT.reopen @devnull
  STDERR.reopen @devnull
  block.call
ensure
  STDOUT.reopen io.first
  STDERR.reopen io.last
  $stdout, $stderr = STDOUT, STDERR
end

quiet { puts 'foo' }
quiet { puts 'foo' }
puts 'foo'
