package cc.lingnow.admin.controller;

import cc.lingnow.admin.manager.SysPostManager;
import cc.lingnow.admin.model.bo.PostQueryBO;
import cc.lingnow.admin.model.bo.PostSaveBO;
import cc.lingnow.admin.model.vo.PostVO;
import cc.lingnow.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制器
 *
 * @author LingNow Team
 */
@Tag(name = "岗位管理", description = "岗位管理接口")
@RestController
@RequestMapping("/system/post")
@RequiredArgsConstructor
@Validated
public class SysPostController {

    private final SysPostManager postManager;

    @Operation(summary = "获取岗位列表")
    @GetMapping("/list")
    public Result<List<PostVO>> list(PostQueryBO query) {
        return Result.success(postManager.listPosts(query));
    }

    @Operation(summary = "获取岗位详细信息")
    @GetMapping("/{postId}")
    public Result<PostVO> getInfo(@PathVariable Long postId) {
        return Result.success(postManager.getPost(postId));
    }

    @Operation(summary = "新增岗位")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody PostSaveBO bo) {
        postManager.addPost(bo);
        return Result.success();
    }

    @Operation(summary = "修改岗位")
    @PutMapping
    public Result<Void> edit(@Valid @RequestBody PostSaveBO bo) {
        postManager.updatePost(bo);
        return Result.success();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{postIds}")
    public Result<Void> remove(@PathVariable List<Long> postIds) {
        postManager.removePost(postIds);
        return Result.success();
    }
}
