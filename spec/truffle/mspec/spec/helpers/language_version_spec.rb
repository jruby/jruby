require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#language_version" do
  before :all do
    @ruby_version = Object.const_get :RUBY_VERSION

    Object.const_set :RUBY_VERSION, "8.2.3"

    dir = "#{File.expand_path('../', __FILE__)}/versions"
    @method82  = "#{dir}/method_8.2.rb"
    @method823 = "#{dir}/method_8.2.3.rb"
  end

  after :all do
    Object.const_set :RUBY_VERSION, @ruby_version
  end

  it "loads the most version-specific file if it exists" do
    File.should_receive(:exists?).with(@method823).and_return(true)
    should_receive(:require).with(@method823)
    language_version __FILE__, "method"
  end

  it "loads a less version-specific file if it exists" do
    File.should_receive(:exists?).with(@method823).and_return(false)
    File.should_receive(:exists?).with(@method82).and_return(true)
    should_receive(:require).with(@method82)
    language_version __FILE__, "method"
  end

  it "does not load the file if it does not exist" do
    File.should_receive(:exists?).with(@method82).and_return(false)
    File.should_receive(:exists?).with(@method823).and_return(false)
    should_not_receive(:require).with(@method82)
    should_not_receive(:require).with(@method823)
    language_version __FILE__, "method"
  end
end
