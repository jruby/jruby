require 'rspec'

describe 'nested rescue of java exception' do
  it 'should not have a nil exception' do
    caught = false
    begin
      begin
        raise 'success'
      rescue javax.naming.NameNotFoundException
      end
    rescue Exception => ex
      ex.should_not be_nil
      ex.message.should == 'success'
      caught = true
    end
    caught.should be_true
  end
end
