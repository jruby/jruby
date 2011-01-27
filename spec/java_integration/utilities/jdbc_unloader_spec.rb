require File.dirname(__FILE__) + "/../spec_helper"

describe "JDBCDriverUnloader" do
  let(:driver_jar) { File.expand_path('../../fixtures/tinySQL-2.26.jar', __FILE__) }
  let(:container) do
    org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD).tap do |c|
      c.runScriptlet("require 'jruby'; require '#{driver_jar}'")
    end
  end

  def drivers
    iterator = container.runScriptlet('JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.iterator')
    drivers = []
    while iterator.has_next
      drivers << iterator.next
    end
    drivers
  end

  before :each do
    @driver_count = drivers.size
    # loading the driver causes it to be registered
    container.runScriptlet('Java::com.sqlmagic.tinysql.textFileDriver')
  end

  it "unregisters the drivers" do
    drivers.size.should == @driver_count + 1
    drivers.detect {|d| d.java_class.name == 'com.sqlmagic.tinysql.textFileDriver' }.should be_true
    container.runScriptlet('JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.run')
    drivers.should be_empty
  end
end
