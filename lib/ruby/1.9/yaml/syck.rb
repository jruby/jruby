# $Id$
#
# = yaml/syck.rb:
#

require 'stringio'
require 'syck.so' unless RUBY_ENGINE == 'jruby'
require 'yecht' if RUBY_ENGINE == 'jruby'
require 'syck/error'
require 'syck/syck'
require 'syck/tag'
require 'syck/stream'
require 'syck/constants'
require 'syck/rubytypes'
require 'syck/types'

if RUBY_ENGINE == 'jruby'
  # Many of Syck's features are under YAML::Yecht
  module Syck
    include YAML::Yecht
  end
end