describe 'JRUBY-5773: assignment in a method with default argument strip off other arguments.' do
  it "should assign proper post args at proper offsets" do
    cls = Class.new do
      # eval to avoid parser trouble
      class_eval "
        def foo2(a = 0, b); [a, b]; end
        def foo3(a = 0, b); c = 0; [a, b]; end
        def foo4(a = 0, b); c = 0; d = 0; [a, b]; end
        "
    end
    obj = cls.new

    expect(obj.foo2(1, 2)).to eq([1, 2])
    expect(obj.foo3(1, 2)).to eq([1, 2])
    expect(obj.foo4(1, 2)).to eq([1, 2])
  end
end