require 'rspec'

describe "JRUBY-6570: autoload called from instance method" do
  it "defines an autoload on the current object's class" do
    cls = Class.new do
      def go
        autoload :Time, 'time'
        autoload? :Time
      end
    end
    
    obj = cls.new
    expect(obj.go).to eq('time')
    expect(cls.autoload?(:Time)).to eq('time')
  end
end