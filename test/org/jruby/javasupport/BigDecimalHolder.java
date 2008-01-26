package org.jruby.javasupport;

import java.math.BigDecimal;

public class BigDecimalHolder {

    private BigDecimal number;
    
    public BigDecimalHolder() {}

    public BigDecimal getNumber() {
        return number;
    }

    public void setNumber(BigDecimal number) {
        this.number = number;
    }
    
}
