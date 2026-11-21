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
      provided += %w[monitor io/wait]
    end

    out = ruby_exe("puts $LOADED_FEATURES", options: '--disable-gems --disable-did-you-mean')
    features = out.lines.map(&:chomp)

    # Ignore engine-specific internals
    case RUBY_ENGINE
    when "jruby"
      features -= %w[java.rb jruby/util.rb]
    when "ruby"
      # remove all external libraries first
      features.reject! { |feature| File.absolute_path?(feature) }

      # for statically-linked ruby
      features -= [ "encdb.so", "trans/transdb.so" ] # the suffixes are always ".so"
      features.reject! { |feature| feature.start_with?("enc/") } # and "enc/trans/"
    end

    features_no_ext = features.map { |path| path.sub(/\.(?:rb|so)\z/, '') }
    features_no_ext.sort.uniq.should == provided.sort

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

describe "Kernel#require of a file another Fiber is still requiring" do
  before :each do
    @path = fixture(__FILE__, "require_fiber_yield.rb")
    @fibers = []
    ScratchPad.record []
  end

  after :each do
    @fibers.each { |fiber| fiber.resume while fiber.alive? }
    Fiber.set_scheduler(nil)
    $LOADED_FEATURES.delete(@path)
  end

  it "raises a ThreadError without a Fiber scheduler" do
    @fibers << Fiber.new { require @path }
    @fibers.last.resume

    -> { require @path }.should.raise(ThreadError, "deadlock; lock already owned by another fiber belonging to the same thread")
  end

  describe "with a Fiber scheduler" do
    require_relative '../fiber/fixtures/scheduler'

    before :each do
      Fiber.set_scheduler(FiberSpecs::LoggingScheduler.new)
    end

    it "blocks through the scheduler until the other Fiber finishes" do
      @fibers << loader = Fiber.schedule { require @path }
      ScratchPad.recorded.should == [:loading]

      result = nil
      @fibers << waiter = Fiber.schedule { result = require @path }
      event = Fiber.scheduler.events.last
      event[:event].should == :block
      event[:fiber].should.equal?(waiter)

      loader.resume
      ScratchPad.recorded.should == [:loading, :loaded]
      event = Fiber.scheduler.events.last
      event[:event].should == :unblock
      event[:fiber].should.equal?(loader)
      event[:args].last.should.equal?(waiter)

      waiter.resume
      result.should == false
    end
  end
end
