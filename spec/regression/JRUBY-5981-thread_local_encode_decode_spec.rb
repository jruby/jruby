require 'rspec'

describe 'JRUBY-5981: thread-local cached UTF-8 coder' do
  it "should not truncate strings smaller than 1k" do
    require 'jruby'
    str = "." * 200
    rstr = JRuby.reference(str)
    expect(rstr.getUnicodeValue.length).to eq(200)
  end

  it "should not truncate strings smaller than 4k" do
    require 'jruby'
    str = "." * 2000
    rstr = JRuby.reference(str)
    expect(rstr.getUnicodeValue.length).to eq(2000)
  end

  it "should not truncate strings larger than 4k" do
    require 'jruby'
    str = "." * 20000
    rstr = JRuby.reference(str)
    expect(rstr.getUnicodeValue.length).to eq(20000)
  end
end
