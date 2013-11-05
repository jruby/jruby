require 'java'
require 'jruby'
require 'fileutils.rb'

java_import org.jruby.Ruby
java_import org.jruby.RubyInstanceConfig
java_import org.jruby.ast.Node
java_import org.jruby.ir.IRBuilder
java_import org.jruby.ir.IRManager
java_import org.jruby.ir.IRScope
java_import org.jruby.ir.persistence.IRPersistenceFacade
java_import org.jruby.ir.persistence.IRReadingContext

# Fibbonacci example from http://www.engineyard.com/blog/2012/oss-grant-roundup-jruby-runtime/ is used here
describe IRPersistenceFacade do
  # Pathes
  CURRENT_DIR = File.expand_path(File.dirname(__FILE__))
  REFERENCE_DIR = "#{CURRENT_DIR}/reference"
  RB_FILE_NAME = "fib_ruby.rb"
  REFERENCE_RUBY_FILE_PATH = "#{REFERENCE_DIR}/#{RB_FILE_NAME}"
  REFERENCE_IR_FILE_PATH = "#{REFERENCE_DIR}/fib_ir.ir"
  EXPECTED_CREATED_IR_FOLDER_PATH = "#{REFERENCE_DIR}/ir"
  EXPECTED_CREATED_IR_FILE_PATH = "#{EXPECTED_CREATED_IR_FOLDER_PATH}/fib_ruby.ir"

  # Produce Ruby runtime and IRScope from .rb file
  before :all do
    instance_config = RubyInstanceConfig.new
    instance_config.script_file_name = RB_FILE_NAME
    instance_config.current_directory = REFERENCE_DIR

    @runtime = Ruby.new_instance(instance_config)

    rb_file_content = File.read(REFERENCE_RUBY_FILE_PATH)
    ast = JRuby.parse(rb_file_content)
    manager = @runtime.ir_manager
    manager.dry_run = true
    is_19 = false
    @scope = IRBuilder.createIRBuilder(manager, is_19).buildRoot(ast)
  end

  # Clean up
  after :all do
    if File.exists?(EXPECTED_CREATED_IR_FILE_PATH)
      File.delete(EXPECTED_CREATED_IR_FILE_PATH)
      Dir.delete(EXPECTED_CREATED_IR_FOLDER_PATH)
    end
  end

  describe '.persist' do    
    before :all do
      IRReadingContext::INSTANCE.setFileName(RB_FILE_NAME)
      IRPersistenceFacade.persist(@scope, @runtime)
    end
    
    it 'should create .ir file in ir directory relatively to rb file' do
      File.exists?(EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end

    it 'should create file identical to the reference one' do
      FileUtils.identical?(REFERENCE_IR_FILE_PATH, EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end
  end

  describe '.read' do
    it 'should read IR from disk without loss of information'
  end

end
require 'java'
require 'jruby'
require 'fileutils.rb'

java_import org.jruby.Ruby
java_import org.jruby.RubyInstanceConfig
java_import org.jruby.ast.Node
java_import org.jruby.ir.IRBuilder
java_import org.jruby.ir.IRManager
java_import org.jruby.ir.IRScope
java_import org.jruby.ir.persistence.persist.IRPersister
java_import org.jruby.ir.persistence.read.IRReadingContext

# Fibbonacci example from http://www.engineyard.com/blog/2012/oss-grant-roundup-jruby-runtime/ is used here
describe IRPersister do
  # Pathes
  CURRENT_DIR = File.expand_path(File.dirname(__FILE__))
  REFERENCE_DIR = "#{CURRENT_DIR}/reference"
  RB_FILE_NAME = "fib_ruby.rb"
  REFERENCE_RUBY_FILE_PATH = "#{REFERENCE_DIR}/#{RB_FILE_NAME}"
  REFERENCE_IR_FILE_PATH = "#{REFERENCE_DIR}/fib_ir.ir"
  EXPECTED_CREATED_IR_FOLDER_PATH = "#{REFERENCE_DIR}/ir"
  EXPECTED_CREATED_IR_FILE_PATH = "#{EXPECTED_CREATED_IR_FOLDER_PATH}/fib_ruby.ir"

  # Produce Ruby runtime and IRScope from .rb file
  before :all do
    instance_config = RubyInstanceConfig.new
    instance_config.script_file_name = RB_FILE_NAME
    instance_config.current_directory = REFERENCE_DIR

    @runtime = Ruby.new_instance(instance_config)

    rb_file_content = File.read(REFERENCE_RUBY_FILE_PATH)
    ast = JRuby.parse(rb_file_content)
    manager = @runtime.ir_manager
    manager.dry_run = true
    is_19 = false
    @scope = IRBuilder.createIRBuilder(manager, is_19).buildRoot(ast)
  end

  # Clean up
  after :all do
    if File.exists?(EXPECTED_CREATED_IR_FILE_PATH)
      File.delete(EXPECTED_CREATED_IR_FILE_PATH)
      Dir.delete(EXPECTED_CREATED_IR_FOLDER_PATH)
    end
  end

  describe '.persist' do    
    before :all do
      IRReadingContext::INSTANCE.setFileName(RB_FILE_NAME)
      IRPersistenceFacade.persist(@scope, @runtime)
    end
    
    it 'should create .ir file in ir directory relatively to rb file' do
      File.exists?(EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end

    it 'should create file identical to the reference one' do
      FileUtils.identical?(REFERENCE_IR_FILE_PATH, EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end
  end

end
