require 'spec_helper'
require 'mspec/guards'
require 'rbconfig'

describe SpecGuard, "#ruby_version_override=" do
  after :each do
    SpecGuard.ruby_version_override = nil
  end

  it "returns nil by default" do
    SpecGuard.ruby_version_override.should be_nil
  end

  it "returns the value set by #ruby_version_override=" do
    SpecGuard.ruby_version_override = "8.3.2"
    SpecGuard.ruby_version_override.should == "8.3.2"
  end
end

describe SpecGuard, ".ruby_version" do
  before :each do
    @ruby_version = Object.const_get :RUBY_VERSION
    @ruby_patchlevel = Object.const_get :RUBY_PATCHLEVEL

    Object.const_set :RUBY_VERSION, "8.2.3"
    Object.const_set :RUBY_PATCHLEVEL, 71
  end

  after :each do
    Object.const_set :RUBY_VERSION, @ruby_version
    Object.const_set :RUBY_PATCHLEVEL, @ruby_patchlevel
  end

  it "returns the version and patchlevel for :full" do
    SpecGuard.ruby_version(:full).should == "8.2.3.71"
  end

  it "returns 0 for negative RUBY_PATCHLEVEL values" do
    Object.const_set :RUBY_PATCHLEVEL, -1
    SpecGuard.ruby_version(:full).should == "8.2.3.0"
  end

  it "returns major.minor.tiny for :tiny" do
    SpecGuard.ruby_version(:tiny).should == "8.2.3"
  end

  it "returns major.minor.tiny for :teeny" do
    SpecGuard.ruby_version(:tiny).should == "8.2.3"
  end

  it "returns major.minor for :minor" do
    SpecGuard.ruby_version(:minor).should == "8.2"
  end

  it "defaults to :minor" do
    SpecGuard.ruby_version.should == "8.2"
  end

  it "returns major for :major" do
    SpecGuard.ruby_version(:major).should == "8"
  end

  describe "with ruby_version_override set" do
    before :each do
      SpecGuard.ruby_version_override = "8.3.2"
    end

    after :each do
      SpecGuard.ruby_version_override = nil
    end

    it "returns the version and patchlevel for :full" do
      SpecGuard.ruby_version(:full).should == "8.3.2.71"
    end

    it "returns 0 for negative RUBY_PATCHLEVEL values" do
      Object.const_set :RUBY_PATCHLEVEL, -1
      SpecGuard.ruby_version(:full).should == "8.3.2.0"
    end

    it "returns major.minor.tiny for :tiny" do
      SpecGuard.ruby_version(:tiny).should == "8.3.2"
    end

    it "returns major.minor.tiny for :teeny" do
      SpecGuard.ruby_version(:tiny).should == "8.3.2"
    end

    it "returns major.minor for :minor" do
      SpecGuard.ruby_version(:minor).should == "8.3"
    end

    it "defaults to :minor" do
      SpecGuard.ruby_version.should == "8.3"
    end

    it "returns major for :major" do
      SpecGuard.ruby_version(:major).should == "8"
    end
  end
end

describe SpecGuard, "#yield?" do
  before :each do
    MSpec.clear_modes
    @guard = SpecGuard.new
  end

  after :each do
    MSpec.unregister :add, @guard
    MSpec.clear_modes
    SpecGuard.clear_guards
  end

  it "returns true if MSpec.mode?(:unguarded) is true" do
    MSpec.register_mode :unguarded
    @guard.yield?.should == true
  end

  it "returns true if MSpec.mode?(:verify) is true" do
    MSpec.register_mode :verify
    @guard.yield?.should == true
  end

  it "returns true if MSpec.mode?(:verify) is true regardless of invert being true" do
    MSpec.register_mode :verify
    @guard.yield?(true).should == true
  end

  it "returns true if MSpec.mode?(:report) is true" do
    MSpec.register_mode :report
    @guard.yield?.should == true
  end

  it "returns true if MSpec.mode?(:report) is true regardless of invert being true" do
    MSpec.register_mode :report
    @guard.yield?(true).should == true
  end

  it "returns true if MSpec.mode?(:report_on) is true and SpecGuards.guards contains the named guard" do
    MSpec.register_mode :report_on
    SpecGuard.guards << :guard_name
    @guard.yield?.should == false
    @guard.name = :guard_name
    @guard.yield?.should == true
  end

  it "returns #match? if neither report nor verify mode are true" do
    @guard.stub(:match?).and_return(false)
    @guard.yield?.should == false
    @guard.stub(:match?).and_return(true)
    @guard.yield?.should == true
  end

  it "returns #match? if invert is true and neither report nor verify mode are true" do
    @guard.stub(:match?).and_return(false)
    @guard.yield?(true).should == true
    @guard.stub(:match?).and_return(true)
    @guard.yield?(true).should == false
  end
end

describe SpecGuard, "#===" do
  it "returns true" do
    anything = double("anything")
    SpecGuard.new.===(anything).should == true
  end
end

describe SpecGuard, "#implementation?" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_name = Object.const_get :RUBY_NAME
    @guard = SpecGuard.new
  end

  after :each do
    Object.const_set :RUBY_NAME, @ruby_name
  end

  it "returns true if passed :ruby and RUBY_NAME == 'ruby'" do
    Object.const_set :RUBY_NAME, 'ruby'
    @guard.implementation?(:ruby).should == true
  end

  it "returns true if passed :rubinius and RUBY_NAME == 'rbx'" do
    Object.const_set :RUBY_NAME, 'rbx'
    @guard.implementation?(:rubinius).should == true
  end

  it "returns true if passed :jruby and RUBY_NAME == 'jruby'" do
    Object.const_set :RUBY_NAME, 'jruby'
    @guard.implementation?(:jruby).should == true
  end

  it "returns true if passed :ironruby and RUBY_NAME == 'ironruby'" do
    Object.const_set :RUBY_NAME, 'ironruby'
    @guard.implementation?(:ironruby).should == true
  end

  it "returns true if passed :maglev and RUBY_NAME == 'maglev'" do
    Object.const_set :RUBY_NAME, 'maglev'
    @guard.implementation?(:maglev).should == true
  end

  it "returns true if passed :topaz and RUBY_NAME == 'topaz'" do
    Object.const_set :RUBY_NAME, 'topaz'
    @guard.implementation?(:topaz).should == true
  end

  it "returns true if passed :ruby and RUBY_NAME matches /^ruby/" do
    Object.const_set :RUBY_NAME, 'ruby'
    @guard.implementation?(:ruby).should == true

    Object.const_set :RUBY_NAME, 'ruby1.8'
    @guard.implementation?(:ruby).should == true

    Object.const_set :RUBY_NAME, 'ruby1.9'
    @guard.implementation?(:ruby).should == true
  end

  it "returns false when passed an unrecognized name" do
    Object.const_set :RUBY_NAME, 'ruby'
    @guard.implementation?(:python).should == false
  end
end

describe SpecGuard, "#standard?" do
  before :each do
    @guard = SpecGuard.new
  end

  it "returns true if #implementation? returns true" do
    @guard.should_receive(:implementation?).with(:ruby).and_return(true)
    @guard.standard?.should be_true
  end

  it "returns false if #implementation? returns false" do
    @guard.should_receive(:implementation?).with(:ruby).and_return(false)
    @guard.standard?.should be_false
  end
end

describe SpecGuard, "#platform?" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_platform = Object.const_get :RUBY_PLATFORM
    Object.const_set :RUBY_PLATFORM, 'solarce'
    @guard = SpecGuard.new
  end

  after :each do
    Object.const_set :RUBY_PLATFORM, @ruby_platform
  end

  it "returns false when arg does not match RUBY_PLATFORM" do
    @guard.platform?(:ruby).should == false
  end

  it "returns false when no arg matches RUBY_PLATFORM" do
    @guard.platform?(:ruby, :jruby, :rubinius, :maglev).should == false
  end

  it "returns true when arg matches RUBY_PLATFORM" do
    @guard.platform?(:solarce).should == true
  end

  it "returns true when any arg matches RUBY_PLATFORM" do
    @guard.platform?(:ruby, :jruby, :solarce, :rubinius, :maglev).should == true
  end

  it "returns true when arg is :windows and RUBY_PLATFORM contains 'mswin'" do
    Object.const_set :RUBY_PLATFORM, 'i386-mswin32'
    @guard.platform?(:windows).should == true
  end

  it "returns true when arg is :windows and RUBY_PLATFORM contains 'mingw'" do
    Object.const_set :RUBY_PLATFORM, 'i386-mingw32'
    @guard.platform?(:windows).should == true
  end

  it "returns false when arg is not :windows and RbConfig::CONFIG['host_os'] contains 'mswin'" do
    Object.const_set :RUBY_PLATFORM, 'i386-mswin32'
    @guard.platform?(:linux).should == false
  end

  it "returns false when arg is not :windows and RbConfig::CONFIG['host_os'] contains 'mingw'" do
    Object.const_set :RUBY_PLATFORM, 'i386-mingw32'
    @guard.platform?(:linux).should == false
  end
end

describe SpecGuard, "#platform? on JRuby" do
  before :all do
    @verbose = $VERBOSE
    $VERBOSE = nil
  end

  after :all do
    $VERBOSE = @verbose
  end

  before :each do
    @ruby_platform = Object.const_get :RUBY_PLATFORM
    Object.const_set :RUBY_PLATFORM, 'java'
    @guard = SpecGuard.new
  end

  after :each do
    Object.const_set :RUBY_PLATFORM, @ruby_platform
  end

  it "returns true when arg is :java and RUBY_PLATFORM contains 'java'" do
    @guard.platform?(:java).should == true
  end

  it "returns true when arg is :windows and RUBY_PLATFORM contains 'java' and os?(:windows) is true" do
    RbConfig::CONFIG.stub(:[]).and_return('mswin32')
    @guard.platform?(:windows).should == true
  end

  it "returns true when RUBY_PLATFORM contains 'java' and os?(argument) is true" do
    RbConfig::CONFIG.stub(:[]).and_return('amiga')
    @guard.platform?(:amiga).should == true
  end
end

describe SpecGuard, "#wordsize?" do
  before :each do
    @guard = SpecGuard.new
  end

  it "returns true when arg is 32 and 1.size is 4" do
    @guard.wordsize?(32).should == (1.size == 4)
  end

  it "returns true when arg is 64 and 1.size is 8" do
    @guard.wordsize?(64).should == (1.size == 8)
  end
end

describe SpecGuard, "#os?" do
  before :each do
    @guard = SpecGuard.new
    RbConfig::CONFIG.stub(:[]).and_return('unreal')
  end

  it "returns true if argument matches RbConfig::CONFIG['host_os']" do
    @guard.os?(:unreal).should == true
  end

  it "returns true if any argument matches RbConfig::CONFIG['host_os']" do
    @guard.os?(:bsd, :unreal, :amiga).should == true
  end

  it "returns false if no argument matches RbConfig::CONFIG['host_os']" do
    @guard.os?(:bsd, :netbsd, :amiga, :msdos).should == false
  end

  it "returns false if argument does not match RbConfig::CONFIG['host_os']" do
    @guard.os?(:amiga).should == false
  end

  it "returns true when arg is :windows and RbConfig::CONFIG['host_os'] contains 'mswin'" do
    RbConfig::CONFIG.stub(:[]).and_return('i386-mswin32')
    @guard.os?(:windows).should == true
  end

  it "returns true when arg is :windows and RbConfig::CONFIG['host_os'] contains 'mingw'" do
    RbConfig::CONFIG.stub(:[]).and_return('i386-mingw32')
    @guard.os?(:windows).should == true
  end

  it "returns false when arg is not :windows and RbConfig::CONFIG['host_os'] contains 'mswin'" do
    RbConfig::CONFIG.stub(:[]).and_return('i386-mingw32')
    @guard.os?(:linux).should == false
  end

  it "returns false when arg is not :windows and RbConfig::CONFIG['host_os'] contains 'mingw'" do
    RbConfig::CONFIG.stub(:[]).and_return('i386-mingw32')
    @guard.os?(:linux).should == false
  end
end

describe SpecGuard, "#windows?" do
  before :each do
    @guard = SpecGuard.new
  end

  it "returns false if not passed :windows" do
    @guard.windows?(:linux, 'mswin32').should == false
    @guard.windows?(:linux, 'i386-mingw32').should == false
  end

  it "returns true if passed :windows and the key matches 'mswin' or 'mingw'" do
    @guard.windows?(:windows, 'mswin32').should == true
    @guard.windows?(:windows, 'i386-mingw32').should == true
  end

  it "returns false if passed :windows and the key matches neither 'mswin' nor 'mingw'" do
    @guard.windows?(:windows, 'darwin9.0').should == false
    @guard.windows?(:windows, 'linux').should == false
  end
end

describe SpecGuard, "#match?" do
  before :each do
    @guard = SpecGuard.new
    SpecGuard.stub(:new).and_return(@guard)
  end

  it "returns true if #platform? or #implementation? return true" do
    @guard.stub(:implementation?).and_return(true)
    @guard.stub(:platform?).and_return(false)
    @guard.match?.should == true

    @guard.stub(:implementation?).and_return(false)
    @guard.stub(:platform?).and_return(true)
    @guard.match?.should == true
  end

  it "returns false if #platform? and #implementation? return false" do
    @guard.stub(:implementation?).and_return(false)
    @guard.stub(:platform?).and_return(false)
    @guard.match?.should == false
  end
end

describe SpecGuard, "#unregister" do
  before :each do
    MSpec.stub(:unregister)
    @guard = SpecGuard.new
  end

  it "unregisters from MSpec :add actions" do
    MSpec.should_receive(:unregister).with(:add, @guard)
    @guard.unregister
  end
end

describe SpecGuard, "#record" do
  after :each do
    SpecGuard.clear
  end

  it "saves the name of the guarded spec under the name of the guard" do
    guard = SpecGuard.new "a", "1.8"..."1.9"
    guard.name = :named_guard
    guard.record "SomeClass#action returns true"
    SpecGuard.report.should == {
      'named_guard a, 1.8...1.9' => ["SomeClass#action returns true"]
    }
  end
end

describe SpecGuard, ".guards" do
  it "returns an Array" do
    SpecGuard.guards.should be_kind_of(Array)
  end
end

describe SpecGuard, ".clear_guards" do
  it "resets the array to empty" do
    SpecGuard.guards << :guard
    SpecGuard.guards.should == [:guard]
    SpecGuard.clear_guards
    SpecGuard.guards.should == []
  end
end

describe SpecGuard, ".finish" do
  before :each do
    $stdout = @out = IOStub.new
  end

  after :each do
    $stdout = STDOUT
    SpecGuard.clear
  end

  it "prints the descriptions of the guarded specs" do
    guard = SpecGuard.new "a", "1.8"..."1.9"
    guard.name = :named_guard
    guard.record "SomeClass#action returns true"
    guard.record "SomeClass#reverse returns false"
    SpecGuard.finish
    $stdout.should == %[

2 specs omitted by guard: named_guard a, 1.8...1.9:

SomeClass#action returns true
SomeClass#reverse returns false

]
  end
end
