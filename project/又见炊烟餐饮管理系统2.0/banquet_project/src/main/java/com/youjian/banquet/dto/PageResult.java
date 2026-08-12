package com.youjian.banquet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Long total;
    private List<T> records;

    public PageResult(Page<T> page) {
        this.total = page.getTotalElements();
        this.records = page.getContent();
    }
}
