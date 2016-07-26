# Regression spec courtesy Jason Lunn to for GH issue 4104
module DefinesMethod
  def def_meth &block
    define_method :foo, &block
  end
end

class RegressionTest
  extend DefinesMethod
  def_meth do
    def bar
      'Success'
    end
  end
end

describe "NameError created internally using a format string" do
  it "does not warn in verbose mode" do
    o = RegressionTest.new
    o.foo
    expect(o.bar).to eq "Success"
  end
end
