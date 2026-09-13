// ScriptUtilsHarness.java — 验证 Spring ScriptUtils 能否执行迁移 SQL（定向，仅隔离库）
// 用法: java ScriptUtilsHarness <jdbcUrl> <user> <password> <scriptPath> [separator]
//   jdbcUrl 例: jdbc:mysql://127.0.0.1:13317/<schema>?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8
//   连接信息(host/port/user/password)由调用方在命令行运行时传入，本文件不硬编码任何凭据。
// 行为: 用 ScriptUtils.executeSqlScript 执行脚本，separator 缺省 ";"；成功打印 SCRIPT_OK。
import java.sql.Connection;
import java.sql.DriverManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;

public class ScriptUtilsHarness {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: ScriptUtilsHarness <jdbcUrl> <user> <password> <scriptPath> [separator]");
            System.exit(2);
        }
        String url = args[0];
        String user = args[1];
        String pass = args[2];
        String script = args[3];
        String separator = args.length >= 5 ? args[4] : ";";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, pass);
            ScriptUtils.executeSqlScript(
                conn,
                new EncodedResource(new FileSystemResource(script)),
                false, false,
                ScriptUtils.DEFAULT_COMMENT_PREFIX,
                separator,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            System.out.println("SCRIPT_OK separator=" + separator);
        } catch (Throwable t) {
            System.out.println("SCRIPT_FAIL separator=" + separator);
            Throwable cur = t;
            int depth = 0;
            while (cur != null && depth < 6) {
                String msg = cur.getMessage();
                if (msg != null) {
                    System.out.println("ERR" + depth + "[" + cur.getClass().getSimpleName() + "]: " + msg);
                }
                cur = cur.getCause();
                depth++;
            }
            System.exit(1);
        } finally {
            if (conn != null) { try { conn.close(); } catch (Exception ignore) {} }
        }
    }
}
