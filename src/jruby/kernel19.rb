# This is the Ruby 1.9-specific kernel file.

# Thread features are always available in 1.9.
require 'thread.jar'

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel19/thread.rb'
load 'jruby/kernel19/kernel.rb'
load 'jruby/kernel19/proc.rb'
load 'jruby/kernel19/process.rb'
load 'jruby/kernel19/jruby/process_util.rb'
load 'jruby/kernel19/jruby/type.rb'
load 'jruby/kernel19/enumerator.rb'
load 'jruby/kernel19/enumerable.rb'
load 'jruby/kernel19/io.rb'
load 'jruby/kernel19/time.rb'
load 'jruby/kernel19/gc.rb'
load 'jruby/kernel19/encoding/converter.rb'

load 'jruby/kernel19/rubygems.rb' unless JRuby::CONFIG.rubygems_disabled?
