require 'java'
require File.expand_path('../../../build/jruby-test-classes.jar', __FILE__)
require 'rspec'

RSpec.configure do |config|
  require File.expand_path('../../../test/test_helper', __FILE__)
  config.include TestHelper
end
