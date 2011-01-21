require File.dirname(__FILE__) + "/../spec_helper"

require 'java'

describe "The $CLASSPATH variable" do
  let(:container) do
    org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD).tap do |c|
      c.runScriptlet("require 'java'")
    end
  end

  it "appends URLs unmodified" do
    container.runScriptlet('$CLASSPATH << "http://jruby.org/"; $CLASSPATH.include?("http://jruby.org/")').should be_true
  end

  it "assumes entries without URL protocols are files" do
    container.runScriptlet("$CLASSPATH << '#{__FILE__}'; $CLASSPATH.include?('file:#{__FILE__}')").should be_true
  end

  it "appends slashes to directory names" do
    d = File.expand_path(File.dirname(__FILE__))
    container.runScriptlet("$CLASSPATH << '#{d}'; $CLASSPATH.include?('file:#{d}/')").should be_true
  end
end
