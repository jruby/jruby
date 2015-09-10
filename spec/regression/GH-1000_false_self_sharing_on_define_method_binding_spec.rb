require 'rspec'

describe "A define_method method called across threads and selfs" do
  it "never loses proper self identity" do
    self_class = Class.new do
      attr_reader :self
      define_method(:initialize) { @self = self }
    end


    expect do
      (0..9).map do
        Thread.new do
          10000.times do
            sc = self_class.new
            fail unless sc == sc.self
          end
        end
      end.each(&:value)
    end.not_to raise_error
  end
end
