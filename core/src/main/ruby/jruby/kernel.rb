# This file boots the Ruby-based parts of JRuby.

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/jruby/type.rb'
load 'jruby/kernel/signal.rb'

require 'jruby'
begin
  # Try to access ProcessBuilder; if it fails, don't define our special process logic
  java.lang.ProcessBuilder # GH-1148: ProcessBuilder is not available on GAE
  load 'jruby/kernel/jruby/process_manager.rb'
rescue Exception
  warn "ProcessBuilder unavailable; using default backtick" if $VERBOSE
  # leave old backtick logic in place
end unless JRuby.runtime.posix.native? # native POSIX uses new logic for back-quote

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/kernel.rb'
load 'jruby/kernel/proc.rb'
load 'jruby/kernel/process.rb'
load 'jruby/kernel/jruby/process_util.rb'
load 'jruby/kernel/jruby/type.rb'
load 'jruby/kernel/enumerator.rb'
load 'jruby/kernel/enumerable.rb'
load 'jruby/kernel/io.rb'
load 'jruby/kernel/time.rb'
load 'jruby/kernel/gc.rb'
load 'jruby/kernel/range.rb'
load 'jruby/kernel/load_error.rb'
load 'jruby/kernel/file.rb'
load 'jruby/kernel/basicobject.rb'
load 'jruby/kernel/hash.rb'
load 'jruby/kernel/string.rb'

$" << 'thread.rb'