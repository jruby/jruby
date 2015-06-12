require 'jbundler'

describe "something" do
  it "does something" do
    __FILE__.should == 'uri:classloader:/spec/one_spec.rb'
    $CLASSPATH.size.should == 6
    Jars.home.should == 'uri:classloader://'
    Dir.pwd.should == 'uri:classloader://'
    $LOAD_PATH.each do |lp|
      # bundler or someone else messes up the $LOAD_PATH
      unless ["uri", "classloader", "//gems/bundler-1.7.7/lib"].member?( lp )
        lp.should =~ /^uri:classloader:|runnable.jar!\//
      end
    end
  end
end
