require 'java'
require 'fileutils.rb'

java_import org.jruby.ir.persistence.IRPersistenceFacade

# Fibbonacci example from http://www.engineyard.com/blog/2012/oss-grant-roundup-jruby-runtime/ is used here
describe IRPersistenceFacade do
  # Pathes
  REFERENCE_DIR = "${Dir.pwd}/reference"
  REFERENCE_RUBY_FILE_PATH = "${REFERENCE_DIR}/fib_ruby.rb"
  REFERENCE_IR_FILE_PATH = "${REFERENCE_DIR}/fib_ir.jir"
  EXPECTED_CREATED_IR_FOLDER_PATH = "${REFERENCE_DIR}/.ir"
  EXPECTED_CREATED_IR_FILE_PATH = "${EXPECTED_CREATED_IR_FOLDER_PATH}/fib_ruby.jir"

  # Clean up
  after :all do
    if File.exists?(EXPECTED_CREATED_IR_FILE_PATH) do
      File.delete(EXPECTED_CREATED_IR_FILE_PATH)
      Dir.delete(EXPECTED_CREATED_IR_FOLDER_PATH)
    end
  end

  describe '.persist' do    
    before :all do
      IRPersistence.persist(REFERENCE_RUBY_FILE_PATH)
    end
    
    it 'should create .jir file in .ir directory relatively to rb file' do
      File.exists?(EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end

    it 'should create file identical to the reference one' do
      FileUtils.identical?(REFERENCE_RUBY_FILE_PATH, EXPECTED_CREATED_IR_FILE_PATH).should be_true
    end
  end

  # not quite sure whether we need IRPersistenceFacade.execute method or IRPersistenceFacade.execute will be enough
  describe '.execute' do
    it 'should execute persisted code with expected result'
  end

  describe '.read' do
    it 'should read IR from disk without loss of information'
  end

end
