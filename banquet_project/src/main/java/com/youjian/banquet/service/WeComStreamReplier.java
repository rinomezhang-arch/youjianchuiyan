package com.youjian.banquet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 企业微信"智能机器人"的流式回复工具。
 * <p>
 * 解决的问题：Main/Leo 现在自己写 SQL 干活，一个稍复杂的问题要跑几十秒甚至几分钟。
 * 原来的做法是等 AI 全部算完再一次性 push 回去，这段时间用户那边一片空白，
 * 看起来就像机器人死了（2026-09-03 实测：张总连发几条"你特么死了？"，其实它在正常干活）。
 * <p>
 * 官方给的正解是流式消息（developer.work.weixin.qq.com/document/path/101031）：
 * {@code msgtype=stream}，同一个 {@code stream.id} 可以多次推送，
 * 后一次的 content 覆盖前一次（不是追加），{@code finish=false} 表示还没说完、
 * {@code finish=true} 收尾。所以可以先秒回一句"收到，我查一下"占住位置，
 * 等真结果出来再用同一个 id 覆盖掉。
 * <p>
 * 注意：response_url 是这次回调专属的，只在本次会话内有效，
 * 所以"先占位再覆盖"必须在同一次 handleAsync 里完成，不能跨消息复用。
 */
@Component
public class WeComStreamReplier {

    private static final Logger log = LoggerFactory.getLogger(WeComStreamReplier.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 这批机器人是否支持流式消息。文档上写着支持，但 2026-09-03 生产实测：
     * 推 {@code msgtype=stream} 被企业微信以 {@code errcode 40008 invalid message type} 拒掉，
     * 结果占位和最终结果两条全都发不出去，机器人直接变哑巴。
     * 所以这里做成运行时自动探测：第一次被 40008 拒掉之后就永久退回 markdown，
     * 保证"宁可体验差一点，也绝不能一个字都发不出去"。
     */
    private volatile boolean streamSupported = true;

    /** 新开一条流式消息，返回这条消息的 streamId，后续用它来覆盖更新 */
    public String begin(String tag, String responseUrl, String placeholder) {
        String streamId = "s-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        push(tag, responseUrl, streamId, placeholder, false);
        return streamId;
    }

    /** 用最终结果覆盖掉之前的占位内容，并标记这条消息说完了 */
    public void finish(String tag, String responseUrl, String streamId, String finalContent) {
        push(tag, responseUrl, streamId, finalContent, true);
    }

    private void push(String tag, String responseUrl, String streamId, String content, boolean finished) {
        // 先试流式（能用的话体验最好：占位那条会被真结果原地覆盖，不刷屏）
        if (streamSupported && pushOnce(tag, responseUrl, buildStreamBody(streamId, content, finished), "流式")) {
            return;
        }
        // 流式不被支持时（实测本机器人返回 errcode 40008 invalid message type）自动退回 markdown。
        // markdown 是两条独立消息（占位一条、结果一条），体验差一点，但至少不会一个字都发不出去。
        if (!finished) {
            // 占位那条在 markdown 模式下会永远留在对话里，等于刷屏，所以干脆不发占位，
            // 只保证最终结果一定能送达。
            return;
        }
        pushOnce(tag, responseUrl, buildMarkdownBody(content), "markdown");
    }

    private Map<String, Object> buildStreamBody(String streamId, String content, boolean finished) {
        Map<String, Object> stream = new HashMap<>();
        stream.put("id", streamId);
        stream.put("content", content);
        stream.put("finish", finished);
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "stream");
        body.put("stream", stream);
        return body;
    }

    private Map<String, Object> buildMarkdownBody(String content) {
        Map<String, Object> md = new HashMap<>();
        md.put("content", content);
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", md);
        return body;
    }

    /** 推一次，返回是否被企业微信真正接受（errcode 非0 也算失败，不能只看 HTTP 200） */
    private boolean pushOnce(String tag, String responseUrl, Map<String, Object> body, String kind) {
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(responseUrl, body, String.class);
            String respBody = resp.getBody() == null ? "" : resp.getBody();
            // 企业微信这个接口即使业务失败也返回 HTTP 200，真正的成败在 errcode 里，
            // 2026-09-03 就栽在这上面：以为推成功了，其实全被 40008 拒了，用户那边一个字没收到。
            boolean accepted = resp.getStatusCode().is2xxSuccessful() && respBody.contains("\"errcode\":0");
            log.info("[{}] {}推送 {}: {} {}", tag, kind, accepted ? "成功" : "被拒",
                    resp.getStatusCode(), respBody);
            if (!accepted && "流式".equals(kind) && respBody.contains("40008")) {
                // 这个机器人不支持流式，本进程内不用再试了，直接走 markdown
                streamSupported = false;
            }
            return accepted;
        } catch (Exception e) {
            log.warn("[{}] {}推送异常: {}", tag, kind, e.getMessage());
            return false;
        }
    }
}
