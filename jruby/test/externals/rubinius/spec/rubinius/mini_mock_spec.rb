# This is a bit awkward. Currently the way to verify that the
# opposites are true (for example a failure when the specified
# arguments are NOT provided) is to simply alter the particular
# spec to a failure condition.
require File.dirname(__FILE__) + '/../spec_helper'

    $stderr.puts '#remove_method not implemented!'

context 'Setting up mock methods in a #specify/#it block' do
  specify 'any object can set up a mock method using #should_receive' do
    o = Object.new
    o.should_receive :foobar
    o.foobar
  end
end

context 'Lifetime of the mocked methods' do
  @o = [1]

  specify 'methods are released...' do
    @o.should_receive :first
    @o.first.should == nil
  end

  specify '...at the end of the #it block' do
    @o.first.should == 1
  end
end

context 'Controlling the expectations' do
  specify 'the value returned by invocation of the mock method is nil or the given :returning' do
    o, oo = Object.new, Object.new
    o.should_receive :foobar
    oo.should_receive :foobar, :returning => :bazquux

    o.foobar.should == nil
    oo.foobar.should == :bazquux
  end

  specify 'the number of times the method must be called can be specified using :count' do
    o = Object.new
    o.should_receive :foobar, :count => 2
    o.foobar
    o.foobar
  end

  specify 'specifying a count of 0 is the same as using #should_not_receive' do
    o, oo = Object.new, Object.new
    o.should_receive :foobar, :count => 0
    oo.should_not_receive :foobar
  end

  specify 'the method can expect a specific set of arguments' do
    o = Object.new
    o.should_receive :foo, :with => ['hello', 1] 
    o.foo 'hello', 1
  end

  specify 'can require a block to be given' do
    o = Object.new
    o.should_receive :foo, :block => true
    o.foo { puts 'hi' }
  end
end
