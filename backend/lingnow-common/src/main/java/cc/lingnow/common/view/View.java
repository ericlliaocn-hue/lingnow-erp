package cc.lingnow.common.view;

/**
 * JSON View 响应字段分组定义
 * 用于 @JsonView 注解，控制 Controller 返回的字段
 */
public interface View {

    /**
     * 基础视图 (仅包含最基本字段，如 ID, Name)
     */
    interface Base {
    }

    /**
     * 列表视图 (包含列表页展示的常用字段)
     * 继承 Base，自动包含 Base 的字段
     */
    interface List extends Base {
    }

    /**
     * 详情视图 (包含所有详情字段)
     * 继承 List，自动包含 List 和 Base 的字段
     */
    interface Detail extends List {
    }
}
