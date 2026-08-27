-- 宴会预订：客人扫码/点链接自助确认。之前"发预定信息-客人确认回执"这一步完全没有
-- 落地机制，只能靠电话/微信口头确认，没有留痕。这里给 booking_master 加三个字段：
-- confirm_token 用于生成免登录的公开确认链接，guest_confirmed/guest_confirm_time
-- 记录客人真实点击确认的时间，作为回执存档。

ALTER TABLE `booking_master`
  ADD COLUMN `confirm_token` varchar(64) DEFAULT NULL AFTER `remark`,
  ADD COLUMN `guest_confirmed` tinyint NOT NULL DEFAULT 0 AFTER `confirm_token`,
  ADD COLUMN `guest_confirm_time` timestamp NULL DEFAULT NULL AFTER `guest_confirmed`;

ALTER TABLE `booking_master`
  ADD UNIQUE KEY `uk_booking_confirm_token` (`confirm_token`);
