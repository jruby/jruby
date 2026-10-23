require_relative '../../spec_helper'
require_relative '../../fixtures/code_loading'
require_relative 'shared/require'

describe "Kernel#require" do
  before :each do
    CodeLoadingSpecs.spec_setup
  end

  after :each do
    CodeLoadingSpecs.spec_cleanup
  end

  # if this fails, update your rubygems
  it "is a private method" do
    Kernel.private_instance_methods(false).should.include?(:require)
  end

  it "provided features are already required" do
    provided = %w[complex enumerator fiber rational thread ruby2_keywords]
    ruby_version_is "4.0" do
      provided += %w[set pathname]
    end
    ruby_version_is "4.1" do
      provided += %w[monitor]
    end

    out = ruby_exe("puts $LOADED_FEATURES", options: '--disable-gems --disable-did-you-mean')
    features = out.lines.map(&:chomp)

    # Ignore engine-specific internals
    case RUBY_ENGINE
    when "jruby"
      features -= %w[java.rb jruby/util.rb]
    when "ruby"
      so = RbConfig::CONFIG['DLEXT']
      features.reject! { |feature| feature.end_with?("windows_1252.#{so}", "windows_31j.#{so}") }
      features.reject! { |feature| feature.end_with? "encdb.#{so}" }
      features.reject! { |feature| feature.end_with? "transdb.#{so}" }
      features.reject! { |feature| feature.include?('-fake') }
    end

    features_no_ext = features.map { |path| File.basename(path, '.*') }
    features_no_ext.sort.should == provided.sort

    requires = features
    code = requires.map { |f| "puts require #{f.inspect}\n" }.join
    required = ruby_exe(code, options: '--disable-gems')
    required.should == "false\n" * requires.size
  end

  it_behaves_like :kernel_require_basic, :require, CodeLoadingSpecs::Method.new
  it_behaves_like :kernel_require, :require, CodeLoadingSpecs::Method.new
end

describe "Kernel.require" do
  it "is a public method" do
    Kernel.public_methods(false).should.include?(:require)
  end
end

describe "Kernel#require with a Fiber scheduler" do
  require_relative '../fiber/fixtures/scheduler'

  before :each do
    @path = fixture(__FILE__, "require_sleep.rb")
    ScratchPad.record []
    Fiber.set_scheduler(FiberSpecs::LoggingScheduler.new)
  end

  after :each do
    Fiber.set_scheduler(nil)
    $LOADED_FEATURES.delete(@path)
  end

  it "blocks through the scheduler while another fiber is requiring the same file" do
    loader = Fiber.schedule { require @path }
    ScratchPad.recorded.should == [:loading]

    result = nil
    waiter = Fiber.schedule { result = require @path }
    event = Fiber.scheduler.events.last
    event[:event].should == :block
    event[:fiber].should.equal?(waiter)

    loader.resume
    ScratchPad.recorded.should == [:loading, :loaded]
    event = Fiber.scheduler.events.last
    event[:event].should == :unblock
    event[:fiber].should.equal?(loader)
    event[:args].last.should.equal?(waiter)

    [loader, waiter].each { |f| f.resume while f.alive? }
    result.should == false
  end
end
