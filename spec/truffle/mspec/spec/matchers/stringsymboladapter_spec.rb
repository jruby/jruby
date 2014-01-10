require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/matchers'

describe StringSymbolAdapter, "#convert_name" do
  include StringSymbolAdapter

  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_version = Object.const_get :RUBY_VERSION
  end

  after :each do
    Object.const_set :RUBY_VERSION, @ruby_version
  end

  it "converts the name to a string if RUBY_VERSION < 1.9" do
    Object.const_set :RUBY_VERSION, "1.8.6"

    convert_name("name").should == "name"
    convert_name(:name).should  == "name"
  end

  it "converts the name to a symbol if RUBY_VERSION >= 1.9" do
    Object.const_set :RUBY_VERSION, "1.9.0"

    convert_name("name").should == :name
    convert_name(:name).should  == :name
  end
end
