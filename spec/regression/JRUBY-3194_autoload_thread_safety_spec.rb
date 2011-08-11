require 'tempfile'
require 'thread'

describe 'JRUBY-3194: threaded autoload' do
  before :each do
    @loaded_features = $".dup
  end

  after :each do
    $".replace @loaded_features
  end

  def add_autoload(path)
    eval <<-END
      class Object
        autoload :SpecRegressionJruby3194, #{path.dump}
      end
    END
  end

  def remove_autoload_constant
    eval <<-END
      class Object
        remove_const(:SpecRegressionJruby3194)
      end
    END
  end

  it 'should not raise recursive autoload' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'class Object; SpecRegressionJruby3194 = 1; end'
    file.close
    add_autoload(file.path)
    begin
      lambda {
        (require file.path).should == true
        SpecRegressionJruby3194.should == 1
      }.should_not raise_error
    ensure
      remove_autoload_constant
    end
  end

  it 'should not raise for accessing a constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'sleep 0.5; class SpecRegressionJruby3194; X = 1; end'
    file.close
    add_autoload(file.path)
    begin
      lambda {
        t1 = Thread.new { SpecRegressionJruby3194::X }
        t2 = Thread.new { SpecRegressionJruby3194::X }
        [t1, t2].each(&:join)
      }.should_not raise_error
    ensure
      remove_autoload_constant
    end
  end

  it 'should not raise for accessing an inner constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'class SpecRegressionJruby3194; sleep 0.5; X = 1; end'
    file.close
    add_autoload(file.path)
    begin
      lambda {
        t1 = Thread.new { SpecRegressionJruby3194::X }
        t2 = Thread.new { SpecRegressionJruby3194::X }
        [t1, t2].each(&:join)
      }.should_not raise_error
    ensure
      remove_autoload_constant
    end
  end

  it 'should raise NameError when autoload did not define the constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts ''
    file.close
    add_autoload(file.path)
    begin
      lambda {
        SpecRegressionJruby3194
      }.should raise_error NameError
    ensure
      remove_autoload_constant
    end
  end

  it 'should allow to override autoload with constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts ''
    file.close
    add_autoload(file.path)
    begin
      class SpecRegressionJruby3194
      end
      SpecRegressionJruby3194.class.should == Class
    ensure
      remove_autoload_constant
    end
  end
end
