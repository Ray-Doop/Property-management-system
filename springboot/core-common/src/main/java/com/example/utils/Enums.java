// util/Enums.java
package com.example.utils;

public interface Enums {
    interface BillStatus {
        int CANCELED = 0, PENDING = 1, PAID = 2, CLOSED = 3;
    }
    interface PayStatus {
        int CLOSED = 0, PENDING = 1, SUCCESS = 2, FAIL = 3, REFUNDED = 4;
    }
}
