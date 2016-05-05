# Extensions to Ruby classes

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/java/core_ext/module.rb'
load 'jruby/java/core_ext/object.rb'
#load 'jruby/java/core_ext/kernel.rb' - moved to org.jruby.javasupport.ext.Kernel