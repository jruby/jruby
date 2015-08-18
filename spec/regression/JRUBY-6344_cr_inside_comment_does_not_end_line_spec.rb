require 'rspec'
describe 'JRUBY-6344: embedded CR' do
  it "should not be treated as EOL" do
  	f = File.expand_path('../grammar.kpeg.rb', __FILE__)
    expect { eval "load '#{f}'" }.not_to raise_error
  end
end
