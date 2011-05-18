require 'tempfile'
require 'thread'

describe 'JRUBY-3194: threaded autoload' do
  before :each do
    @loaded_features = $".dup
  end

  after :each do
    $".replace @loaded_features
  end

  if RUBY_VERSION >= "1.9.2"
    # TODO: RubyModule#resolveUndefConstant() removes existing Constant in 1.8 mode for CRuby compatibility.
    it 'should not raise for threaded autoload' do
      file = Tempfile.open(['autoload', '.rb'])
      file.puts 'sleep 0.5'
      file.puts 'class Object; SpecRegressionJruby3194 = 1; end'
      file.close
      eval <<-END
        class Object
          autoload :SpecRegressionJruby3194, #{file.path.dump}
        end
      END
      t1 = Thread.new {
        Object::SpecRegressionJruby3194
      }
      t2 = Thread.new {
        Object::SpecRegressionJruby3194
      }
      t1.join.value.should == 1
      t2.join.value.should == 1
      class Object
        remove_const(:SpecRegressionJruby3194)
      end
    end
  end

  it 'should not raise recursive autoload' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'class Object; SpecRegressionJruby3194 = 1; end'
    file.close
    eval <<-END
      class Object
        autoload :SpecRegressionJruby3194, #{file.path.dump}
      end
    END
    lambda {
      (require file.path).should == true
      SpecRegressionJruby3194.should == 1
    }.should_not raise_error
    class Object
      remove_const(:SpecRegressionJruby3194)
    end
  end
end
