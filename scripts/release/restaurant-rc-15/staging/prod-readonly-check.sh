set -e
R=/home/ubuntu/deploy_tmp_main/banquet_project
echo '---resources---'
ls "$R/src/main/resources/" | grep -iE 'ipad|migration' || echo '(no ipad/migration resources)'
echo '---jar classes---'
unzip -l "$R/target/banquet-1.0.0.jar" | grep -E 'IpadBatch(Authorization|Submission)|StaffRealtimeGuard' || echo '(none in jar)'
echo '---db---'
source ~/.banquet_env.sh >/dev/null 2>&1
export MYSQL_PWD="$MYSQL_PASSWORD"
mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "
SELECT CONCAT('table:', TABLE_NAME) FROM information_schema.tables
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('ipad_device_binding','ipad_batch_request');
SELECT CONCAT('pkcol:', TABLE_NAME, '.', COLUMN_NAME) FROM information_schema.columns
 WHERE TABLE_SCHEMA=DATABASE()
   AND ((TABLE_NAME='staff_master' AND COLUMN_NAME IN ('staff_id','id'))
     OR (TABLE_NAME='store_info' AND COLUMN_NAME IN ('store_id','id')));"
