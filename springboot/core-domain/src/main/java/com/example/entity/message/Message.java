// com/example/entity/message/Message.java
package com.example.entity.message;
import lombok.Data;
import java.util.Date;

@Data
public class Message {
    private Long msgId;
    private Long senderId;
    private String title;
    private String content;
    private String msgType; // DIRECT/SYSTEM
    private Date createTime;
}
