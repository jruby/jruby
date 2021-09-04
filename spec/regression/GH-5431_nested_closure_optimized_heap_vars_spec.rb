describe "A doubly-nested closure in an ensure block" do
  it "accesses root-level local variables correctly" do
    o = Object.new
    def o.test
      Time.now
    ensure
      a = 1
      1.times do
        a = 2
        1.times do
          a = 3
        end
      end
      @a = a
    end
    
    o.test
    expect(o.instance_variable_get(:@a)).to eq(3)
  end
end
