# This is the primary kernel file, containing loads for subkernels that are
# common to all supported versions of Ruby. Subsequent version-specific kernel
# loads are expected to add and patch the behaviors loaded by this file.

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/jruby/generator.rb'
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