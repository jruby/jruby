# This is the Ruby 1.9-specific kernel file.

# Thread features are always available in 1.9.
require 'thread.jar'

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel21/thread.rb'
load 'jruby/kernel21/kernel.rb'
load 'jruby/kernel21/proc.rb'
load 'jruby/kernel21/process.rb'
load 'jruby/kernel21/jruby/process_util.rb'
load 'jruby/kernel21/jruby/type.rb'
load 'jruby/kernel21/enumerator.rb'
load 'jruby/kernel21/enumerable.rb'
load 'jruby/kernel21/io.rb'
load 'jruby/kernel21/time.rb'
load 'jruby/kernel21/gc.rb'
load 'jruby/kernel21/encoding/converter.rb'
load 'jruby/kernel21/range.rb'
load 'jruby/kernel21/load_error.rb'

load 'jruby/kernel21/rubygems.rb' unless JRuby::CONFIG.rubygems_disabled?
