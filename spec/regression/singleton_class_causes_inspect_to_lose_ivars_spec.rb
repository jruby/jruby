require 'rspec'

describe 'A class on which singleton_class has been called' do
  it 'still shows instance variables in #inspect' do
    cls = Class.new do
      def initialize(sing)
        @a = 1
        @b = 2
        singleton_class if sing
      end
    end

    expect(cls.new(false).inspect).to match(/@a=1/)
    expect(cls.new(false).inspect).to match(/@b=2/)
    expect(cls.new(true).inspect).to match(/@a=1/)
    expect(cls.new(true).inspect).to match(/@b=2/)
  end
end
