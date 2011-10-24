require 'thread.jar'

# these are loads so they don't pollute LOADED_FEATURES
load 'builtin/kernel19/mutex.rb'
load 'builtin/kernel19/thread.rb'
load 'builtin/kernel19/kernel.rb'
load 'builtin/kernel19/proc.rb'
load 'builtin/kernel19/process.rb'
load 'builtin/kernel19/jruby/process_util.rb'
load 'builtin/kernel19/enumerator.rb'
load 'builtin/kernel19/enumerable.rb'
load 'builtin/kernel19/rubygems.rb' unless JRuby::CONFIG.rubygems_disabled?