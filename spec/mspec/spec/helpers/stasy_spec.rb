require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#stasy when RUBY_VERSION < 1.9" do
  before :all do
    @ruby_version = Object.const_get :RUBY_VERSION

    Object.const_set :RUBY_VERSION, "1.8.7"
  end

  after :all do
    Object.const_set :RUBY_VERSION, @ruby_version
  end

  it "returns a String when passed a String" do
    stasy("nom").should == "nom"
  end

  it "returns a String when passed a Symbol" do
    stasy(:some).should == "some"
  end

  it "returns an Array of Strings when passed an Array of Strings" do
    stasy("nom", "nom").should == ["nom", "nom"]
  end

  it "returns an Array of Strings when passed an Array of Symbols" do
    stasy(:some, :thing).should == ["some", "thing"]
  end
end

describe Object, "#stasy when RUBY_VERSION >= 1.9.0" do
  before :all do
    @ruby_version = Object.const_get :RUBY_VERSION

    Object.const_set :RUBY_VERSION, "1.9.0"
  end

  after :all do
    Object.const_set :RUBY_VERSION, @ruby_version
  end

  it "returns a Symbol when passed a String" do
    stasy("nom").should == :nom
  end

  it "returns a Symbol when passed a Symbol" do
    stasy(:some).should == :some
  end

  it "returns an Array of Symbols when passed an Array of Strings" do
    stasy("some", "thing").should == [:some, :thing]
  end

  it "returns an Array of Symbols when passed an Array of Symbols" do
    stasy(:nom, :nom).should == [:nom, :nom]
  end
end
