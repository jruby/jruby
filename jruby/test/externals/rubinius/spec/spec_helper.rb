$:.unshift File.dirname(__FILE__)

# JRuby can run RSpec fine, so we just use RSpec here
require 'rspec_helper'
=begin
  if ENV['USE_RSPEC'] == '1'
    require 'rspec_helper'
  else
    require 'mspec_helper'
  end
rescue
  require 'mspec_helper'
=end
