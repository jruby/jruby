describe "JRuby numeric literal caching" do
  def numeric_literal_samples
    samples = []
    iteration = 0
    # Revisit each literal site within one invocation. Separate calls may use
    # different caches when a method transitions from the interpreter to JIT.
    while iteration < 2
      samples << {
        complex: [5i, 0.6i, 18446744073709551616i, 5ri, 0.6ri,
                  18446744073709551616ri, 0.00000000000000000001ri],
        rational: [5r, 0.6r, 18446744073709551616r, 0.00000000000000000001r],
        normalized_rational: [0.5000000000000000000000000000r,
                              0.00000000000000000001r,
                              100000000000000000000.5000000000000000000000r,
                              1.23456789012345678901r],
        normalized_complex: [0.5000000000000000000000000000ri,
                             0.00000000000000000001ri,
                             100000000000000000000.5000000000000000000000ri,
                             1.23456789012345678901ri]
      }
      iteration += 1
    end
    samples
  end

  it "reuses frozen Complex literals with integer, float, bignum and rational imaginary parts" do
    first, second = numeric_literal_samples
    first[:complex].each_with_index do |literal, index|
      expect(literal).to equal(second[:complex][index])
      expect(literal).to be_frozen
    end
  end

  it "reuses frozen Rational literals with integer, decimal and bignum components" do
    first, second = numeric_literal_samples
    first[:rational].each_with_index do |literal, index|
      expect(literal).to equal(second[:rational][index])
      expect(literal).to be_frozen
    end
  end

  it "normalizes and reuses Rational and Complex literals with large decimal components" do
    first, second = numeric_literal_samples
    parts = [[1, 2], [1, 100000000000000000000],
             [200000000000000000001, 2],
             [123456789012345678901, 100000000000000000000]]
    parts.each_with_index do |expected, index|
      rational = first[:normalized_rational][index]
      complex = first[:normalized_complex][index]
      expect(rational).to equal(second[:normalized_rational][index])
      expect(complex).to equal(second[:normalized_complex][index])
      expect([rational.numerator, rational.denominator]).to eq(expected)
      expect([complex.imaginary.numerator, complex.imaginary.denominator]).to eq(expected)
    end
  end
end
