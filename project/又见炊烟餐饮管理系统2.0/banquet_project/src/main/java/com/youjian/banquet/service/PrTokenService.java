package com.youjian.banquet.service;

import com.youjian.banquet.entity.PrToken;
import com.youjian.banquet.repository.PrTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrTokenService {

    @Autowired
    private PrTokenRepository repository;

    private static final SecureRandom RANDOM = new SecureRandom();

    public Page<PrToken> queryPage(Map<String, Object> params, PrToken entity) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "10").toString());
        Specification<PrToken> spec = (root, query, cb) -> cb.conjunction();
        return repository.findAll(spec, PageRequest.of(page - 1, limit, Sort.by("id").descending()));
    }

    public Optional<PrToken> selectById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public PrToken insert(PrToken entity) {
        return repository.save(entity);
    }

    @Transactional
    public PrToken updateById(PrToken entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteBatchIds(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    public String generateToken(Long userid, String username, String tableName, String role) {
        Optional<PrToken> existing = repository.findByUseridAndRole(userid, role);
        String token = generateRandomString(32);
        LocalDateTime expiratedTime = LocalDateTime.now().plusHours(1);

        if (existing.isPresent()) {
            PrToken tokenEntity = existing.get();
            tokenEntity.setToken(token);
            tokenEntity.setExpiratedtime(expiratedTime);
            repository.save(tokenEntity);
        } else {
            PrToken tokenEntity = new PrToken(userid, username, tableName, role, token, expiratedTime);
            repository.save(tokenEntity);
        }
        return token;
    }

    public PrToken getTokenEntity(String token) {
        Optional<PrToken> tokenEntity = repository.findByToken(token);
        if (tokenEntity.isEmpty() || tokenEntity.get().getExpiratedtime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return tokenEntity.get();
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}