# https://github.com/jruby/jruby/issues/2198

class C
  def update(v); self.v = v; end

  protected
  attr_accessor :v
end

describe 'Protected methods' do
  it 'should be accessed correctly' do
    a = []
    begin
      a << C.new.v
    rescue => e
      a << e.class.name
    end

    begin
      a << C.new.update(42)
    rescue => e
      a << e.class.name
    end

    expect(a).to eq(["NoMethodError", 42])
  end
end
