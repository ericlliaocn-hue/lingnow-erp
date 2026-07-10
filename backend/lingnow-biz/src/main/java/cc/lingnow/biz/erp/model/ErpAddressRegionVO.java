package cc.lingnow.biz.erp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ErpAddressRegionVO implements Serializable {

    private String code;
    private String name;
    private Integer level;
    private Boolean leaf;
    private List<String> path = new ArrayList<>();
    private List<String> pathNames = new ArrayList<>();

    public ErpAddressRegionVO(String code, String name, Integer level, Boolean leaf) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.leaf = leaf;
    }

    public ErpAddressRegionVO(String code, String name, Integer level, Boolean leaf, List<String> path, List<String> pathNames) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.leaf = leaf;
        this.path = path == null ? new ArrayList<>() : new ArrayList<>(path);
        this.pathNames = pathNames == null ? new ArrayList<>() : new ArrayList<>(pathNames);
    }
}
