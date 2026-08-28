describe "Object#instance_eval" do
  it "uses the target object as self in the executed code" do
    o = Object.new
    o.instance_variable_set :@foo, 1
    newself, binding = o.instance_eval "[self, binding]"
    expect(newself).to eq(o)
    expect(eval('self', binding)).to eq(self)

    newself, binding = o.instance_eval {[self, binding]}
    expect(newself).to eq(o)
    expect(eval('self', binding)).to eq(self)
  end
end
