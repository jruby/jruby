require 'rspec'

if RUBY_VERSION =~ /1\.9/
  describe 'A class on which singleton_class has been called' do
    it 'still shows instance variables in #inspect' do
      cls = Class.new do
        def initialize(sing)
          @a = 1
          @b = 2
          singleton_class if sing
        end
      end

      cls.new(false).inspect.should =~ /@a=1/
      cls.new(false).inspect.should =~ /@b=2/
      cls.new(true).inspect.should =~ /@a=1/
      cls.new(true).inspect.should =~ /@b=2/
    end
  end
end
