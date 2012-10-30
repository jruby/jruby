describe "The -n command line option" do
  before :each do
    @names  = fixture __FILE__, "names.txt"
  end

  it "runs the code in loop conditional on Kernel.gets()" do
    ruby_exe("puts $_", :options => "-n", :escape => true,
                        :args => " < #{@names}").should ==
      "alice\nbob\njames\n"
  end
end
