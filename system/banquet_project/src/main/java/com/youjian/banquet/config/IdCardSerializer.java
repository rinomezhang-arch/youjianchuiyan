package com.youjian.banquet.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.youjian.banquet.util.DataMaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 身份证号脱敏 Jackson 序列化器
 */
@Component
public class IdCardSerializer extends JsonSerializer<String> {

    private static DataMaskUtil dataMaskUtil;

    @Autowired
    public void setDataMaskUtil(DataMaskUtil dataMaskUtil) {
        IdCardSerializer.dataMaskUtil = dataMaskUtil;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (dataMaskUtil != null) {
            gen.writeString(dataMaskUtil.maskIdCard(value));
        } else {
            gen.writeString("****");
        }
    }
}
