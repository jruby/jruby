# This file boots the Ruby-based parts of JRuby.

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/jruby/type.rb'
load 'jruby/kernel/signal.rb'

# Java 7 process launching support
spec_version = ENV_JAVA['java.specification.version']

# GH-1148: ProcessBuilder is not available on GAE
begin
  # Try to access ProcessBuilder; if it fails, don't define our special process logic
  java.lang.ProcessBuilder
  load 'jruby/kernel/jruby/process_manager.rb' if spec_version && spec_version >= '1.7'
rescue Exception
  warn "ProcessBuilder unavailable; using default backtick" if $VERBOSE
  # leave old backtick logic in place
end

require 'thread.jar'

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/thread.rb'
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

load 'jruby/kernel/rubygems.rb' unless JRuby::CONFIG.rubygems_disabled?
