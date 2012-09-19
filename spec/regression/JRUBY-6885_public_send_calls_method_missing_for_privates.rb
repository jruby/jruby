require 'rspec'

if RUBY_VERSION =~ /1\.9/
  describe 'Kernel#public_send' do
    it 'invokes method missing when the name in question is defined but not public' do
      obj = Class.new do
	def method_missing(name, *)
	  name
	end
	def foo; end
	private :foo
	def bar; end
	protected :bar
      end.new

      obj.public_send(:foo).should == :foo
      obj.public_send(:bar).should == :bar
    end
  end
end
