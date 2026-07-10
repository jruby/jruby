require 'rspec'
require_relative '../fixtures/for_in_evaluated_lambda_used_for_define_method'

# The case here only triggers if a for loop inside an eval is wrapped in a lambda and
# converted into a method with define_method.
#
# This is a complex combination of states, so we have a regression spec rather than try to
# force it into ruby/spec somewhere weird.
describe 'defined?(Array) inside a for loop inside an evaluated lambda used for define_method' do
  it "returns 'constant'" do
    expect(ForInLambdaInEvalInDefineMethod.new.bar) == 'constant'
  end
end
