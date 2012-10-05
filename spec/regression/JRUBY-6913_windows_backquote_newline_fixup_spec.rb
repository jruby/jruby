require 'rspec'

describe 'Kernel#` on Windows' do
  it 'replaces \r\n line endings with \n' do
    # testing same on all platforms, for simplicity
    result = `#{ENV_JAVA['jruby.home']}/bin/jruby --properties`

    result.should_not match(/\r\n/)
  end
end
