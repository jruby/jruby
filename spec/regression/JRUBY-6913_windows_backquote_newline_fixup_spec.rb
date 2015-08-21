require 'rspec'

describe 'Kernel#` on Windows' do
  it 'replaces \r\n line endings with \n' do
    # testing same on all platforms, for simplicity
    result = `#{ENV_JAVA['jruby.home']}/bin/jruby --properties`

    expect(result).not_to match(/\r\n/)
  end
end
