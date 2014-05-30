require 'pp'
require 'mspec/helpers/io'
require 'mspec/helpers/scratch'

# Remove this when MRI has intelligent warnings
$VERBOSE = nil unless $VERBOSE

class MOSConfig < Hash
  def initialize
    self[:includes]  = []
    self[:requires]  = []
    self[:flags]     = []
    self[:options]   = []
    self[:includes]  = []
    self[:excludes]  = []
    self[:patterns]  = []
    self[:xpatterns] = []
    self[:tags]      = []
    self[:xtags]     = []
    self[:atags]     = []
    self[:astrings]  = []
    self[:target]    = 'ruby'
    self[:command]   = nil
    self[:ltags]     = []
    self[:files]     = []
    self[:launch]    = []
  end
end

def new_option
  config = MOSConfig.new
  return MSpecOptions.new("spec", 20, config), config
end

# Just to have an exception name output not be "Exception"
class MSpecExampleError < Exception
end
