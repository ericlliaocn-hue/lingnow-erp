package cc.lingnow.admin.model.vo.shop;

import lombok.Data;

@Data
public class ShopLoginVO {
    private String token;
    private Long accountId;
    private Long customerId;
    private String username;
    private String nickname;
    private String customerName;
}
