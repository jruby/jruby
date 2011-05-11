describe 'JRUBY-5787: ConcurrencyError from Array#choice when the Array was truncated before' do
  it 'should not raise' do
    a = [1, 2, 3, 4, 5]
    a.shift(4)
    if RUBY_VERSION >= "1.9.2"
      a.respond_to?(:choice).should == false
    else
      a.choice.should == 5
    end
  end
end
