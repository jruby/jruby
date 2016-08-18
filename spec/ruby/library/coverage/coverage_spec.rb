require File.expand_path('../../spec_helper', __dir__)
config_file = File.expand_path('fixtures/start_coverage.rb', __dir__)

describe "Coverage#result" do
  it "gives the covered files as a hash with arrays" do
    $LOADED_FEATURES.delete(config_file)
    require 'coverage'
    Coverage.start
p    require config_file.chomp('.rb')
    result = Coverage.result
p result
    result.should == {config_file => [1, 1, 1]}
  end

  it "should list coverage for the required file starting coverage" do
    $LOADED_FEATURES.delete(config_file)
    require config_file.chomp('.rb')
    result = Coverage.result
p result
    result.should == {config_file => []}
  end

  it "should list coverage for the loaded file starting coverage" do
    load config_file
    result = Coverage.result
p result
    result.should == {config_file => []}
  end
end
