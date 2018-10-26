describe "Kernel#sleep" do
  it "can be interrupted using java.lang.Thread.interrupt" do
    t = Thread.new do
      Kernel.sleep
      :ok
    end

    Thread.pass until t.status == 'sleep'

    JRuby.reference(t).native_thread.interrupt

    expect(t.value).to eq :ok
  end
end