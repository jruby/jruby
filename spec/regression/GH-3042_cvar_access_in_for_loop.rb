class Foo
  @@A = 42

  def self.test_it
    a = []
    a << @@A
    for name in [1] do
      a << @@A
      for value in [1] do
        a << @@A
      end
    end

    a
  end
end

# https://github.com/jruby/jruby/issues/3042
describe 'cvar access in for loop' do
  it 'should use correct module depth' do
    expect(Foo.test_it).to eq([42, 42, 42])
  end
end
