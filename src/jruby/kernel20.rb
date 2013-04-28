# This is the Ruby 2.0-specific kernel file.

# Currently, all 1.9 features are in 2.0. We will need to
# differentiate when there are features from 1.9 removed
# in 2.0.

# These are loads so they don't pollute LOADED_FEATURES
load 'jruby/kernel19.rb'
load 'jruby/kernel20/enumerable.rb'
load 'jruby/kernel20/range.rb'
load 'jruby/kernel20/load_error.rb'