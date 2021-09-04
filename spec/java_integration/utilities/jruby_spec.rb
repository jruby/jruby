require File.dirname(__FILE__) + "/../spec_helper"

describe "JRuby#compile" do
  it "should produce a CompiledScript instance" do
    require 'jruby'
    compiled = JRuby.compile("foo = 1")
    expect(compiled).to be_kind_of JRuby::CompiledScript
  end
end

describe "JRuby::CompiledScript#inspect_bytecode" do
  it "should produce a String representation of the compiled bytecode" do
    require 'jruby'
    compiled = JRuby.compile("foo = 1")
    bytecode = compiled.inspect_bytecode

    expect(bytecode).to be_kind_of String
  end
end

describe "JRuby#compile_ir" do
  it "should return an IR script body" do
    require 'jruby'
    compiled = JRuby.compile_ir("foo = 1; bar = 2", 'foobar.rbx')

    expect(compiled.file_name).to eql 'foobar.rbx'
  end
end

describe "JRuby#parse" do
  it "as bytes" do
    require 'jruby'
    node = JRuby.parse('bar = :bar; bar.to_s * 111'.force_encoding('ASCII-8BIT'), '')

    expect(node).to be_kind_of org.jruby.ast.RootNode
  end

  it "as string" do
    require 'jruby'
    node = JRuby.parse('baz = :baz; baz.to_s + " "'.force_encoding('UTF-8'), '')

    expect(node).to be_kind_of org.jruby.ast.RootNode
  end
end