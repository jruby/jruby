require File.dirname(__FILE__) + "/../spec_helper"

describe "JDBCDriverUnloader" do
  let(:driver_jar) { File.expand_path('../../fixtures/tinySQL-2.26.jar', __FILE__) }
  let(:container) do
    org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD).tap do |c|
      c.runScriptlet("require 'jruby'; require '#{driver_jar}'")
    end
  end

  def drivers
    container.runScriptlet('JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.iterator').to_a
  end

  it "unregisters the drivers" do
    # loading the driver causes it to be registered
    container.runScriptlet('Java::com.sqlmagic.tinysql.textFileDriver')
    drivers.map {|d| d.java_class.name }.should include('com.sqlmagic.tinysql.textFileDriver')
    container.runScriptlet('JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.run')
    drivers.should be_empty
  end
end
