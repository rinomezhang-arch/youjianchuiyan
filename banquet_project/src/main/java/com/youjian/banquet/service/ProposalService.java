package com.youjian.banquet.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "提案-确认"两步写操作的通用底座。任何一个写类工具在模型侧其实是两个函数：
 * propose_xxx（只校验参数、拼出人类可读摘要，不落库、不改数据）+ confirm_action（真正执行）。
 * 硬约束靠这里的状态机保证，不是靠提示词"叮嘱"模型走两步：模型没有真实调用过 propose_xxx，
 * confirm_action 这边就查不到待确认的提案，直接拒绝执行。
 *
 * 按 sessionKey（比如某个对话线程）记录"这个会话当前唯一待确认的提案"——不要求模型记住/传回
 * proposalId：我们目前的对话历史只在轮次之间传递人类能看到的文字，不会把上一轮工具调用返回的
 * proposalId 带过去，模型在下一条独立消息里其实拿不到这个ID。改成"确认最近这一个"之后，
 * confirm_action 不需要参数，天然就不会有这个问题——一个会话同一时间只允许有一个待确认提案，
 * 新的 propose 会覆盖旧的（旧提案作废，不会残留一个模型也不知道该不该确认的悬空提案）。
 */
@Service
public class ProposalService {

    private static final long EXPIRE_MS = 10 * 60 * 1000L; // 提案10分钟内有效，超时要求重新发起

    public record Proposal(String actionType, Map<String, Object> params, String summary, long expiresAt) {
    }

    private final ConcurrentHashMap<String, Proposal> latestBySession = new ConcurrentHashMap<>();

    public String create(String sessionKey, String actionType, Map<String, Object> params, String summary) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        latestBySession.put(sessionKey, new Proposal(actionType, params, summary, System.currentTimeMillis() + EXPIRE_MS));
        return id;
    }

    /** 取出并立即移除该会话当前待确认的提案（一次性消费）；不存在或已过期返回 null。 */
    public Proposal consumeLatest(String sessionKey) {
        Proposal p = latestBySession.remove(sessionKey);
        if (p == null) return null;
        if (System.currentTimeMillis() > p.expiresAt()) return null;
        return p;
    }
}
