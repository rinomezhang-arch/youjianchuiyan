package com.youjian.banquet.config;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 配置。
 * <p>
 * 1. 兼容 ISO 标准格式与常见 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd HH:mm" 反序列化（保留原有能力）。
 * 2. 多端命名策略：通过请求头 {@code X-Client-Type} 区分客户端
 *    - PC 端（默认/未指定）：驼峰命名 camelCase
 *    - iPad 端（X-Client-Type: ipad）：下划线命名 snake_case
 *    实现方式：注册自定义 {@link ClientAwareJacksonConverter}，在响应写入时按当前请求头部
 *    线程安全地选择对应 ObjectMapper，避免并发下共享状态污染。
 */
@Configuration
public class JacksonConfig {

    /** 标识 iPad 客户端的请求头值 */
    public static final String IPAD_CLIENT_TYPE = "ipad";

    /** 多端区分请求头名称 */
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    private static final DateTimeFormatter[] DATETIME_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    };

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private static final DateTimeFormatter[] TIME_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
    };

    @Bean
    public SimpleModule customJavaTimeModule() {
        SimpleModule module = new SimpleModule("CustomJavaTime");
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) return null;
                text = text.trim();
                if (text.contains("T")) {
                    try { return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME); } catch (Exception ignore) {}
                }
                for (DateTimeFormatter f : DATETIME_FORMATS) {
                    try { return LocalDateTime.parse(text, f); } catch (Exception ignore) {}
                }
                for (DateTimeFormatter f : DATE_FORMATS) {
                    try { return LocalDate.parse(text, f).atStartOfDay(); } catch (Exception ignore) {}
                }
                throw new IOException("无法解析 LocalDateTime: " + text);
            }
        });
        module.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) return null;
                text = text.trim();
                for (DateTimeFormatter f : DATE_FORMATS) {
                    try { return LocalDate.parse(text, f); } catch (Exception ignore) {}
                }
                for (DateTimeFormatter f : DATETIME_FORMATS) {
                    try { return LocalDateTime.parse(text, f).toLocalDate(); } catch (Exception ignore) {}
                }
                throw new IOException("无法解析 LocalDate: " + text);
            }
        });
        module.addDeserializer(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) return null;
                text = text.trim();
                for (DateTimeFormatter f : TIME_FORMATS) {
                    try { return LocalTime.parse(text, f); } catch (Exception ignore) {}
                }
                throw new IOException("无法解析 LocalTime: " + text);
            }
        });
        return module;
    }

    /**
     * 注册客户端感知的 Jackson 消息转换器。
     * <p>
     * 使用 {@link HttpMessageConverters} 的追加模式（不替换默认转换器集合），
     * 将自定义转换器置于列表最前，使 JSON 响应优先由它处理。
     *
     * @param pcMapper Spring 自动装配的主 ObjectMapper（已含 customJavaTimeModule，驼峰命名）
     */
    @Bean
    public HttpMessageConverters clientAwareMessageConverters(@Autowired ObjectMapper pcMapper) {
        // iPad Mapper：复制主 ObjectMapper（保留时间模块等所有配置），切换为下划线命名
        ObjectMapper ipadMapper = pcMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        ClientAwareJacksonConverter converter = new ClientAwareJacksonConverter(pcMapper, ipadMapper);
        return new HttpMessageConverters(converter);
    }

    /**
     * 按请求头 {@code X-Client-Type} 线程安全地选择 ObjectMapper 的 JSON 转换器。
     * <p>
     * 不能通过 {@code setObjectMapper} 切换（并发不安全），而是在 {@link #writeInternal}
     * 中使用局部变量引用选中的 ObjectMapper 直接写入，保证多请求并发互不干扰。
     */
    public static class ClientAwareJacksonConverter extends MappingJackson2HttpMessageConverter {

        private final ObjectMapper pcMapper;
        private final ObjectMapper ipadMapper;

        public ClientAwareJacksonConverter(ObjectMapper pcMapper, ObjectMapper ipadMapper) {
            super(pcMapper);
            this.pcMapper = pcMapper;
            this.ipadMapper = ipadMapper;
        }

        @Override
        protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {
            ObjectMapper mapper = selectMapper();
            HttpHeaders headers = outputMessage.getHeaders();
            if (headers.getContentType() == null) {
                MediaType contentType = getDefaultContentType(object);
                if (contentType != null) {
                    headers.setContentType(contentType);
                }
            }
            JsonEncoding encoding = getJsonEncoding(headers.getContentType());
            OutputStream out = outputMessage.getBody();
            Writer writer = new OutputStreamWriter(out, encoding.getJavaName());
            mapper.writer()
                    .without(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .writeValue(writer, object);
            writer.flush();
        }

        /**
         * 根据当前请求的 X-Client-Type 头选择 ObjectMapper。
         * 无法获取请求上下文（如异步任务）时回退为 PC 端驼峰命名。
         */
        private ObjectMapper selectMapper() {
            try {
                RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
                if (attrs instanceof ServletRequestAttributes sra) {
                    HttpServletRequest request = sra.getRequest();
                    String clientType = request.getHeader(CLIENT_TYPE_HEADER);
                    if (IPAD_CLIENT_TYPE.equalsIgnoreCase(clientType)) {
                        return ipadMapper;
                    }
                }
            } catch (Exception ignored) {
                // 回退到默认 PC Mapper
            }
            return pcMapper;
        }
    }
}
