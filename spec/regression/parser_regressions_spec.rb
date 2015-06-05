require 'rspec'

describe "The Parsing experience" do
  it "parses a cond which used to get confused as a tLABEL" do
    cond = true
    x = '('
    a = cond ? x + ')': ''
    a.should eq('()')
  end

  it "parse the stabby lambda even if args params separated by space" do
    a = -> () { 1 }
    a.call.should eq(1)
  end

  it "parses a jumbled mess of nestng" do
    self.class.send(:define_method, :foo) { |a| a[:key].call[:key] }
    var = 1
    res = foo key: (proc do
                      {
                        key: "#{var}#{var}",
                        other_key: proc do
                        end
                      }
                    end)

    res.should eq("11")
  end

  it "parsers a block do inside of a call arg list" do
    self.class.send(:define_method, :foo) { |a| a }
    res = foo (10.times.to_a.map do |i|
                 7 + i
               end)
    res.should eq([7,8,9,10,11,12,13,14,15,16])
  end
end
