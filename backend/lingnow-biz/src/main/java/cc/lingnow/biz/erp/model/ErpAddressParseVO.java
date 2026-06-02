package cc.lingnow.biz.erp.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpAddressParseVO {

    private String contactName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String normalizedAddress;
    private Integer confidence;
    private List<String> warnings = new ArrayList<>();
}
