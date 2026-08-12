package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrDocs;
import com.youjian.banquet.repository.HrDocsRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * HR文件服务类
 * 复刻自HR系统 DocsService，ORM从MyBatis Plus改为JPA+JdbcTemplate
 * 完整保留：文件上传/下载、MD5去重、导入导出
 *
 * @author cow
 * @since 2022-02-24
 */
@Service
public class HrDocsService {

    /** 文件上传路径 */
    @Value("${app.file.upload.path:./files/}")
    private String fileUploadPath;

    @Autowired
    private HrDocsRepository hrDocsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ==================== 文件上传/下载 ====================

    /**
     * 文件上传
     * 对应参考系统 upload(MultipartFile uploadFile, HttpServletRequest request)
     * 使用MD5去重，避免重复上传
     */
    public Result<HrDocs> upload(MultipartFile uploadFile, Long storeId, Integer staffId) throws Exception {
        if (storeId == null) {
            storeId = 1L;
        }

        // 判断上传的文件是否为空
        if (uploadFile == null || uploadFile.isEmpty()) {
            return Result.error(400, "文件不存在");
        }

        String originalFilename = uploadFile.getOriginalFilename();
        String extName = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        String filename = UUID.randomUUID().toString().replace("-", "").substring(2, 22) + "." + extName;

        // 获取文件的md5信息
        String md5 = md5(uploadFile.getBytes());
        List<HrDocs> docsList = hrDocsRepository.findByMd5AndIsDeleted(md5, 0);
        // 若文件已经存在，则不用上传
        if (docsList != null && !docsList.isEmpty()) {
            filename = docsList.get(0).getName();
        } else {
            File fold = new File(fileUploadPath);
            // 若存储上传文件的文件夹不存在，则创建
            if (!fold.exists()) {
                fold.mkdirs();
            }
            File file = new File(fileUploadPath + filename);
            // 将文件存储到磁盘
            uploadFile.transferTo(file);
        }

        // 将文件数据保存到数据库
        HrDocs docs = new HrDocs();
        docs.setStoreId(storeId);
        docs.setName(filename);
        docs.setStaffId(staffId);
        docs.setType(extName);
        docs.setOldName(originalFilename);
        docs.setMd5(md5);
        docs.setSize(uploadFile.getSize() / 1024); // KB

        HrDocs saved = hrDocsRepository.save(docs);
        return Result.success(saved);
    }

    /**
     * 文件下载
     * 对应参考系统 download(String filename, HttpServletResponse response)
     */
    public Result<String> download(String filename, HttpServletResponse response) throws Exception {
        // 通知浏览器以下载的方式打开
        response.addHeader("Content-Type", "application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));

        File downloadFile = new File(fileUploadPath + filename);
        if (!downloadFile.exists()) {
            return Result.error(500, "文件不存在");
        }

        OutputStream out = response.getOutputStream();
        try (FileInputStream fis = new FileInputStream(downloadFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.flush();
        out.close();
        return Result.success();
    }

    // ==================== 基础 CRUD ====================

    public Result<HrDocs> add(HrDocs docs) {
        HrDocs saved = hrDocsRepository.save(docs);
        return Result.success(saved);
    }

    public Result<String> deleteById(Integer id) {
        Optional<HrDocs> opt = hrDocsRepository.findById(id);
        if (opt.isPresent()) {
            HrDocs docs = opt.get();
            docs.setIsDeleted(1);
            hrDocsRepository.save(docs);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Optional<HrDocs> opt = hrDocsRepository.findById(id);
            if (opt.isPresent()) {
                HrDocs docs = opt.get();
                docs.setIsDeleted(1);
                hrDocsRepository.save(docs);
            }
        }
        return Result.success();
    }

    public Result<HrDocs> edit(HrDocs docs) {
        if (docs.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        HrDocs saved = hrDocsRepository.save(docs);
        return Result.success(saved);
    }

    public Result<HrDocs> findById(Integer id) {
        Optional<HrDocs> opt = hrDocsRepository.findById(id);
        return opt.map(Result::success).orElseGet(() -> Result.error(500, "未找到"));
    }

    // ==================== 分页条件查询 ====================

    /**
     * 分页条件查询
     * 对应参考系统 list(current, size, oldName, staffName)
     * 查询文件信息并关联员工姓名
     */
    public Result<Map<String, Object>> list(Integer current, Integer size, String oldName, String staffName) {
        if (oldName == null) {
            oldName = "";
        }
        if (staffName == null) {
            staffName = "";
        }

        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM hr_docs d LEFT JOIN hr_staff s ON d.staff_id = s.id AND s.is_deleted = 0 " +
                        "WHERE d.is_deleted = 0");
        StringBuilder dataSql = new StringBuilder(
                "SELECT d.*, s.name staff_name FROM hr_docs d LEFT JOIN hr_staff s ON d.staff_id = s.id AND s.is_deleted = 0 " +
                        "WHERE d.is_deleted = 0");

        List<Object> params = new ArrayList<>();

        if (!oldName.isEmpty()) {
            countSql.append(" AND d.old_name LIKE ?");
            dataSql.append(" AND d.old_name LIKE ?");
            params.add("%" + oldName + "%");
        }
        if (!staffName.isEmpty()) {
            countSql.append(" AND s.name LIKE ?");
            dataSql.append(" AND s.name LIKE ?");
            params.add("%" + staffName + "%");
        }

        int total = jdbc.queryForObject(countSql.toString(), Integer.class, params.toArray());

        int offset = (current - 1) * size;
        dataSql.append(" ORDER BY d.id DESC LIMIT ?, ?");
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(offset);
        dataParams.add(size);

        List<Map<String, Object>> list = jdbc.queryForList(dataSql.toString(), dataParams.toArray());

        int pages = (int) Math.ceil((double) total / size);
        Map<String, Object> map = new HashMap<>();
        map.put("pages", pages);
        map.put("total", total);
        map.put("list", list);
        return Result.success(map);
    }

    // ==================== 导入导出 ====================

    /**
     * 数据导出（CSV格式）
     * 对应参考系统 export(response)
     */
    public Result<String> export(HttpServletResponse response) throws java.io.IOException {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT d.*, s.name staff_name FROM hr_docs d " +
                        "LEFT JOIN hr_staff s ON d.staff_id = s.id AND s.is_deleted = 0 " +
                        "WHERE d.is_deleted = 0");

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"文件信息表.csv\"");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write('\ufeff');
        writer.println("文件名称,文件类型,原名称,MD5,大小(KB),上传者,备注,创建时间");

        for (Map<String, Object> item : list) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    nvl(item.get("name")),
                    nvl(item.get("type")),
                    nvl(item.get("old_name")),
                    nvl(item.get("md5")),
                    nvl(item.get("size")),
                    nvl(item.get("staff_name")),
                    nvl(item.get("remark")),
                    nvl(item.get("create_time")));
        }
        writer.flush();
        return Result.success();
    }

    /**
     * 数据导入（CSV格式）
     * 对应参考系统 imp(file)
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> imp(MultipartFile file) throws java.io.IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String header = reader.readLine();
        if (header == null) {
            return Result.error(500, "文件为空");
        }

        String line;
        int successCount = 0;
        int skipCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split(",");
            if (cols.length < 8) continue;

            try {
                HrDocs docs = new HrDocs();
                docs.setStoreId(1L);
                docs.setName(cols[0].trim().isEmpty() ? null : cols[0].trim());
                docs.setType(cols[1].trim().isEmpty() ? null : cols[1].trim());
                docs.setOldName(cols[2].trim().isEmpty() ? null : cols[2].trim());
                docs.setMd5(cols[3].trim().isEmpty() ? null : cols[3].trim());
                docs.setSize(cols[4].trim().isEmpty() ? null : Long.parseLong(cols[4].trim()));
                docs.setRemark(cols[6].trim().isEmpty() ? null : cols[6].trim());

                hrDocsRepository.save(docs);
                successCount++;
            } catch (Exception e) {
                skipCount++;
            }
        }
        reader.close();
        return Result.success("导入成功：" + successCount + "条，跳过：" + skipCount + "条");
    }

    // ==================== MD5 工具方法 ====================

    private String md5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            return toHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toHex(byte[] bytes) {
        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            ret.append(HEX_DIGITS[(aByte >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[aByte & 0x0f]);
        }
        return ret.toString();
    }

    // ==================== 工具方法 ====================

    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}