package com.youjian.banquet.service;

import com.youjian.banquet.entity.BtToken;
import com.youjian.banquet.repository.BtTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Token Service
 * 来源：点餐系统 token Service
 */
@Service
public class BtTokenService {

    @Autowired
    private BtTokenRepository btTokenRepo;

    /**
     * 生成token
     */
    @Transactional
    public String generateToken(Long userid, String username, String tableName, String role) {
        Optional<BtToken> existingToken = btTokenRepo.findByUseridAndRole(userid, role);
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiratedTime = LocalDateTime.now().plusHours(1);

        if (existingToken.isPresent()) {
            BtToken tokenEntity = existingToken.get();
            tokenEntity.setToken(token);
            tokenEntity.setExpiratedtime(expiratedTime);
            btTokenRepo.save(tokenEntity);
        } else {
            btTokenRepo.save(new BtToken(userid, username, tableName, role, token, expiratedTime));
        }
        return token;
    }

    /**
     * 获取token实体
     */
    public BtToken getTokenEntity(String token) {
        Optional<BtToken> tokenOpt = btTokenRepo.findByToken(token);
        if (tokenOpt.isPresent()) {
            BtToken tokenEntity = tokenOpt.get();
            if (tokenEntity.getExpiratedtime().isAfter(LocalDateTime.now())) {
                return tokenEntity;
            }
        }
        return null;
    }
}