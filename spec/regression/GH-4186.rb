# This is not really ruby/spec worthy as it is a weird internal mistake on
# our part in checking all variables.

describe "#4186 JRuby accepts wrong method arguments when mixing positional with defaults and keywords" do
  it "complains about missing required kwarg" do
    def gh4186(a=1, b:); end
    expect { gh4186(a: 2, b: 3) }.to raise_error(ArgumentError)
  end
end
