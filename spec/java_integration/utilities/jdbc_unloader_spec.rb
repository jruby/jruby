require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby'

require File.expand_path('../../fixtures/tinySQL-2.26.jar', __FILE__)

describe "JDBCDriverUnloader" do
  before :each do
    # loading the driver causes it to be registered
    Java::com.sqlmagic.tinysql.textFileDriver
  end

  it "unregisters the drivers" do
    iterator = JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.iterator
    drivers = []
    while iterator.has_next
      drivers << iterator.next
    end
    drivers.size.should == 1
    drivers[0].should be_kind_of(Java::com.sqlmagic.tinysql.textFileDriver)

    JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.run
    iterator = JRuby.runtime.getJRubyClassLoader.getJDBCDriverUnloader.iterator
    drivers = []
    while iterator.has_next
      drivers << iterator.next
    end
    drivers.should be_empty
  end
end
