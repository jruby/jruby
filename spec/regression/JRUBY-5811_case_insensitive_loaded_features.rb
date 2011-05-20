require 'tempfile'

describe 'JRUBY-5811: jruby 1.9 fails to load irb' do
  if RUBY_VERSION >= "1.9"
    before :each do
      @loaded_features = $".dup
    end

    after :each do
      $".replace(@loaded_features)
    end

    it 'works' do
      @file = Tempfile.open(['dummy', '.rb'])
      @file.close
      if File.exist?(@file.path.upcase)
        org_size = $".size
        require @file.path.downcase
        (require @file.path.upcase).should == false
        (require @file.path.capitalize).should == false
        $".size.should == org_size + 1
      else
        File.exist?(@file.path.upcase).should be_false
      end
    end
  end
end
