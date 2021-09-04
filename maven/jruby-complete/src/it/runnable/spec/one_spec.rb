describe "something" do
  it "does something" do
    expect(__FILE__).to eq 'uri:classloader:/spec/one_spec.rb'
    expect($CLASSPATH.size).to eq 2
    expect(Jars.home).to eq 'uri:classloader:/jars' if ENV_JAVA['jars.home']
    expect(Dir.pwd).to eq 'uri:classloader://'
    $LOAD_PATH.each do |lp|
      # bundler or someone else messes up the $LOAD_PATH
      unless ["uri", "classloader", "//gems/bundler-1.7.7/lib", "/gems/rspec-core-3.3.1/lib", "/gems/rspec-support-3.3.0/lib" ].member?( lp )
        expect(lp).to match /^uri:classloader:|runnable.jar!\//
      end
    end
  end

  it 'see the Helper module from spec_helper' do
    expect( defined? Helper ).to eq('constant')
  end
end
