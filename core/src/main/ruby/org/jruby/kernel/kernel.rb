# This file boots the Ruby-based parts of JRuby.

module JRuby
  autoload :ProcessUtil, 'org/jruby/kernel/kernel/jruby/process_util.rb'
  autoload :Type, 'org/jruby/kernel/kernel/jruby/type.rb'
end

begin
  # Try to access ProcessBuilder; if it fails, don't define our special process logic
  load 'org/jruby/kernel/kernel/jruby/process_manager.rb'
rescue Exception # java.lang.ProcessBuilder not available
  warn "ProcessBuilder unavailable; using default backtick" if $VERBOSE
  JRuby.send(:remove_const, :ProcessManager) rescue nil
  # leave old backtick logic in place
end unless JRuby::Util.native_posix? # native POSIX uses new logic for back-quote

# These are loads so they don't pollute LOADED_FEATURES
load 'org/jruby/kernel/kernel/signal.rb'
load 'org/jruby/kernel/kernel/kernel.rb'
load 'org/jruby/kernel/kernel/proc.rb'
load 'org/jruby/kernel/kernel/process.rb'
load 'org/jruby/kernel/kernel/enumerator.rb'
load 'org/jruby/kernel/kernel/enumerable.rb'
load 'org/jruby/kernel/kernel/io.rb'
load 'org/jruby/kernel/kernel/gc.rb'
load 'org/jruby/kernel/kernel/range.rb'
load 'org/jruby/kernel/kernel/file.rb'
load 'org/jruby/kernel/kernel/method.rb'
load 'org/jruby/kernel/kernel/thread.rb'
load 'org/jruby/kernel/kernel/integer.rb'
load 'org/jruby/kernel/kernel/time.rb'
