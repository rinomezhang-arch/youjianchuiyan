-- ======================================================================
-- 加宽 staff_master/supplier_master/finance_account/store_info 的
-- bank_account/id_card 列 迁移 v1
-- 生成时间: 2026-08-27
-- 说明: DataEncryptionInitializer(启动时自动加密明文银行账号/身份证)
--       每次启动都会尝试加密这几列，但列宽只有 varchar(20)/varchar(30)，
--       AES-GCM 加密后的密文(IV+密文+tag，base64编码，带"ENC:"前缀)远超
--       原长度，UPDATE 直接报 "Data too long"，加密从来没有真正成功过——
--       员工银行账号/身份证号一直以明文形式留在生产库里。
--       这里只加宽列宽(VARCHAR 加宽不会丢数据)，不做其它改动；加宽后
--       下次后端启动，DataEncryptionInitializer 会自动把现存明文加密。
-- 幂等: MODIFY COLUMN 可重复执行
-- ======================================================================

ALTER TABLE `staff_master`
  MODIFY COLUMN `bank_account` VARCHAR(255) NULL,
  MODIFY COLUMN `id_card` VARCHAR(255) NULL;

ALTER TABLE `supplier_master`
  MODIFY COLUMN `bank_account` VARCHAR(255) NULL;

ALTER TABLE `finance_account`
  MODIFY COLUMN `bank_account` VARCHAR(255) NULL;

ALTER TABLE `store_info`
  MODIFY COLUMN `bank_account` VARCHAR(255) NULL;
