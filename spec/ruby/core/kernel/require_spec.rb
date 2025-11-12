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
    Kernel.should have_private_instance_method(:require)
  end

  provided = %w[complex enumerator fiber rational thread ruby2_keywords]
  ruby_version_is "3.5" do
    provided << "set"
    provided << "pathname"
  end

  provided_requires = provided.dup.to_h {|f| [f,f]}
  ruby_version_is "3.5" do
    provided_requires["pathname"] = "pathname.so"
  end

  provided.each do |feature|
    it "#{feature} is already required and provided in loaded features at boot" do
      feature_require = provided_requires[feature]

      code = <<~RUBY
        loaded_feature_base = $\".map{|f| File.basename(f, '.*')}
        required = begin
          require(#{feature_require.inspect})
        rescue LoadError
          "error"
        end
        feature = loaded_feature_base.include?(#{feature.inspect})
        p({required:, feature:})
        RUBY
      output = ruby_exe(code, options: '--disable-gems').chomp
      output.should == "{required: false, feature: true}"
    end
  end

  it_behaves_like :kernel_require_basic, :require, CodeLoadingSpecs::Method.new
  it_behaves_like :kernel_require, :require, CodeLoadingSpecs::Method.new
end

describe "Kernel.require" do
  before :each do
    CodeLoadingSpecs.spec_setup
  end

  after :each do
    CodeLoadingSpecs.spec_cleanup
  end

  it_behaves_like :kernel_require_basic, :require, Kernel
  it_behaves_like :kernel_require, :require, Kernel
end
