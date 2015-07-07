def foo(string)
  if string =~ /Prefix (\w)/
    $1
  end
end

# Make sure foo JITs
(1..100).each do
  foo('Prefix A')
end

# https://github.com/jruby/jruby/issues/3104
describe 'pop scope/frames in AddCallProtocolInstructionsPass' do
  it 'should not break returns' do
    expect(foo("Prefix A")).to eq 'A'
  end
end
