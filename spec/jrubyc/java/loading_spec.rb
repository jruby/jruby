require_relative '../spec_helper'

describe "JRuby::Compiler.compile_argv" do

  def compile_files(files)
    JRuby::Compiler.compile_argv(files)
  end

  FILES_DIR = File.expand_path('files', File.dirname(__FILE__))

  before(:all) do
    argv = [ '--dir', FILES_DIR, '--target', FILES_DIR ]
    rb_files = Dir.glob(File.join(FILES_DIR, '*.rb'))
    argv.push *rb_files

    puts argv.inspect if $VERBOSE

    JRuby::Compiler.compile_argv argv
  end

  after(:all) do
    Dir.glob(File.join(FILES_DIR, '*.class')).each { |f| File.delete(f) }
  end

  it "loads double_rescue.class" do
    load File.join(FILES_DIR, 'double_rescue.class')

    expect( defined?(DoubleRescue) ).to be_truthy
    DoubleRescue.new._call

    expect( DoubleRescue.re_raise ).to be_a LoadError
  end

  it "loads sample_block.class" do
    load File.join(FILES_DIR, 'sample_block.class')

    expect( defined?(SampleBlock) ).to be_truthy
    expect( SampleBlock.class_variable_get :@@func ).to eql '11'
  end

  it "deserializes symbol_proc.class" do
    load File.join(FILES_DIR, 'symbol_proc.class')

    expect( $symbol_proc_result ).to be_truthy
    expect( $symbol_proc_result ).to eql [ 1, 2, 3 ]
  end

end
