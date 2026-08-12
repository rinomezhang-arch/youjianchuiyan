package com.youjian.banquet.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;

@Data
public class PageParam {
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小最小为1")
    private Integer pageSize = 10;
}
