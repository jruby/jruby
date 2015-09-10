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
      expect(ex).to_not be_nil
      expect(ex.message).to eq('success')
      caught = true
    end
    expect(caught).to be true
  end
end
