package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.AgentCollabIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Statement;
import java.util.*;

/** Single-table coordination. COS code review is separate from authenticated human approval here. */
@RestController
@RequestMapping("/api/agent")
public class AgentCollabController {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AgentCollabIdentity identity;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Value("${agent.collab.heartbeat-timeout-minutes:240}") private int heartbeatTimeoutMinutes;
    private static final Set<String> AGENTS=Set.of("codex","claude","dilong","tianlong","trae");

    @PostMapping("/claim")
    public ResponseEntity<Result<?>> claim(@RequestBody Map<String,Object> body,HttpServletRequest req) {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // InnoDB duplicate-key gap locks can conflict with same-table audit inserts.
        // Retry the entire rolled-back transaction, never just its final statement.
        for (int attempt = 0; ; attempt++) {
            try { return transaction.execute(status -> claimTransaction(body, req)); }
            catch (org.springframework.dao.PessimisticLockingFailureException conflict) {
                if (attempt >= 2) throw conflict;
            }
        }
    }

    private ResponseEntity<Result<?>> claimTransaction(Map<String,Object> body,HttpServletRequest req) {
        String actor=actor(req,body),line=text(body,"line",60,true),title=text(body,"title",200,false),detail=text(body,"body",12000,false);
        // Atomic no-op upsert takes the unique row lock without duplicate-insert shared-lock upgrades.
        // Never modify the existing owner, heartbeat or request when another actor races.
        jdbc.update("INSERT INTO agent_collab(record_type,from_agent,line_name,title,body,status,heartbeat_at) " +
                "VALUES('claim',?,?,?,?,'open',NOW()) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                actor,line,title==null?line:title,detail);
        var rows=jdbc.queryForList("SELECT id,from_agent,title,heartbeat_at,UNIX_TIMESTAMP(heartbeat_at)*1000 heartbeatEpochMillis, " +
                "(heartbeat_at IS NULL OR heartbeat_at<=DATE_SUB(NOW(),INTERVAL ? MINUTE)) stale FROM agent_collab " +
                "WHERE record_type='claim' AND status='open' AND line_name=? FOR UPDATE",timeout(),line);
        if(rows.size()!=1)throw new IllegalStateException("Active claim constraint unavailable");
        var holder=rows.get(0);
        if(!actor.equals(holder.get("from_agent"))) {
            var conflict=new LinkedHashMap<String,Object>();conflict.put("heldBy",holder.get("from_agent"));
            conflict.put("claimId",holder.get("id"));conflict.put("heartbeatAt",holder.get("heartbeat_at"));
            conflict.put("heartbeatEpochMillis",holder.get("heartbeatEpochMillis"));conflict.put("stale",holder.get("stale"));
            conflict.put("title",holder.get("title"));
            return response(409,"该任务线已有占用者；超时仅标记，请先协商",conflict);
        }
        Long id=((Number)holder.get("id")).longValue();
        boolean renewed=jdbc.queryForObject("SELECT COUNT(*) FROM agent_collab WHERE record_type='event' AND title IN ('claimed','claim_renewed') AND JSON_EXTRACT(body,'$.targetId')=?",Long.class,id)>0;
        jdbc.update("UPDATE agent_collab SET heartbeat_at=NOW() WHERE id=?",id);
        audit(actor,renewed?"claim_renewed":"claimed",id,"line="+line);
        return response(200,"success",Map.of("claimId",id,"line",line,"agent",actor,"renewed",renewed));
    }

    @PostMapping("/heartbeat") @Transactional
    public ResponseEntity<Result<?>> heartbeat(@RequestBody Map<String,Object> body,HttpServletRequest req) {
        String actor=actor(req,body),line=text(body,"line",60,true);
        var rows=jdbc.queryForList("SELECT id FROM agent_collab WHERE record_type='claim' AND status='open' AND line_name=? AND from_agent=? FOR UPDATE",line,actor);
        if(rows.isEmpty())return response(404,"没有属于当前AI的认领",null);
        long id=((Number)rows.get(0).get("id")).longValue();
        jdbc.update("UPDATE agent_collab SET heartbeat_at=NOW() WHERE id=?",id);audit(actor,"heartbeat",id,"line="+line);
        return response(200,"已续期",Map.of("claimId",id));
    }

    @PostMapping("/release") @Transactional
    public ResponseEntity<Result<?>> release(@RequestBody Map<String,Object> body,HttpServletRequest req) {
        String actor=actor(req,body),line=text(body,"line",60,true);
        var rows=jdbc.queryForList("SELECT id FROM agent_collab WHERE record_type='claim' AND status='open' AND line_name=? AND from_agent=? FOR UPDATE",line,actor);
        if(rows.isEmpty())return response(404,"没有属于当前AI的有效认领",null);
        long id=((Number)rows.get(0).get("id")).longValue();
        jdbc.update("UPDATE agent_collab SET status='released' WHERE id=?",id);audit(actor,"released",id,"line="+line);
        return response(200,"已交回，历史保留",Map.of("claimId",id));
    }

    @PostMapping("/message") @Transactional
    public ResponseEntity<Result<?>> message(@RequestBody Map<String,Object> body,HttpServletRequest req) {
        String actor=actor(req,body),to=text(body,"to",40,true);
        if(!AGENTS.contains(to))return response(400,"未知收件人",null);
        long id=insert("message",actor,to,text(body,"line",60,false),text(body,"title",200,true),text(body,"body",12000,false),false);
        audit(actor,"message_sent",id,"to="+to);
        return response(200,"消息已存入收件箱，尚未处理",Map.of("id",id,"delivered",true,"processed",false));
    }

    @GetMapping("/inbox")
    public ResponseEntity<Result<?>> inbox(@RequestParam(required=false) String agent,@RequestParam(defaultValue="false") boolean includeRead,HttpServletRequest req) {
        String actor=identity.agent(req);
        if(agent!=null&&!actor.equals(agent))return response(403,"只能读取自己的收件箱",null);
        return response(200,"success",jdbc.queryForList("SELECT id,from_agent,to_agent,line_name,title,body,status,created_at FROM agent_collab " +
                "WHERE record_type='message' AND to_agent=? "+(includeRead?"":"AND status='open' ")+"ORDER BY id ASC LIMIT 100",actor));
    }

    @PostMapping("/message/{id}/read") @Transactional
    public ResponseEntity<Result<?>> markRead(@PathVariable Long id,@RequestBody Map<String,Object> body,HttpServletRequest req) {
        String actor=identity.agent(req);
        if(!Boolean.TRUE.equals(body.get("processed")))return response(400,"处理完成后才能确认",null);
        String evidence=text(body,"evidence",2000,true);
        var rows=jdbc.queryForList("SELECT status FROM agent_collab WHERE id=? AND record_type='message' AND to_agent=? FOR UPDATE",id,actor);
        if(rows.isEmpty())return response(404,"没有属于当前AI的留言",null);
        if("read".equals(rows.get(0).get("status")))return response(200,"已确认，无重复处理",Map.of("id",id));
        jdbc.update("UPDATE agent_collab SET status='read' WHERE id=?",id);audit(actor,"message_processed",id,evidence);
        return response(200,"处理结果已确认",Map.of("id",id));
    }

    @PostMapping("/ask") @Transactional
    public ResponseEntity<Result<?>> ask(@RequestBody Map<String,Object> body,HttpServletRequest req) {
        String actor=actor(req,body);
        long id=insert("ask",actor,null,text(body,"line",60,false),text(body,"title",200,true),text(body,"body",12000,true),false);
        audit(actor,"human_approval_requested",id,"Immutable request body; approval grants only its stated scope");
        return response(200,"等待真人批复",Map.of("id",id,"submitted",true,"approved",false));
    }

    @PostMapping("/ask/{id}/decide") @Transactional
    public ResponseEntity<Result<?>> decide(@PathVariable Long id,@RequestBody Map<String,Object> body,HttpServletRequest req) {
        var human=identity.human(req);
        String decision=text(body,"decision",16,true),comment=text(body,"comment",2000,true);
        if(!Set.of("approved","rejected").contains(decision))return response(400,"decision须为approved或rejected",null);
        int changed=jdbc.update("UPDATE agent_collab SET status=?,decided_by=?,decided_at=NOW() WHERE id=? AND record_type='ask' AND status='open'",decision,human.account(),id);
        if(changed==0)return response(409,"请求不存在或已经批复，不能覆盖原决定",null);
        audit("human",decision,id,"staffId="+human.staffId()+"; "+comment);
        return response(200,"真人决定已记录",Map.of("id",id,"decision",decision,"decidedBy",human.account()));
    }

    @GetMapping("/board")
    public ResponseEntity<Result<?>> board(HttpServletRequest req) {
        boolean human=req.getHeader("X-Agent-Token")==null;
        if(human)identity.human(req);else identity.agent(req);
        var data=new LinkedHashMap<String,Object>();
        data.put("claims",jdbc.queryForList("SELECT id,from_agent,line_name,title,status,heartbeat_at,created_at,UNIX_TIMESTAMP(heartbeat_at)*1000 heartbeatEpochMillis," +
                "(heartbeat_at IS NULL OR heartbeat_at<=DATE_SUB(NOW(),INTERVAL ? MINUTE)) stale FROM agent_collab WHERE record_type='claim' ORDER BY id DESC LIMIT 100",timeout()));
        data.put("asks",jdbc.queryForList("SELECT id,from_agent,line_name,title,body,status,created_at,decided_by,decided_at FROM agent_collab WHERE record_type='ask' ORDER BY id DESC LIMIT 100"));
        data.put("messages",jdbc.queryForList("SELECT id,from_agent,to_agent,line_name,title,status,created_at FROM agent_collab WHERE record_type='message' ORDER BY id DESC LIMIT 100"));
        data.put("canDecide",human);data.put("heartbeatTimeoutMinutes",timeout());data.put("serverTime",new Date());
        data.put("deliveryNotice","送达不等于处理；非驻留工具下次启动时领取；无人值守唤醒只派只读任务");
        return response(200,"success",data);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<?>> identityError(ResponseStatusException error) {
        return response(error.getStatusCode().value(),error.getReason(),null);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> inputError(IllegalArgumentException ignored) {return response(400,"字段缺失、过长或格式无效",null);}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> storageError(Exception ignored) {return response(503,"协作数据暂不可用，未确认成功，请保留任务并稍后重试",null);}

    private int timeout(){if(heartbeatTimeoutMinutes<1)throw new IllegalStateException("Invalid timeout");return heartbeatTimeoutMinutes;}
    private String actor(HttpServletRequest req,Map<String,Object> body) {
        String actor=identity.agent(req);
        for(String key:List.of("agent","from"))if(body.get(key)!=null&&!actor.equals(body.get(key)))
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"不能冒充其他AI");
        return actor;
    }
    private static String text(Map<String,Object> body,String key,int limit,boolean required) {
        Object raw=body.get(key);if(raw!=null&&!(raw instanceof String))throw new IllegalArgumentException();
        String value=raw==null?null:raw.toString().trim();
        if((required&&(value==null||value.isEmpty()))||(value!=null&&value.length()>limit))throw new IllegalArgumentException();
        return value==null||value.isEmpty()?null:value;
    }
    private long insert(String kind,String from,String to,String line,String title,String body,boolean heartbeat) {
        var keys=new GeneratedKeyHolder();
        jdbc.update(connection->{
            var statement=connection.prepareStatement("INSERT INTO agent_collab(record_type,from_agent,to_agent,line_name,title,body,status,heartbeat_at) VALUES(?,?,?,?,?,?,'open',"+(heartbeat?"NOW()":"NULL")+")",Statement.RETURN_GENERATED_KEYS);
            statement.setString(1,kind);statement.setString(2,from);statement.setString(3,to);statement.setString(4,line);statement.setString(5,title);statement.setString(6,body);return statement;
        },keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    private void audit(String actor,String action,long target,String evidence) {
        try { insert("event",actor,null,null,action,new ObjectMapper().writeValueAsString(Map.of("targetId",target,"evidence",evidence)),false); }
        catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException(error);}
    }
    private static ResponseEntity<Result<?>> response(int status,String message,Object data) {
        return ResponseEntity.status(status).body(new Result<>(status,message,data));
    }
}
