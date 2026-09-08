#!/bin/bash
# RC15 read-only schema evidence: no writes.
source ~/.banquet_env.sh >/dev/null 2>&1
export MYSQL_PWD="$MYSQL_PASSWORD"
M="mysql -u$MYSQL_USER $MYSQL_DATABASE -N"
echo "DB=$MYSQL_DATABASE"
echo -n "dish_recipe.is_active: ";   $M -e "SHOW COLUMNS FROM dish_recipe LIKE 'is_active';" | wc -l
echo -n "dish_recipe.revision_id: "; $M -e "SHOW COLUMNS FROM dish_recipe LIKE 'revision_id';" | wc -l
for t in recipe_revision ipad_batch_request restaurant_print_config month_salary; do
  echo -n "table.$t: "; $M -e "SHOW TABLES LIKE '$t';" | wc -l
done
