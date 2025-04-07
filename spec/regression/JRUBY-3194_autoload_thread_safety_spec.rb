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
    file.flush
    add_autoload(file.path)
    begin
      expect {
        expect(require file.path).to eq(true)
        expect(SpecRegressionJruby3194).to eq(1)
      }.not_to raise_error
    ensure
      file.close! rescue nil
      remove_autoload_constant
    end
  end

  it 'should not raise for accessing a constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'sleep 0.5; class SpecRegressionJruby3194; X = 1; end'
    file.flush
    add_autoload(file.path)
    begin
      expect {
        t1 = Thread.new { SpecRegressionJruby3194::X }
        t2 = Thread.new { SpecRegressionJruby3194::X }
        [t1, t2].each(&:join)
      }.not_to raise_error
    ensure
      file.close! rescue nil
      remove_autoload_constant
    end
  end

  it 'should not raise for accessing an inner constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts 'class SpecRegressionJruby3194; sleep 0.5; X = 1; end'
    file.flush
    add_autoload(file.path)
    begin
      expect {
        t1 = Thread.new { SpecRegressionJruby3194::X }
        t2 = Thread.new { SpecRegressionJruby3194::X }
        [t1, t2].each(&:join)
      }.not_to raise_error
    ensure
      file.close! rescue nil
      remove_autoload_constant
    end
  end

  it 'should raise NameError when autoload did not define the constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts ''
    file.flush
    add_autoload(file.path)
    begin
      expect {
        SpecRegressionJruby3194
      }.to raise_error NameError
    ensure
      file.close! rescue nil
      remove_autoload_constant
    end
  end

  it 'should allow to override autoload with constant' do
    file = Tempfile.open(['autoload', '.rb'])
    file.puts ''
    file.flush
    add_autoload(file.path)
    begin
      class SpecRegressionJruby3194
      end
      expect(SpecRegressionJruby3194.class).to eq(Class)
    ensure
      file.close! rescue nil
      remove_autoload_constant
    end
  end
end
