require File.dirname(__FILE__) + "/../spec_helper"

require 'java'

describe "The $CLASSPATH variable" do
  let(:container) do
    org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD).tap do |c|
      c.runScriptlet("require 'java'")
    end
  end

  it "appends URLs unmodified" do
    expect(
      container.runScriptlet('$CLASSPATH << "http://jruby.org/"; $CLASSPATH.include?("http://jruby.org/")')
    ).to be true
  end

  it "assumes entries without URL protocols are files" do
    expect(
      container.runScriptlet("$CLASSPATH << '#{__FILE__}'; $CLASSPATH.include?('file:#{__FILE__}')")
    ).to be true
  end

  it "appends slashes to directory names" do
    d = File.expand_path(File.dirname(__FILE__))
    expect(
      container.runScriptlet("$CLASSPATH << '#{d}'; $CLASSPATH.include?('file:#{d}/')")
    ).to be true
  end
end
