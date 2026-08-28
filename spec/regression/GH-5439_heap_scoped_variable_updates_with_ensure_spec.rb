describe "A value written to a variable read by a proc" do
  it "is visible to that proc" do
    o = Object.new
    def o.foo
      var = 0
      pr = ->(){@var = var}
      1.times do
        var = 1
        pr.call
      ensure
        var = 2
      end
    end

    o.foo

    expect(o.instance_variable_get(:@var)).to eq(1)
  end
end
