// com/example/entity/message/MessageRecipient.java
package com.example.entity.message;
import lombok.Data;
import java.util.Date;

@Data
public class MessageRecipient {
    private Long id;
    private Long msgId;
    private Long receiverId;
    private Boolean readFlag;
    private Date readTime;
    private Boolean deleted;
}
