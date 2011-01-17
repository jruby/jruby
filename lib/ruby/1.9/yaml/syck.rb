# $Id$
#
# = yaml/syck.rb:
#

require 'stringio'
require 'yecht'
require 'syck/error'
require 'syck/syck'
require 'syck/tag'
require 'syck/stream'
require 'syck/constants'
require 'syck/rubytypes'
require 'syck/types'

# Many of Syck's features are under YAML::Yecht
module Syck
  include YAML::Yecht
end