# Regression spec courtesy Jason Lunn for GH issue 4104
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

describe "Method defined inside an instance method" do
  it "should succeed even when that instance method is generated via define_method" do
    o = RegressionTest.new
    o.foo
    expect(o.bar).to eq "Success"
  end
end
