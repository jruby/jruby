# This file boots the Ruby-based parts of JRuby.

module JRuby
  autoload :ProcessUtil, 'jruby/kernel/jruby/process_util.rb'
  autoload :Type, 'jruby/kernel/jruby/type.rb'
end

begin
  # Try to access ProcessBuilder; if it fails, don't define our special process logic
  load 'uri:classloader:jruby/kernel/jruby/process_manager.rb'
rescue Exception # java.lang.ProcessBuilder not available
  warn "ProcessBuilder unavailable; using default backtick" if $VERBOSE
  JRuby.send(:remove_const, :ProcessManager) rescue nil
  # leave old backtick logic in place
end unless JRuby::Util.native_posix? # native POSIX uses new logic for back-quote

# These are loads so they don't pollute LOADED_FEATURES
load 'uri:classloader:jruby/kernel/signal.rb'
load 'uri:classloader:jruby/kernel/kernel.rb'
load 'uri:classloader:jruby/kernel/proc.rb'
load 'uri:classloader:jruby/kernel/process.rb'
load 'uri:classloader:jruby/kernel/enumerator.rb'
load 'uri:classloader:jruby/kernel/enumerable.rb'
load 'uri:classloader:jruby/kernel/io.rb'
load 'uri:classloader:jruby/kernel/gc.rb'
load 'uri:classloader:jruby/kernel/range.rb'
load 'uri:classloader:jruby/kernel/file.rb'
load 'uri:classloader:jruby/kernel/method.rb'
load 'uri:classloader:jruby/kernel/thread.rb'
load 'uri:classloader:jruby/kernel/integer.rb'
load 'uri:classloader:jruby/kernel/time.rb'
load 'uri:classloader:jruby/kernel/string.rb'
