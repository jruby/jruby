describe 'Kernel#Float' do
  it 'raises an error when exponent has extra garbage after it' do
    expect { Float("661e7086-33af-11eb") }.to raise_error(ArgumentError)
  end
end

