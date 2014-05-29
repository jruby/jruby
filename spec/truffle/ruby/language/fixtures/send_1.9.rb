module LangSendSpecs
  module_function

  def fooR(*r); r; end
  def fooM0RQ1(*r, q); [r, q]; end
  def fooM0RQ2(*r, s, q); [r, s, q]; end
  def fooM1RQ1(a, *r, q); [a, r, q]; end
  def fooM1O1RQ1(a, b=9, *r, q); [a, b, r, q]; end
  def fooM1O1RQ2(a, b=9, *r, q, t); [a, b, r, q, t]; end

  def fooO1Q1(a=1, b); [a,b]; end
  def fooM1O1Q1(a,b=2,c); [a,b,c]; end
  def fooM2O1Q1(a,b,c=3,d); [a,b,c,d]; end
  def fooM2O2Q1(a,b,c=3,d=4,e); [a,b,c,d,e]; end
  def fooO4Q1(a=1,b=2,c=3,d=4,e); [a,b,c,d,e]; end
  def fooO4Q2(a=1,b=2,c=3,d=4,e,f); [a,b,c,d,e,f]; end

  def destructure2((a,b)); a+b; end
  def destructure2b((a,b)); [a,b]; end
  def destructure4r((a,b,*c,d,e)); [a,b,c,d,e]; end

end
