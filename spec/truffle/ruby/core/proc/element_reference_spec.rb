require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/call', __FILE__)

language_version __FILE__, "call"

describe "Proc#[]" do
  it_behaves_like :proc_call, :[]

  ruby_version_is "1.8.7" do
    it_behaves_like :proc_call_block_args, :[]
  end
end

describe "Proc#call on a Proc created with Proc.new" do
  it_behaves_like :proc_call_on_proc_new, :call
end

describe "Proc#call on a Proc created with Kernel#lambda or Kernel#proc" do
  it_behaves_like :proc_call_on_proc_or_lambda, :call
end
