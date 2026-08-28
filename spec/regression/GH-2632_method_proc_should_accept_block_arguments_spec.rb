describe "A proc created from a Method object" do
  it "receives block arguments based on its arity" do
    $GH2632 = nil
    o = Object.new
    def o.foo(a); $GH2632 = a; end
    m = o.method :foo
    (1..1).each &m
    expect($GH2632).to eq(1)
  end
end