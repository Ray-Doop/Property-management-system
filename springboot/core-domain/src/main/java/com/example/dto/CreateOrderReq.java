// com/example/property/dto/CreateOrderReq.java
package com.example.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//import javax.validation.constraints.NotEmpty;
//import javax.validation.constraints.NotNull;
import java.util.List;
@Data
public class CreateOrderReq {
    @NotNull
    private Long userId;
    @NotEmpty
    private List<Long> dueIds;  // 选择要支付的代缴费记录ID
}
