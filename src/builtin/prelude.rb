# prelude is called early in bootstrap for 1.9 so we need to load native
# thread bits to ensure our native classes are wired up instead.
require 'thread.jar'

# these are loads so they don't pollute LOADED_FEATURES
load 'builtin/kernel19/mutex.rb'
load 'builtin/kernel19/thread.rb'
load 'builtin/kernel19/kernel.rb'
load 'builtin/kernel19/proc.rb'
load 'builtin/kernel19/process.rb'
load 'builtin/kernel19/jruby/process_util.rb'