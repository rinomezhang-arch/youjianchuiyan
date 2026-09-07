package com.youjian.banquet.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.youjian.banquet.config.LegalCosConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 法务案卷证据服务。
 *
 * 关键设计：<b>预签名链接一律现签现用，短时效（默认 15 分钟）</b>。
 * 早期给律师的那版把 180 天的长效链接直接烤进页面里，等于链接一旦转发就长期
 * 可访问；改成登录后按需签发，链接很快失效，转发出去也没用。这是本模块存在
 * 的主要理由之一。
 */
@Service
public class LegalEvidenceService {

    @Autowired(required = false)
    @Qualifier("legalCosClient")
    private COSClient cos;

    @Autowired
    private LegalCosConfig cfg;

    public boolean available() {
        return cos != null;
    }

    /** 列出案卷根目录下的全部证据，按文件夹分组，不含链接。 */
    public List<Map<String, Object>> listFolders() {
        requireReady();
        Map<String, List<Map<String, Object>>> byFolder = new LinkedHashMap<>();
        String root = cfg.getRoot();

        ListObjectsRequest req = new ListObjectsRequest();
        req.setBucketName(cfg.getBucket());
        req.setPrefix(root);
        req.setMaxKeys(1000);
        String marker = null;
        ObjectListing listing;
        do {
            req.setMarker(marker);
            listing = cos.listObjects(req);
            for (COSObjectSummary s : listing.getObjectSummaries()) {
                String key = s.getKey();
                // 目录占位对象（0 字节、以 / 结尾）不是证据
                if (key.endsWith("/") || s.getSize() == 0) continue;
                String rel = key.substring(root.length());
                int cut = rel.lastIndexOf('/');
                String folder = cut < 0 ? "未分类" : rel.substring(0, cut);
                String name = cut < 0 ? rel : rel.substring(cut + 1);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", key);
                item.put("name", name);
                item.put("size", s.getSize());
                item.put("lastModified", s.getLastModified());
                byFolder.computeIfAbsent(folder, k -> new ArrayList<>()).add(item);
            }
            marker = listing.getNextMarker();
        } while (listing.isTruncated());

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : byFolder.entrySet()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("folder", e.getKey());
            g.put("label", labelOf(e.getKey()));
            g.put("count", e.getValue().size());
            g.put("items", e.getValue());
            out.add(g);
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("folder"))));
        return out;
    }

    /**
     * 为单个证据签发短时效访问链接。
     * @param download true 则加 Content-Disposition: attachment，点击直接下载
     */
    public String sign(String key, boolean download) {
        requireReady();
        if (key == null || !key.startsWith(cfg.getRoot())) {
            // 防目录穿越：只允许签本案卷根目录下的对象
            throw new IllegalArgumentException("非本案卷证据，拒绝签发");
        }
        GeneratePresignedUrlRequest req =
                new GeneratePresignedUrlRequest(cfg.getBucket(), key, HttpMethodName.GET);
        req.setExpiration(new Date(System.currentTimeMillis() + cfg.getUrlExpireSeconds() * 1000L));
        // 必须显式指定：桶里对象默认带 attachment，不覆盖的话"查看"也会变成下载，
        // 图片和视频就没法在浏览器里直接看。
        req.addRequestParameter("response-content-disposition", download ? "attachment" : "inline");
        URL url = cos.generatePresignedUrl(req);
        return url.toString();
    }

    private void requireReady() {
        if (cos == null) {
            throw new IllegalStateException("法务 COS 未配置，请设置 LEGAL_COS_* 环境变量");
        }
    }

    /** 文件夹的中文显示名。COS 上的目录名本身已是中文，这里只做少量补充说明。 */
    private String labelOf(String folder) {
        switch (folder) {
            case "01_合同":                              return "一、合同原件";
            case "02_设计图纸":                          return "二、乙方交付的全部设计成果";
            case "03_微信证据/单聊_倪静云15105637557":   return "三、微信单聊·倪静云（2025-01-26 至 2026-02-09 全程）";
            case "03_微信证据/张婧张总倪静云的聊天记录": return "四、微信·张婧与倪静云";
            case "03_微信证据/消防施工立阳建材":         return "五、微信·消防施工（立阳建材）";
            case "03_微信证据/图纸以及源文件":           return "六、微信往来的图纸与源文件";
            case "03_微信证据/设计施工群_倪静云相关":    return "七、设计施工对接群·倪静云相关";
            case "04_现场视频":                          return "八、竣工后现场视频";
            case "07 消防设计师黄大海":                  return "六之二、乙方委托的设计人·黄大海往来（关键）";
            case "05_案卷文书":                          return "九、案卷文书";
            case "06_现场照片":                          return "现场照片（上传归档）";
            case "99_备查_重复件与低相关":               return "十、备查（重复件，正卷已另存同件）";
            default:                                     return folder;
        }
    }

    /**
     * 上传一个证据文件到 COS，按类型归类到对应目录，文件名打时间戳。
     *
     * @param originalName 用户上传的原始文件名
     * @param contentType  文件 MIME
     * @param bytes        文件字节
     * @param classifyBy   客户端指定的目标目录（可为空，由类型推断）
     * @return 上传结果：{key, folder, name, url(短效)}
     */
    public Map<String, Object> upload(String originalName, String contentType, byte[] bytes, String classifyBy) {
        requireReady();
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("文件为空");

        String folder = resolveFolder(originalName, contentType, classifyBy);
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String safe = sanitize(originalName);
        // 时间戳前缀，便于归档追溯；保留原文件名后缀
        String name = ts + "_" + safe;
        String key = cfg.getRoot() + folder + "/" + name;

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(bytes.length);
        if (contentType != null && !contentType.isBlank()) meta.setContentType(contentType);
        cos.putObject(new PutObjectRequest(cfg.getBucket(), key,
                new ByteArrayInputStream(bytes), meta));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", key);
        out.put("folder", folder);
        out.put("name", name);
        out.put("size", bytes.length);
        return out;
    }

    /** 归档说明写入 COS（与目标目录并列的一个 .txt），记录上传者、时间、文件名。 */
    public void writeNote(String who, String note, String key) {
        requireReady();
        String noteKey = key + ".note.txt";
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String body = "上传人: " + (who == null ? "" : who) + "\n时间: " + ts + "\n\n" + (note == null ? "" : note) + "\n";
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(b.length);
        meta.setContentType("text/plain; charset=utf-8");
        cos.putObject(new PutObjectRequest(cfg.getBucket(), noteKey, new ByteArrayInputStream(b), meta));
    }

    /** 按扩展名/MIME 推断归档目录。 */
    private String resolveFolder(String name, String contentType, String classifyBy) {
        if (classifyBy != null && !classifyBy.isBlank()) return classifyBy;
        String n = name == null ? "" : name.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".avi") || n.endsWith(".mkv")
                || ct.startsWith("video/")) return "04_现场视频";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif")
                || n.endsWith(".webp") || ct.startsWith("image/")) return "06_现场照片";
        if (n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".txt")
                || n.endsWith(".md") || n.endsWith(".xls") || n.endsWith(".xlsx")) return "05_案卷文书";
        return "05_案卷文书";
    }

    /** 对外：仅推断推荐目录，不落盘（供 /analyze 用）。 */
    public String suggestFolder(String name, String contentType) {
        return resolveFolder(name, contentType, null);
    }

    /** 去除文件名中的路径符号，防目录穿越。 */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String s = name.replaceAll("[\\\\/]", "_");
        int cut = s.lastIndexOf('/');
        if (cut >= 0) s = s.substring(cut + 1);
        return s;
    }

    // ============================================================ 开庭助理聊天记录

    private static final Logger log = LoggerFactory.getLogger(LegalEvidenceService.class);

    private static final String CHAT_DIR = "09_开庭助理";
    private static final String CHAT_FILE = "chat_history.jsonl";

    /** 旧版单文件历史。2026-09-06 之前的记录在这里，只读，不再写入。 */
    private String legacyChatKey() {
        return cfg.getRoot() + CHAT_DIR + "/" + CHAT_FILE;
    }

    /** 新版：每条消息一个对象，放在这个前缀下。 */
    private String msgPrefix() {
        return cfg.getRoot() + CHAT_DIR + "/msgs/";
    }

    /**
     * 追加一条聊天记录。
     *
     * <p>每条消息单独写成一个对象 {@code msgs/yyyy-MM-dd/{ts}-{msgId}.json}，
     * <b>全程不读旧内容</b>。
     *
     * <p>旧实现是“读回整份 JSONL → 拼上新行 → 整体覆写”，而 {@code readObject}
     * 把所有异常都吞成 null，于是 COS 只要有一次瞬时读失败，整份历史就会被
     * 一行新记录静静地替掉。2026-09-05 07:41 真的发生过一次，之前约 47 组
     * 问答被抹除，日志里没有任何异常。现在写入路径上没有读，结构上不可能再被覆盖。
     */
    public void appendChat(String userId, String userName, String role, String content, String msgId) {
        requireReady();
        long ts = System.currentTimeMillis();
        String line = "{\"msgId\":\"" + esc(msgId)
                + "\",\"userId\":\"" + esc(userId)
                + "\",\"userName\":\"" + esc(userName)
                + "\",\"role\":\"" + esc(role)
                + "\",\"content\":\"" + esc(content)
                + "\",\"ts\":" + ts + "}";
        String day = new SimpleDateFormat("yyyy-MM-dd").format(new Date(ts));
        String key = msgPrefix() + day + "/" + ts + "-" + sanitize(msgId) + ".json";
        byte[] b = line.getBytes(StandardCharsets.UTF_8);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(b.length);
        meta.setContentType("application/json; charset=utf-8");
        cos.putObject(new PutObjectRequest(cfg.getBucket(), key, new ByteArrayInputStream(b), meta));
    }

    /** 收齐全部聊天记录行：旧单文件 + msgs/ 下每条一个对象，按时间先后。 */
    private List<String> allChatLines() {
        List<String> lines = new ArrayList<>();
        String legacy = null;
        try {
            legacy = readObject(legacyChatKey());
        } catch (Exception e) {
            log.warn("[Legal] 读旧聊天文件失败：{}", e.getMessage());
        }
        if (legacy != null && !legacy.isBlank()) {
            for (String l : legacy.split("\n")) {
                String t = l.trim();
                if (!t.isEmpty()) lines.add(t);
            }
        }
        List<String> keys = new ArrayList<>();
        ListObjectsRequest req = new ListObjectsRequest();
        req.setBucketName(cfg.getBucket());
        req.setPrefix(msgPrefix());
        req.setMaxKeys(1000);
        ObjectListing listing;
        do {
            listing = cos.listObjects(req);
            for (COSObjectSummary o : listing.getObjectSummaries()) {
                if (!o.getKey().endsWith("/")) keys.add(o.getKey());
            }
            req.setMarker(listing.getNextMarker());
        } while (listing.isTruncated());
        Collections.sort(keys);
        for (String k : keys) {
            try {
                String one = readObject(k);
                if (one != null && !one.isBlank()) lines.add(one.trim());
            } catch (Exception e) {
                // 单条读不到不能拖垮整份历史
                log.warn("[Legal] 跳过读不到的消息 {}：{}", k, e.getMessage());
            }
        }
        return lines;
    }

    /** 读取全部聊天记录（JSONL 文本）。无则返回空串。 */
    public String readChatHistory() {
        requireReady();
        StringBuilder sb = new StringBuilder();
        for (String l : allChatLines()) sb.append(l).append("\n");
        return sb.toString();
    }

    /** 读取某个用户自己的聊天记录（按 userId 过滤）。 */
    public String readChatHistoryOf(String userId) {
        requireReady();
        boolean all = (userId == null || userId.isBlank());
        StringBuilder sb = new StringBuilder();
        for (String l : allChatLines()) {
            if (all || userId.equals(fieldOf(l, "userId"))) sb.append(l).append("\n");
        }
        return sb.toString();
    }

    /** 从一行 JSON 里取出某个字符串字段（手写解析，避免额外依赖）。 */
    private String fieldOf(String json, String field) {
        String key = "\"" + field + "\":\"";
        int i = json.indexOf(key);
        if (i < 0) return "";
        int st = i + key.length();
        int e = json.indexOf("\"", st);
        if (e < 0) return "";
        return json.substring(st, e);
    }

    /**
     * 读 COS 对象为 UTF-8 字符串。
     * <b>对象不存在返回 null；真的读失败则抛出</b>——两者以前都返回 null，
     * 调用方无法区分，正是聊天记录被覆写的根因。
     */
    private String readObject(String key) {
        try {
            InputStream in = cos.getObject(cfg.getBucket(), key).getObjectContent();
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            in.close();
            return s;
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) return null;
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读 COS 对象失败: " + key, e);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
