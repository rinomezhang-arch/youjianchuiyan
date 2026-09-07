package com.youjian.banquet.service;

import com.youjian.banquet.util.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;
import java.util.*;

/** Persists configuration intent only; never contacts a printer or reports device success. */
@Service
public class RestaurantPrintService {
    private final JdbcTemplate jdbc;
    public RestaurantPrintService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public static class Rejected extends RuntimeException {
        public final int status;
        public Rejected(int status,String message){super(message);this.status=status;}
    }
    private static Rejected invalid(){return new Rejected(400,"打印配置字段不合法");}
    private Long scope(Object requested) {
        if(UserContext.get()==null || UserContext.getStaffId()==null) throw new Rejected(403,"缺少身份或门店权限");
        Long own=UserContext.currentStoreId();
        if(!UserContext.isGeneralManager() && (own==null || own<=0)) throw new Rejected(403,"缺少门店权限");
        Long sid=requested==null?null:positive(requested);
        if(UserContext.isGeneralManager()) {if(sid==null)throw new Rejected(400,"总经理必须显式指定有效storeId");return existingStore(sid);}
        if(sid!=null && !sid.equals(own)) throw new Rejected(403,"仅可操作本店打印配置");
        return existingStore(own);
    }
    private Long existingStore(Long id) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM store_info WHERE store_id=?",Integer.class,id)!=1)
            throw new Rejected(400,"门店不存在");
        return id;
    }
    private static long positive(Object v) {
        try {if(v==null || !v.toString().matches("[0-9]+"))throw invalid();long n=Long.parseLong(v.toString());if(n<=0)throw invalid();return n;}
        catch(NumberFormatException e){throw invalid();}
    }
    private static String text(Object v,int max,boolean empty) {
        if(!(v instanceof String))throw invalid();String s=((String)v).trim();if(s.length()>max || (!empty&&s.isEmpty()))throw invalid();return s;
    }
    private static boolean bool(Object v) {if(!(v instanceof Boolean))throw invalid();return (Boolean)v;}
    private static Object value(Map<String,Object>b,String key,Map<String,Object>old,String column,Object fallback){return b.containsKey(key)?b.get(key):old==null?fallback:old.get(column);}
    private static boolean dbBool(Object v){return v instanceof Boolean?(Boolean)v:((Number)v).intValue()!=0;}
    private static void fields(Map<String,Object>b,Set<String>allowed){if(b==null||b.keySet().stream().anyMatch(k->!allowed.contains(k))||b.values().stream().anyMatch(Objects::isNull))throw invalid();}
    private Map<String,Object> locked(String table,long id,long store) {
        var rows=jdbc.queryForList("SELECT * FROM "+table+" WHERE id=? AND store_id=? FOR UPDATE",id,store);
        if(rows.isEmpty())throw new Rejected(403,"记录不存在或不属于本店");return rows.get(0);
    }
    private Map<String,Object> out(Map<String,Object> r,boolean printer) {
        Map<String,Object>d=new LinkedHashMap<>();d.put("id",r.get("id"));d.put("storeId",r.get("store_id"));d.put("name",r.get("name"));
        d.put("archive",dbBool(r.get("archived")));d.put("createdAt",r.get("created_at").toString());d.put("updatedAt",r.get("updated_at").toString());
        if(printer){d.put("type",r.get("type"));d.put("paperWidth",r.get("paper_width"));d.put("copies",r.get("copies"));d.put("address",r.get("address"));d.put("connectionStatus","unverified");}
        else {d.put("printerId",r.get("printer_id"));d.put("documentType",r.get("document_type"));d.put("configuredEnabled",dbBool(r.get("configured_enabled")));d.put("effectiveEnabled",false);d.put("inactiveReason","device adapter not connected");}
        return d;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> printers(String storeId){long s=scope(storeId);return jdbc.queryForList("SELECT * FROM restaurant_print_printer WHERE store_id=? ORDER BY id",s).stream().map(r->out(r,true)).toList();}
    @Transactional(readOnly=true)
    public List<Map<String,Object>> rules(String storeId){long s=scope(storeId);return jdbc.queryForList("SELECT * FROM restaurant_print_rule WHERE store_id=? ORDER BY id",s).stream().map(r->out(r,false)).toList();}
    private long insert(String sql,Object...args){var key=new GeneratedKeyHolder();jdbc.update(c->{var ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);return ps;},key);return Objects.requireNonNull(key.getKey()).longValue();}
    @Transactional
    public Map<String,Object> savePrinter(Map<String,Object>b) {
        fields(b,Set.of("id","storeId","name","type","paperWidth","copies","address","archive"));long s=scope(b.get("storeId"));
        Long id=b.containsKey("id")?positive(b.get("id")):null;Map<String,Object>old=id==null?null:locked("restaurant_print_printer",id,s);
        if(old!=null&&dbBool(old.get("archived")))throw new Rejected(409,"已归档记录不可修改");
        String name=text(value(b,"name",old,"name",null),80,false),type=text(value(b,"type",old,"type",null),20,false),paper=text(value(b,"paperWidth",old,"paper_width",null),2,false);
        if(!Set.of("network","usb","bluetooth","browser").contains(type)||!Set.of("58","80","A4").contains(paper))throw invalid();
        long copies=positive(value(b,"copies",old,"copies",1));if(copies>5)throw invalid();
        String address=text(value(b,"address",old,"address",""),255,true);boolean archive=b.containsKey("archive")?bool(b.get("archive")):false;
        if(archive&&id==null)throw invalid();
        if(archive&&jdbc.queryForObject("SELECT COUNT(*) FROM restaurant_print_rule WHERE printer_id=? AND store_id=? AND archived=FALSE",Integer.class,id,s)>0)throw new Rejected(409,"请先归档引用此打印机的规则");
        if(id==null)id=insert("INSERT INTO restaurant_print_printer(store_id,name,type,paper_width,copies,address,archived) VALUES(?,?,?,?,?,?,?)",s,name,type,paper,copies,address,archive);
        else jdbc.update("UPDATE restaurant_print_printer SET name=?,type=?,paper_width=?,copies=?,address=?,archived=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND store_id=?",name,type,paper,copies,address,archive,id,s);
        return out(locked("restaurant_print_printer",id,s),true);
    }
    @Transactional
    public Map<String,Object> saveRule(Map<String,Object>b) {
        fields(b,Set.of("id","storeId","name","printerId","documentType","configuredEnabled","archive"));long s=scope(b.get("storeId"));
        Long id=b.containsKey("id")?positive(b.get("id")):null;Map<String,Object>old=id==null?null:locked("restaurant_print_rule",id,s);
        if(old!=null&&dbBool(old.get("archived")))throw new Rejected(409,"已归档记录不可修改");
        String name=text(value(b,"name",old,"name",null),80,false),kind=text(value(b,"documentType",old,"document_type",null),30,false);
        if(!Set.of("receipt","kitchen","refund","daily_report").contains(kind))throw invalid();
        long printer=positive(value(b,"printerId",old,"printer_id",null));boolean archive=b.containsKey("archive")?bool(b.get("archive")):false;
        boolean enabled=b.containsKey("configuredEnabled")?bool(b.get("configuredEnabled")):old!=null&&dbBool(old.get("configured_enabled"));
        if(archive&&id==null)throw invalid();
        Map<String,Object>p=locked("restaurant_print_printer",printer,s);
        if(dbBool(p.get("archived"))&&!(archive&&old!=null&&((Number)old.get("printer_id")).longValue()==printer))throw new Rejected(409,"规则只能引用未归档打印机");
        if(id==null)id=insert("INSERT INTO restaurant_print_rule(store_id,name,printer_id,document_type,configured_enabled,archived) VALUES(?,?,?,?,?,?)",s,name,printer,kind,enabled,archive);
        else jdbc.update("UPDATE restaurant_print_rule SET name=?,printer_id=?,document_type=?,configured_enabled=?,archived=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND store_id=?",name,printer,kind,enabled,archive,id,s);
        return out(locked("restaurant_print_rule",id,s),false);
    }
}
