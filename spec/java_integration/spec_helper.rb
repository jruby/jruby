require 'java'
require File.expand_path('../../../build/jruby-test-classes.jar', __FILE__)
require 'spec'

Spec::Runner.configure do |config|
  config.append_before :suite do
    require File.expand_path('../../../test/test_helper', __FILE__)
    include TestHelper
  end

  # config.after :each do
  # end
end
