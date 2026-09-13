package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.PublicMarketingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 营销客人 H5 公开读取与浏览归因控制器（TR-MARKETING-PUBLIC-API-54）。
 * <p>
 * 免登录公开接口（WebMvcConfig 已放行 /api/public/**）：
 * <ul>
 *   <li>GET /api/public/marketing/activities?storeId=... 公开活动列表；</li>
 *   <li>GET /api/public/marketing/a/{publicSlug} 单个公开快照（不可见统一 404）；</li>
 *   <li>POST /api/public/marketing/events 脱敏浏览事件（首版仅 view，幂等）。</li>
 * </ul>
 * 不扩内部状态机或咨询。
 */
@RestController
@RequestMapping("/api/public/marketing")
@CrossOrigin(origins = "*")
public class PublicMarketingController {

    private final PublicMarketingService publicMarketingService;

    public PublicMarketingController(PublicMarketingService publicMarketingService) {
        this.publicMarketingService = publicMarketingService;
    }

    /** 公开活动列表：仅合法正门店，只返回可见快照。 */
    @GetMapping("/activities")
    public Result<List<Map<String, Object>>> listActivities(
            @RequestParam(required = false) Long storeId) {
        try {
            return Result.success(publicMarketingService.listPublicActivities(storeId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取公开活动失败");
        }
    }

    /** 单个公开快照：草稿/待批/暂停/过期/未来生效/未知 slug/错误门店关系统一 404。 */
    @GetMapping("/a/{publicSlug}")
    public Result<Map<String, Object>> getActivity(@PathVariable String publicSlug) {
        try {
            Map<String, Object> snapshot = publicMarketingService.getPublicActivityBySlug(publicSlug);
            if (snapshot == null) {
                return Result.error(404, "公开内容不存在");
            }
            return Result.success(snapshot);
        } catch (Exception e) {
            return Result.error(500, "获取公开活动失败");
        }
    }

    /** 脱敏浏览事件（首版仅 view），requestId 幂等。 */
    @PostMapping("/events")
    public Result<Map<String, Object>> recordEvent(@RequestBody Map<String, Object> body) {
        try {
            return Result.success(publicMarketingService.recordViewEvent(body));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(409, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "记录浏览事件失败");
        }
    }
}
