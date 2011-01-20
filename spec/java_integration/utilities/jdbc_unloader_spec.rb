require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

require File.expand_path('../../fixtures/tinySQL-2.26.jar', __FILE__)

describe "JDBCDriverUnloader" do
  def drivers
    iterator = JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.iterator
    drivers = []
    while iterator.has_next
      drivers << iterator.next
    end
    drivers
  end

  before :each do
    @driver_count = drivers.size
    # loading the driver causes it to be registered
    Java::com.sqlmagic.tinysql.textFileDriver
  end

  it "unregisters the drivers" do
    drivers.size.should == @driver_count + 1
    drivers.detect {|d| d.kind_of?(Java::com.sqlmagic.tinysql.textFileDriver) }.should be_true
    JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.run
    drivers.should be_empty
  end
end
