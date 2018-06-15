require File.dirname(__FILE__) + "/../spec_helper"

describe "inspect method" do

  it "produces \"hashy\" inspect output for java.lang.Object" do
    o = java.lang.Object.new
    expect(o.inspect).to match(/\#<Java::JavaLang::Object:0x[0-9a-f]+>/)
  end

  it 'returns toString when overriden in Java types' do
    expect(java.lang.Short.new(1).inspect).to eql '1'

    expect(java.util.concurrent.TimeUnit::DAYS.inspect).to eql 'DAYS'

    date = java.sql.Date.new(0)
    expect(date.inspect).to eql '1970-01-01'
    expect(date.toLocalDate.inspect).to eql date.to_s
    time = java.sql.Timestamp.new(0)
    expect(time.toInstant.inspect).to eql '1970-01-01T00:00:00Z'

    expect(java.math.BigInteger.new('1').inspect).to eql '1'
    expect(java.math.BigDecimal.new('3.6').to_s).to eql '3.6'

    expect(java.lang.String.new('str').inspect).to eql 'str'

    expect(java.util.ArrayList.new([1, '2']).inspect).to eql '[1, 2]'
  end

end