require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

describe "JRuby#compile" do
  it "should produce a CompiledScript instance" do
    compiled = JRuby.compile("foo = 1")
    compiled.should be_kind_of JRuby::CompiledScript
  end
end

describe "JRuby::CompiledScript#inspect_bytecode" do
  it "should produce a String representation of the compiled bytecode" do
    compiled = JRuby.compile("foo = 1")
    bytecode = compiled.inspect_bytecode

    bytecode.should be_kind_of String
  end
end
