# This is the primary kernel file, containing loads for subkernels that are
# common to all supported versions of Ruby. Subsequent version-specific kernel
# loads are expected to add and patch the behaviors loaded by this file.

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel/generator.rb'
load 'jruby/kernel/signal.rb'

# Java 7 process launching support
load 'jruby/kernel/jruby/process_manager.rb' if ENV_JAVA['java.specification.version'] == '1.7'