describe "java_import" do

  before(:all) { require 'java' }

  it "still works (is deprecated) on Object target" do
    obj = Object.new
    expect(obj.send :java_import).to eq([])
    expect(obj.send :java_import, 'java.util.Hashtable').to eq([java.util.Hashtable])
  end
end

