package com.youjian.banquet.controller;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.*;
import com.youjian.banquet.util.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Real local MySQL and transaction proxies; synthetic records only, no production or legal writes. */
@EnabledIfEnvironmentVariable(named="YOUJIAN_TEST_MYSQL", matches="1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SupplyCostMysqlIntegrationTest {
    JdbcTemplate jdbc;
    KitchenSupplyService supply;
    InventoryService inventory;
    RecipeRevisionService revisions;
    LocalContainerEntityManagerFactoryBean factory;
    JpaRepositoryFactory repositories;
    int sequence=1;

    @BeforeAll void start() {
        String root="jdbc:mysql://127.0.0.1:13317/", opts="?useSSL=false&allowPublicKeyRetrieval=true";
        String schema="supply_cost_"+UUID.randomUUID().toString().replace("-","");
        new JdbcTemplate(new DriverManagerDataSource(root+opts,"root","")).execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");
        var ds=new DriverManagerDataSource(root+schema+opts,"root",""); jdbc=new JdbcTemplate(ds);
        // Reproduce real widths, defaults, foreign keys and unique keys; ORM-created tables hid a release bug.
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.ClassPathResource("restaurant-production-schema-20260906.sql"),
            new org.springframework.core.io.FileSystemResource("../scripts/migrations/preprocessing_requisition_link_v1.sql"),
            new org.springframework.core.io.FileSystemResource("../scripts/migrations/supply_price_precision_v1.sql"),
            new org.springframework.core.io.ClassPathResource("payable-metadata-fixture-20260907.sql"),
            new org.springframework.core.io.FileSystemResource("../scripts/migrations/receipt_payable_source_v1.sql"),
            new org.springframework.core.io.FileSystemResource("../scripts/migrations/recipe_revision_v1.sql")
        ).execute(ds);
        factory=new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(ds);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(GoodsReceipt.class.getName(),GoodsReceiptItem.class.getName(),
            IngredientMaster.class.getName(),IngredientInventoryLog.class.getName(),DishMaster.class.getName(),
            DishRecipe.class.getName(),RecipeRevision.class.getName(),CostCard.class.getName(),PreprocessingRecord.class.getName(),
            StoreInfo.class.getName(),SupplierMaster.class.getName(),MaterialRequisition.class.getName(),MaterialRequisitionItem.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate","hibernate.hbm2ddl.halt_on_error","true"));
        factory.afterPropertiesSet();
        jdbc.update("INSERT INTO store_info(store_id,store_code,store_name) VALUES(1,'SYN-1','合成验收门店一'),(2,'SYN-2','合成验收门店二')");
        jdbc.update("INSERT INTO supplier_master(supplier_id,store_id,supplier_name,is_active) VALUES(1,1,'合成验收供应商',1)");
        var repos=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        repositories=repos;
        var manager=new JpaTransactionManager(factory.getObject());
        InventoryService target=new InventoryService();
        ReflectionTestUtils.setField(target,"ingredientMasterRepository",repos.getRepository(IngredientMasterRepository.class));
        ReflectionTestUtils.setField(target,"inventoryLogRepository",repos.getRepository(IngredientInventoryLogRepository.class));
        inventory=proxy(target,manager);
        KitchenSupplyService kitchen=new KitchenSupplyService(mock(PurchaseRequestRepository.class),mock(PurchaseRequestItemRepository.class),
            repos.getRepository(GoodsReceiptRepository.class),repos.getRepository(GoodsReceiptItemRepository.class),
            repos.getRepository(MaterialRequisitionRepository.class),repos.getRepository(PreprocessingRecordRepository.class),
            repos.getRepository(CostCardRepository.class),mock(UnitConversionRepository.class),repos.getRepository(DishMasterRepository.class),
            repos.getRepository(DishRecipeRepository.class),repos.getRepository(IngredientMasterRepository.class),
            repos.getRepository(IngredientInventoryLogRepository.class),inventory);
        ReflectionTestUtils.setField(kitchen,"jdbc",jdbc);
        ReflectionTestUtils.setField(kitchen,"requisitionItems",repos.getRepository(MaterialRequisitionItemRepository.class));
        supply=proxy(kitchen,manager);
        RecipeRevisionService revisionTarget=new RecipeRevisionService();
        ReflectionTestUtils.setField(revisionTarget,"recipeRepo",repos.getRepository(DishRecipeRepository.class));
        ReflectionTestUtils.setField(revisionTarget,"revisionRepo",repos.getRepository(RecipeRevisionRepository.class));
        ReflectionTestUtils.setField(revisionTarget,"ingredientRepo",repos.getRepository(IngredientMasterRepository.class));
        ReflectionTestUtils.setField(revisionTarget,"dishRepo",repos.getRepository(DishMasterRepository.class));
        ReflectionTestUtils.setField(revisionTarget,"jdbc",jdbc);
        revisions=proxy(revisionTarget,manager);
    }
    @SuppressWarnings("unchecked") private <T> T proxy(T bean,JpaTransactionManager manager) {
        ProxyFactory proxy=new ProxyFactory(bean);
        proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        return (T)proxy.getProxy();
    }
    @BeforeEach void identity(){UserContext.set(new UserContext.CurrentUser(1L,1L,"store_manager","合成验收员"));}
    @AfterEach void clear(){UserContext.clear();}
    @AfterEach void checkRelationships(){SupplyDataAssertions.verify(jdbc);}
    @AfterAll void close(){if(factory!=null)factory.destroy();}
    BigDecimal decimal(String value){return new BigDecimal(value);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    String ingredient(){
        String id="SYN"+sequence++;
        jdbc.update("INSERT INTO ingredient_master(ingredient_id,store_id,ingredient_name,purchase_unit,usage_unit,conversion_rate,yield_rate,current_stock,unit_price,avg_price,is_active) VALUES(?,1,'合成验收原料','斤','克',500,80,0,20,20,1)",id);
        return id;
    }
    GoodsReceipt receipt(String status){
        GoodsReceipt r=new GoodsReceipt();r.setStoreId(1L);r.setSupplierId(1);r.setStatus(status);r.setWarehouseKeeperName("合成验收员");return r;
    }
    GoodsReceiptItem item(String id,String qty,String price){
        GoodsReceiptItem i=new GoodsReceiptItem();i.setIngredientId(id);i.setIngredientName("合成验收原料");
        i.setUnit("斤");i.setActualQuantity(decimal(qty));i.setUnitPrice(decimal(price));i.setQualityStatus("QUALIFIED");return i;
    }
    BigDecimal stock(String id){return jdbc.queryForObject("SELECT current_stock FROM ingredient_master WHERE ingredient_id=? AND store_id=1",BigDecimal.class,id);}

    @Test void versionedRecipeThenActualReceiptRepricesOnlyCurrentLinesAndPreservesHistory(){
        String id=ingredient(),dish="VR"+sequence++;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'合成版本收货闭环菜',50,1)",dish);
        DishRecipe first=new DishRecipe();first.setIngredientId(id);first.setQuantity(decimal("100"));first.setUnit("克");
        RecipeRevision v1=revisions.saveNewVersion(dish,1L,List.of(first),"合成验收员",null);
        var historyBefore=jdbc.queryForList("SELECT recipe_id,quantity,unit_price,total_cost FROM dish_recipe WHERE revision_id=?",v1.getRevisionId());
        DishRecipe second=new DishRecipe();second.setIngredientId(id);second.setQuantity(decimal("200"));second.setUnit("克");
        RecipeRevision v2=revisions.saveNewVersion(dish,1L,List.of(second),"合成验收员",null);
        assertEquals(decimal("10.0000"),v2.getTotalCost());
        GoodsReceipt accepted=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"2","40")));
        assertEquals(decimal("2.000"),stock(id));
        assertEquals(decimal("20.00"),jdbc.queryForObject("SELECT cost_price FROM dish_master WHERE dish_id=? AND store_id=1",BigDecimal.class,dish));
        assertEquals(decimal("20.0000"),jdbc.queryForObject("SELECT standard_cost FROM dish_cost_card WHERE dish_id=? AND store_id=1",BigDecimal.class,dish));
        assertEquals(historyBefore,jdbc.queryForList("SELECT recipe_id,quantity,unit_price,total_cost FROM dish_recipe WHERE revision_id=?",v1.getRevisionId()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dish_recipe WHERE dish_id=? AND store_id=1",Integer.class,dish));
        assertEquals(1,revisions.activeRecipe(dish,1L).size());
        assertEquals(1,revisions.revisionItems(v1.getRevisionId()).size());
        UserContext.set(new UserContext.CurrentUser(2L,2L,"store_manager","合成他店验收员"));
        assertThrows(RecipeRevisionService.RecipeAccessDeniedException.class,()->revisions.revisionItems(v1.getRevisionId()));
        identity();
        assertEquals(decimal("80.00"),jdbc.queryForObject("SELECT pending_amount FROM finance_payable WHERE source_receipt_id=?",BigDecimal.class,accepted.getReceiptId()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dish_recipe r LEFT JOIN recipe_revision v ON r.revision_id=v.revision_id AND r.store_id=v.store_id AND r.dish_id=v.dish_id WHERE r.revision_id IS NOT NULL AND v.revision_id IS NULL",Integer.class));
    }

    @Test void receiptAmountsAreRecomputedAndStockFlowsFromItsDetails(){
        String id=ingredient();GoodsReceiptItem line=item(id,"2.50","20.00");line.setAmount(decimal("999"));
        GoodsReceipt r=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(line));
        assertEquals(decimal("50.00"),r.getTotalAmount());
        assertEquals(decimal("50.0000"),jdbc.queryForObject("SELECT amount FROM purchase_receipt_detail WHERE receipt_id=?",BigDecimal.class,r.getReceiptId()));
        assertEquals(decimal("2.500"),stock(id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=? AND source_id=? AND source_type='GOODS_RECEIPT'",Integer.class,id,r.getReceiptId().toString()));
        var payable=jdbc.queryForMap("SELECT * FROM finance_payable WHERE source_receipt_id=?",r.getReceiptId());
        assertEquals(decimal("50.00"),payable.get("total_amount"));
        assertEquals(decimal("0.00"),payable.get("paid_amount"));
        assertEquals(decimal("50.00"),payable.get("pending_amount"));
        assertEquals("unpaid",payable.get("status"));
        assertEquals(r.getReceiptNo(),payable.get("source_receipt_no"));
        assertNull(payable.get("purchase_id"),"Receipt order must not be linked to ingredient_purchase");
        assertNull(payable.get("due_date"),"Do not invent a supplier credit period");
    }
    @Test void emptyReceiptCannotClaimAnAcceptedDelivery(){
        int before=count("purchase_receipt");
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(receipt("ACCEPTED"),List.of()));
        assertEquals(before,count("purchase_receipt"));
    }

    @Test void retriedReceiptNumberCannotDuplicateInventory(){
        String id=ingredient(),number="SYN"+UUID.randomUUID().toString().replace("-","");
        var first=receipt("ACCEPTED");first.setReceiptNo(number);
        supply.createGoodsReceipt(first,List.of(item(id,"2","20")));
        var repeated=receipt("ACCEPTED");repeated.setReceiptNo(number);
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(repeated,List.of(item(id,"2","20"))));
        assertEquals(decimal("2.000"),stock(id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM purchase_receipt WHERE receipt_no=?",Integer.class,number));
    }
    @Test void missingIngredientCannotBecomeAnUntrackedReceipt(){
        int before=count("purchase_receipt");
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item("","1","20"))));
        assertEquals(before,count("purchase_receipt"));
    }
    @Test void laterLineFailureRollsBackReceiptDetailsStockAndLogs(){
        String id=ingredient();int headers=count("purchase_receipt"),details=count("purchase_receipt_detail"),logs=count("ingredient_inventory_log");
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"2","20"),item("ABSENT","1","20"))));
        assertEquals(headers,count("purchase_receipt"));assertEquals(details,count("purchase_receipt_detail"));assertEquals(logs,count("ingredient_inventory_log"));assertEquals(decimal("0.000"),stock(id));
    }
    @Test void pendingReceiptDoesNotIncreaseUsableStock(){
        String id=ingredient();GoodsReceipt r=supply.createGoodsReceipt(receipt("PENDING"),List.of(item(id,"2","20")));
        assertEquals(decimal("0.000"),stock(id));
        assertEquals(1,supply.getGoodsReceiptItems(r.getReceiptId()).size());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=?",Integer.class,r.getReceiptId()));
    }

    @Test void acceptanceIsIdempotentAndReadsTheSamePersistedDetail(){
        String id=ingredient();GoodsReceipt r=supply.createGoodsReceipt(receipt("PENDING"),List.of(item(id,"2.50","20")));
        assertEquals("ACCEPTED",supply.acceptGoodsReceipt(r.getReceiptId(),"合成验收员").getStatus());
        assertEquals("ACCEPTED",supply.acceptGoodsReceipt(r.getReceiptId(),"合成验收员").getStatus());
        assertEquals(decimal("2.500"),stock(id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        assertEquals(1,supply.getGoodsReceiptItems(r.getReceiptId()).size());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=?",Integer.class,r.getReceiptId()));
    }
    @Test void payableStorageFailureRollsBackAcceptanceStockAndLogs(){
        String id=ingredient();var receipt=supply.createGoodsReceipt(receipt("PENDING"),List.of(item(id,"2","20")));
        jdbc.execute("CREATE TRIGGER synthetic_payable_failure BEFORE INSERT ON finance_payable FOR EACH ROW BEGIN IF NEW.source_receipt_id = "
                +receipt.getReceiptId()+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic payable storage failure'; END IF; END");
        assertThrows(org.springframework.dao.DataAccessException.class,()->supply.acceptGoodsReceipt(receipt.getReceiptId(),"Synthetic"));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM purchase_receipt WHERE receipt_id=?",String.class,receipt.getReceiptId()));
        assertEquals(decimal("0.000"),stock(id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=?",Integer.class,receipt.getReceiptId()));
        assertEquals(1,supply.getGoodsReceiptItems(receipt.getReceiptId()).size());
    }
    @Test void payableSourceCannotDuplicateOrPointAcrossStores(){
        var receipt=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(ingredient(),"2","20")));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO finance_payable(store_id,payable_no,total_amount,source_receipt_id) VALUES(1,'SYN-DUP',1,?)",receipt.getReceiptId()));
        var pending=supply.createGoodsReceipt(receipt("PENDING"),List.of(item(ingredient(),"2","20")));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO finance_payable(store_id,payable_no,total_amount,source_receipt_id) VALUES(2,'SYN-CROSS',1,?)",pending.getReceiptId()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM finance_payable WHERE source_receipt_id=?",Integer.class,receipt.getReceiptId()));
    }
    @Test void costCardUsesPurchaseUnitConversionAndYieldAndPreservesSalePrice(){
        String id=ingredient(),dish="D"+id;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'合成成本验收菜',20,1)",dish);
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit,yield_rate) VALUES(1,?,?,100,'克',80)",dish,id);
        CostCard card=supply.calculateAndSaveCostCard(dish,1L);
        assertEquals(0,decimal("5.00").compareTo(card.getStandardCost()));
        assertEquals(0,decimal("75.00").compareTo(card.getGrossMargin()));
        assertEquals(decimal("20.00"),jdbc.queryForObject("SELECT sale_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
    }
    @Test void zeroRawWeightCannotProduceAValidYieldRecord(){
        PreprocessingRecord r=new PreprocessingRecord();r.setStoreId(1L);r.setIngredientId(ingredient());r.setRawQty(BigDecimal.ZERO);r.setProcessedQty(BigDecimal.ONE);r.setRecordDate(LocalDate.now());r.setUnit("斤");
        int before=count("preprocessing_record");
        assertThrows(IllegalArgumentException.class,()->supply.createPreprocessingRecord(r));
        assertEquals(before,count("preprocessing_record"));
    }

    @Test void latestReceiptPriceRecalculatesRecipeDishAndCostCardWithoutRewritingHistory(){
        String id=ingredient(),dish="D"+id;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'合成成本验收菜',20,1)",dish);
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit,yield_rate,wastage_rate) VALUES(1,?,?,100,'克',NULL,NULL)",dish,id);
        GoodsReceipt first=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"2","20")));
        GoodsReceipt second=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"1","24")));
        assertEquals(decimal("3.000"),stock(id));
        assertEquals(decimal("24.00000000"),jdbc.queryForObject("SELECT unit_price FROM ingredient_master WHERE ingredient_id=?",BigDecimal.class,id));
        assertEquals(decimal("6.00"),jdbc.queryForObject("SELECT cost_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(decimal("30.00"),jdbc.queryForObject("SELECT cost_rate FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(decimal("6.0000"),jdbc.queryForObject("SELECT standard_cost FROM dish_cost_card WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(decimal("40.00"),jdbc.queryForObject("SELECT total_amount FROM purchase_receipt WHERE receipt_id=?",BigDecimal.class,first.getReceiptId()));
        assertEquals(decimal("40.00"),jdbc.queryForObject("SELECT total_amount FROM ingredient_inventory_log WHERE source_id=? AND food_material_id=?",BigDecimal.class,first.getReceiptId().toString(),id));
        assertEquals(decimal("24.00"),jdbc.queryForObject("SELECT total_amount FROM ingredient_inventory_log WHERE source_id=? AND food_material_id=?",BigDecimal.class,second.getReceiptId().toString(),id));
    }

    @Test void missingUnitConversionRejectsCostInsteadOfProducingZeroOrInflatedCost(){
        String id=ingredient(),dish="D"+id;
        jdbc.update("UPDATE ingredient_master SET conversion_rate=NULL WHERE ingredient_id=?",id);
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'合成换算验收菜',20,1)",dish);
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit,yield_rate,wastage_rate) VALUES(1,?,?,100,'克',NULL,NULL)",dish,id);
        assertThrows(IllegalArgumentException.class,()->supply.calculateAndSaveCostCard(dish,1L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dish_cost_card WHERE dish_id=?",Integer.class,dish));
    }

    @Test void rejectedReceiptAndOtherStoresCannotChangeInventory(){
        String id=ingredient();GoodsReceiptItem bad=item(id,"1","20");bad.setQualityStatus("UNQUALIFIED");
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(bad)));
        GoodsReceipt other=receipt("ACCEPTED");other.setStoreId(2L);
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(other,List.of(item(id,"1","20"))));
        assertEquals(decimal("0.000"),stock(id));
    }

    @Test void receiptRequisitionProcessingYieldCostAndSaleFormOneTraceableFlow(){
        String id=ingredient(),dish="D"+id;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'加工闭环验收菜',20,1)",dish);
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit,yield_rate,wastage_rate) VALUES(1,?,?,100,'克',NULL,NULL)",dish,id);
        supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"10","20")));
        MaterialRequisition header=new MaterialRequisition();header.setStoreId(1L);header.setRequestedBy("合成厨师");
        MaterialRequisitionItem line=new MaterialRequisitionItem();line.setIngredientId(id);line.setQuantity(decimal("4"));line.setUnit("斤");
        MaterialRequisition requisition=supply.createRequisitionWithItems(header,List.of(line));
        assertEquals(decimal("10.000"),stock(id));
        supply.approveRequisition(requisition.getRequisitionId(),"合成仓管");
        supply.approveRequisition(requisition.getRequisitionId(),"合成仓管");
        assertEquals(decimal("6.000"),stock(id));
        var source=supply.getRequisitionItems(requisition.getRequisitionId()).get(0);
        PreprocessingRecord processed=new PreprocessingRecord();processed.setStoreId(1L);processed.setIngredientId(id);
        processed.setRequisitionItemId(source.getItemId());processed.setRawQty(decimal("4"));processed.setProcessedQty(decimal("3"));processed.setUnit("斤");processed.setOperator("合成厨师");
        PreprocessingRecord result=supply.createPreprocessingRecord(processed);
        assertEquals(decimal("75.00"),result.getYieldRate());
        assertEquals(decimal("6.000"),stock(id),"加工不可再次扣减已经领出的毛料");
        assertEquals(decimal("5.33"),jdbc.queryForObject("SELECT cost_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(decimal("26.65"),jdbc.queryForObject("SELECT cost_rate FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(decimal("20.00"),jdbc.queryForObject("SELECT sale_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE source_type='REQUISITION_ITEM' AND source_id=? AND total_amount=80",Integer.class,source.getItemId().toString()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM preprocessing_record p JOIN material_requisition_item i ON i.item_id=p.requisition_item_id AND i.store_id=p.store_id JOIN material_requisition r ON r.requisition_id=i.requisition_id AND r.store_id=i.store_id WHERE p.record_id=? AND r.status='APPROVED'",Integer.class,result.getRecordId()));
        PreprocessingRecord extra=new PreprocessingRecord();extra.setStoreId(1L);extra.setIngredientId(id);extra.setRequisitionItemId(source.getItemId());extra.setRawQty(decimal("1"));extra.setProcessedQty(decimal("0.8"));extra.setUnit("斤");
        assertThrows(IllegalArgumentException.class,()->supply.createPreprocessingRecord(extra));
    }

    @Test void requisitionWithInsufficientStockCannotLeavePartialIssueOrApproval(){
        String a=ingredient(),b=ingredient();
        supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(a,"3","20"),item(b,"1","20")));
        MaterialRequisition header=new MaterialRequisition();header.setStoreId(1L);
        var first=new MaterialRequisitionItem();first.setIngredientId(a);first.setQuantity(decimal("2"));
        var second=new MaterialRequisitionItem();second.setIngredientId(b);second.setQuantity(decimal("2"));
        var saved=supply.createRequisitionWithItems(header,List.of(first,second));
        int logs=count("ingredient_inventory_log");
        assertThrows(IllegalArgumentException.class,()->supply.approveRequisition(saved.getRequisitionId(),"合成仓管"));
        assertEquals(decimal("3.000"),stock(a));assertEquals(decimal("1.000"),stock(b));assertEquals(logs,count("ingredient_inventory_log"));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM material_requisition WHERE requisition_id=?",String.class,saved.getRequisitionId()));
    }

    @Test void legacyZeroYieldCannotSilentlyProduceSuccessfulReceiptOrCost(){
        String id=ingredient(),dish="D"+id;
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'默认零出成率验收菜',20,1)",dish);
        // Deliberately use the actual production default (zero), rather than weakening it in the fixture.
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit) VALUES(1,?,?,100,'克')",dish,id);
        int receipts=count("purchase_receipt"),logs=count("ingredient_inventory_log");
        assertThrows(IllegalArgumentException.class,()->supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"2","20"))));
        assertEquals(receipts,count("purchase_receipt"));assertEquals(logs,count("ingredient_inventory_log"));
        assertEquals(decimal("0.000"),stock(id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dish_cost_card WHERE dish_id=?",Integer.class,dish));
    }

    @Test void eightDecimalLatestReceiptPriceSurvivesRequisitionAndInventoryLedger(){
        String id=ingredient();BigDecimal price=decimal("12.34567891");
        var receipt=supply.createGoodsReceipt(receipt("ACCEPTED"),List.of(item(id,"3",price.toPlainString())));
        assertEquals(price,jdbc.queryForObject("SELECT unit_price FROM purchase_receipt_detail WHERE receipt_id=?",BigDecimal.class,receipt.getReceiptId()));
        assertEquals(price,jdbc.queryForObject("SELECT unit_price FROM ingredient_master WHERE ingredient_id=?",BigDecimal.class,id));
        var header=new MaterialRequisition();header.setStoreId(1L);
        var line=new MaterialRequisitionItem();line.setIngredientId(id);line.setQuantity(decimal("2"));
        var requisition=supply.createRequisitionWithItems(header,List.of(line));
        supply.approveRequisition(requisition.getRequisitionId(),"合成仓管");
        assertEquals(price,jdbc.queryForObject("SELECT unit_price FROM material_requisition_item WHERE requisition_id=?",BigDecimal.class,requisition.getRequisitionId()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=? AND unit_price=?",Integer.class,id,price));
        assertEquals(decimal("24.69"),jdbc.queryForObject("SELECT total_amount FROM material_requisition WHERE requisition_id=?",BigDecimal.class,requisition.getRequisitionId()));
        assertEquals(decimal("1.000"),stock(id));
    }

    @Test
    @EnabledIfEnvironmentVariable(named="YOUJIAN_BROWSER_ACCEPTANCE",matches="1")
    void browserReceiptSaveAcceptReloadAndCostPropagation() throws Exception {
        String id=ingredient(),dish="D"+id;
        jdbc.update("UPDATE ingredient_master SET ingredient_name='浏览器验收原料' WHERE ingredient_id=?",id);
        jdbc.update("INSERT INTO dish_master(dish_id,store_id,dish_name,sale_price,is_active) VALUES(?,1,'浏览器成本验收菜',20,1)",dish);
        jdbc.update("INSERT INTO dish_recipe(store_id,dish_id,ingredient_id,quantity,unit,yield_rate,wastage_rate) VALUES(1,?,?,100,'克',NULL,NULL)",dish,id);
        IngredientService ingredientService=new IngredientService();
        ReflectionTestUtils.setField(ingredientService,"ingredientMasterRepository",repositories.getRepository(IngredientMasterRepository.class));
        ReflectionTestUtils.setField(ingredientService,"supplierMasterRepository",repositories.getRepository(SupplierMasterRepository.class));
        IngredientController ingredients=new IngredientController();ReflectionTestUtils.setField(ingredients,"ingredientService",ingredientService);
        SupplierService supplierService=new SupplierService();ReflectionTestUtils.setField(supplierService,"supplierMasterRepository",repositories.getRepository(SupplierMasterRepository.class));
        SupplierController suppliers=new SupplierController();ReflectionTestUtils.setField(suppliers,"supplierService",supplierService);
        CostController costs=new CostController();ReflectionTestUtils.setField(costs,"jdbc",jdbc);
        RecipeController recipes=new RecipeController();ReflectionTestUtils.setField(recipes,"recipeRepo",repositories.getRepository(DishRecipeRepository.class));
        ReflectionTestUtils.setField(recipes,"dishRepo",repositories.getRepository(DishMasterRepository.class));ReflectionTestUtils.setField(recipes,"ingredientRepo",repositories.getRepository(IngredientMasterRepository.class));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new KitchenSupplyController(supply),ingredients,suppliers,costs,recipes)
            .setControllerAdvice(new com.youjian.banquet.exception.GlobalExceptionHandler()).build();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/",exchange->{
            identity();
            try {
                var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                    org.springframework.http.HttpMethod.valueOf(exchange.getRequestMethod()),exchange.getRequestURI())
                    .content(exchange.getRequestBody().readAllBytes()).contentType("application/json");
                var response=mvc.perform(request).andReturn().getResponse();
                byte[] bytes=response.getContentAsByteArray();exchange.getResponseHeaders().set("Content-Type","application/json;charset=UTF-8");
                exchange.sendResponseHeaders(response.getStatus(),bytes.length);exchange.getResponseBody().write(bytes);
            } catch(Exception e) {
                byte[] bytes="{\"code\":500,\"message\":\"Test HTTP bridge failed\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500,bytes.length);exchange.getResponseBody().write(bytes);
            } finally {UserContext.clear();exchange.close();}
        });
        server.start();
        java.nio.file.Path root=java.nio.file.Path.of("..").toAbsolutePath().normalize(),output=root.resolve("artifacts/production-readiness-20260905");
        Process runner=null;
        try {
            runner=new ProcessBuilder("C:/Users/rinom/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe",
                root.resolve("frontend_v3/acceptance/receipt-browser.mjs").toString(),Integer.toString(server.getAddress().getPort()),output.toString())
                .redirectErrorStream(true).redirectOutput(output.resolve("receipt-browser.log").toFile()).start();
            assertTrue(runner.waitFor(150,java.util.concurrent.TimeUnit.SECONDS),"Browser test timed out; inspect receipt-browser.log");
            assertEquals(0,runner.exitValue(),"Browser flow failed; inspect receipt-browser.log and failure screenshot");
            assertEquals(decimal("1.500"),stock(id));
            assertEquals(decimal("6.40"),jdbc.queryForObject("SELECT cost_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
            assertEquals(decimal("32.00"),jdbc.queryForObject("SELECT cost_rate FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
            assertEquals(decimal("20.00"),jdbc.queryForObject("SELECT sale_price FROM dish_master WHERE dish_id=?",BigDecimal.class,dish));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ingredient_inventory_log WHERE food_material_id=?",Integer.class,id));
        } finally {if(runner!=null && runner.isAlive())runner.destroyForcibly();server.stop(0);}
    }
}
