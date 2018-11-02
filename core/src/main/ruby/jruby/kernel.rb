# This file boots the Ruby-based parts of JRuby.

require 'jruby' # NOTE: consider not doing this, require 'java' is enough!
require 'jruby/util' # stuff boot depends on (compared to a full require 'jruby')

module JRuby
  autoload :ProcessUtil, 'jruby/kernel/jruby/process_util.rb'
  autoload :Type, 'jruby/kernel/jruby/type.rb'
end

begin
  # Try to access ProcessBuilder; if it fails, don't define our special process logic
  java.lang.ProcessBuilder # GH-1148: ProcessBuilder is not available on GAE
  load 'jruby/kernel/jruby/process_manager.rb'
rescue Exception
  warn "ProcessBuilder unavailable; using default backtick" if $VERBOSE
  # leave old backtick logic in place
end unless JRuby::Util.native_posix? # native POSIX uses new logic for back-quote

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/signal.rb'
load 'jruby/kernel/kernel.rb'
load 'jruby/kernel/proc.rb'
load 'jruby/kernel/process.rb'
load 'jruby/kernel/enumerator.rb'
load 'jruby/kernel/enumerable.rb'
load 'jruby/kernel/io.rb'
load 'jruby/kernel/time.rb'
load 'jruby/kernel/gc.rb'
load 'jruby/kernel/range.rb'
load 'jruby/kernel/file.rb'
load 'jruby/kernel/string.rb'

$" << 'thread.rb'
