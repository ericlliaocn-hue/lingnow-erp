package cc.lingnow.admin.model.bo.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ShopAddressSaveBO {

    @NotBlank(message = "收货人不能为空")
    private String receiverName;
    @NotBlank(message = "手机号不能为空")
    private String receiverPhone;
    private List<String> regionPath;
    private List<String> regionPathNames;
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;
    private String addressLabel;
    private Boolean defaultFlag;
}
