ALTER TABLE certificate_credential
  ADD COLUMN `trusted_ca` varchar(7000) DEFAULT NULL;
