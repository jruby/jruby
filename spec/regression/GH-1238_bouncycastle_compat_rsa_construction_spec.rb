require 'openssl'

describe "OpenSSL::PKey::RSA.new" do
  it "constructs a private key instance" do
    pem = <<EOS
-----BEGIN ENCRYPTED PRIVATE KEY-----
MIICxjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIaYgszaX31yECAggA
MBQGCCqGSIb3DQMHBAij3LmXGCmB8wSCAoDcLnAeXiBugFmwXd3wrvznlKvwHkP2
76lIrTiwDRZOLuaKHdBgNQDJ3NP+UPGdM7YEyNqdfdbN/3cLd0qfzeobuU+c/lGI
aE5pAwlWm5lK9boTsJnCqaDFEgJz2khZF+7RqYQVSG7MTM9SnIRNScLKjhTk7AaF
PD2qSnMVtixw/VfwdzhUknuwP2monLY8Ip/l9abicmBp9HGQ+0WA/nKQLQ/egWG0
S6rrXsH91exaxL7gcZL8jF+Ub7VDt4Hvx1RB/3r12k7AQGsK+TyIrKQFUllSnSq/
eFwBqpLSKWYyGJZlkJzW5MTHyeXqpTvav6T7e2mKZ4GG/a8THoWxLLrKeODFFoWn
LQNOQZ2Axa15E0TdeSkaumsOWPJm5DgFxf/1cRNxhJqYdX68QjWXeNS2SXPZBwlx
HCaAYo6OoCHZQ7O/3MpiT3rUAk30fbSa09VSvrenYi5s5lPieKFt3QZI44uGvi9j
MXyN4fkjzzXasE0xZzf6bQLS6aM+ucyQ8CMv0oAgAndoeKu10Ha4KmdT5dZf3LHj
BUXZDYp3Q5UF6ePyxKBdAqJf4PNKl4+VehYJ4eQ6CIQiSxSuWv9T+2b90PyDuRkz
sB1XZpeDD6dhQuU9GjdwCTyatITcm97ZkbdZEoQiDpiWQB4parTvKLKbD4AbP/+E
08btPFgXNocFUjLb5lB4Y/6RqaQxY7VoaFOPOfPpWPXF26X9Y5y3y+ymXdYFpkhp
wGBGScH+dutQWHoRV1TWUjv9a7CuzUxCX2Hrjooz1BtOnG8CoPA7K43+kvire5jN
529p6u+FtUZPUWLm5L5WHBUECEtJGw3ImjosX1HtoM/rW34XDmMHuN0u
-----END ENCRYPTED PRIVATE KEY-----
EOS
    pem_password = "password"
    expect {OpenSSL::PKey::RSA.new(pem, pem_password)}.not_to raise_error
  end
end
