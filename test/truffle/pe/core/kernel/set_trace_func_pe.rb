module KernelSetTraceFuncFixtures

  def self.method_with_many_lines(a, b)
    x = a
    y = b
    z = x + y
    z
  end

end

PETests.tests do

  describe "Kernel" do

    describe "set_trace_func" do

      begin
        set_trace_func proc { |event, file, line, id, binding, classname|
        }

        broken_example "still has Fixnum#+ Fixnum" do
          Truffle::Debug.assert_constant KernelSetTraceFuncFixtures.method_with_many_lines(14, 2)
        end
      ensure
        set_trace_func nil
      end

    end

  end

end
