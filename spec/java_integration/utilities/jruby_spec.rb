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

describe "JRuby.ast_for" do
  it "should produce an AST node" do
    node = JRuby.ast_for("n = 1")
    node[0][0].name.should == "n"
    node[0][0][0].value.should == 1
  end

  it "should produce a node that can be run" do
    JRuby.ast_for("1 + 2 + 3").run.should == 6
  end

  it "can combine nodes together" do
    n1 = JRuby.ast_for("n = 0").first
    n2 = JRuby.ast_for("n += 1").first
    n3 = JRuby.ast_for("n += 2").first
    (n1 + n2 + n3).run.should == 3
  end
end
