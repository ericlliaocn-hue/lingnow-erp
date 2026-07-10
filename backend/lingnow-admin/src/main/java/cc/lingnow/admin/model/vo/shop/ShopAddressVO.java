package cc.lingnow.admin.model.vo.shop;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ShopAddressVO {

    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private String streetCode;
    private String streetName;
    private String villageCode;
    private String villageName;
    private List<String> regionPath = new ArrayList<>();
    private List<String> regionPathNames = new ArrayList<>();
    private String detailAddress;
    private String fullAddress;
    private String addressLabel;
    private Boolean defaultFlag;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
