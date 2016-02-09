require 'tmpdir'

describe "Kernel#system when called during a Dir.chdir" do
  it "does not expand environment variables" do
    def sh(*cmd)
      res = system(*cmd)
      status = $?
    end

    ENV['GH3653'] = 'someval'


    Dir.chdir(Dir.tmpdir) do
      sh ENV_JAVA['jruby.home'] + '/bin/jruby', File.dirname(__FILE__) + '/GH-3653_check_no_expansion.rb', '$GH3653'
    end

    expect($?.exitstatus).to eq(0)
  end
end

