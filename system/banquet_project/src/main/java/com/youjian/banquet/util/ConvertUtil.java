package com.youjian.banquet.util;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertUtil {

    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败: " + e.getMessage(), e);
        }
    }

    public static <S, T> List<T> convertList(List<S> sources, Class<T> targetClass) {
        if (sources == null || sources.isEmpty()) return Collections.emptyList();
        return sources.stream()
                .map(s -> convert(s, targetClass))
                .collect(Collectors.toList());
    }

    public static <S, T> Page<T> convertPage(Page<S> sourcePage, Class<T> targetClass, Pageable pageable) {
        List<T> convertedList = convertList(sourcePage.getContent(), targetClass);
        return new PageImpl<>(convertedList, pageable, sourcePage.getTotalElements());
    }
}
