# https://github.com/jruby/jruby/issues/1980

describe 'Multiple non-local returns in-flight' do
  it 'should not collide' do
    o = Object.new
    def o.bar; yield; end
    def o.baz; bar { return 5 }; end
    def o.foo
      bar { return 10 }
    ensure
      baz
    end

    expect(o.foo).to eq(10)
  end
end
