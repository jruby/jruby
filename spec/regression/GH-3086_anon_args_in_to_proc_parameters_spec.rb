describe 'A method with anonymous required arguments' do
  it 'can to_proc and produce parameters without error' do
    # anonymous required args built wrong, caused NPE (#3086)
    expect(method(:`).to_proc.parameters).to eq([[:req]])
  end
end
