map_lambda = lambda do |arr, method|
  return arr.map(&method.to_sym)
end

arr = [ '1', '2', '3' ]; arr.map! { |e| e * e.to_i }

$symbol_proc_result = map_lambda.call(arr, :size)
