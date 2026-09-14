"""Coordinator's single replay check. Never print raw payload or credentials."""
import base64
import json
from pathlib import Path
import subprocess
import sys

CANDIDATE = '836d1308488891b26a0fcb38d817b43538c3d90f'
SCRIPT = 'scripts/trae-marketing-inquiry-real-e2e-58/test-same-payload-replay.sh'

REMOTE = r'''
import base64,json,os,re,subprocess,sys
def run(args,**kw):
    return subprocess.run(args,capture_output=True,text=True,timeout=20,**kw)
def gate(name, passed):
    print(name+'='+str(bool(passed)).lower(),flush=True)
    if not passed: raise SystemExit(3)
root=os.path.expanduser('~/tr58-work')
marker=root+'/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt'
schema=open(marker).read().strip()
gate('schema_is_tr58',bool(re.fullmatch(r'tr58_e2e_[0-9_]+',schema)))
c=run(['docker','inspect','youjian-mysql-test-13317'])
gate('test_container_readable',c.returncode==0)
container=json.loads(c.stdout)[0]
bindings=container['NetworkSettings']['Ports'].get('3306/tcp',[])
gate('test_container_port_match',any(x['HostPort']=='13317' for x in bindings))
gate('test_container_running',container['State']['Running'])
ports=run(['ss','-ltnp','sport = :18080'])
pids=set(re.findall(r'pid=(\d+)',ports.stdout))
gate('single_test_backend',len(pids)==1)
pid=next(iter(pids))
env=dict(x.split(b'=',1) for x in open('/proc/'+pid+'/environ','rb').read().split(b'\0') if b'=' in x)
gate('backend_test_workdir',os.path.realpath('/proc/'+pid+'/cwd')==root+'/banquet_project')
url=env.get(b'SPRING_DATASOURCE_URL',b'').decode()
gate('backend_test_schema_match',url.startswith('jdbc:mysql://127.0.0.1:13317/'+schema+'?'))
mysql=['mysql','-uroot','--protocol=tcp','-h127.0.0.1','-P13317','-N']
db=run(mysql+['-e','SELECT @@port, @@datadir;'])
gate('database_test_identity',db.returncode==0 and db.stdout.strip()=='3306\t/var/lib/mysql/')
q="SELECT COUNT(*) FROM booking_inquiry b JOIN marketing_attribution_event e ON e.business_id=b.id AND e.business_type='booking_inquiry' WHERE b.id=3 AND e.business_no='INQ3' AND e.event_type='inquiry' AND b.source_code='SRC-VISIBLE-001' AND e.source_code=b.source_code AND e.store_id=b.store_id AND e.publication_id=b.marketing_publication_id;"
row=run(mysql+[schema,'-e',q])
gate('original_inquiry_and_attribution_match',row.returncode==0 and row.stdout.strip()=='1')
script=base64.b64decode('__SCRIPT__').decode()
result=run(['bash'],input=script)
allow=re.compile(r'^(=== SAME PAYLOAD REPLAY ===|requestId=\[REDACTED\]|before: booking_inquiry=\d+, events=\d+|after: booking_inquiry=\d+, events=\d+|response_code=\d+|response_inquiryNo=INQ\d+|delta: booking_inquiry=-?\d+, events=-?\d+|\[PASS\] same-payload replay: 200, same inquiryNo, zero new rows|\[FAIL\] check above|REPLAY_DONE)$')
lines=[s.strip() for s in result.stdout.splitlines() if s.strip()]
safe=all(allow.fullmatch(s) for s in lines)
for line in lines:
    if allow.fullmatch(line): print(line)
print('output_allowlist_pass='+str(safe).lower())
print('stderr_empty='+str(not result.stderr).lower())
print('script_exit='+str(result.returncode))
if result.returncode or not safe or result.stderr or 'REPLAY_DONE' not in lines: raise SystemExit(1)
'''

def main():
    evidence=Path(__file__).with_name('same-payload-output.txt')
    if evidence.exists(): raise SystemExit('EVIDENCE_EXISTS_DO_NOT_RERUN')
    script=subprocess.run(['git','show',CANDIDATE+':'+SCRIPT],check=True,capture_output=True).stdout
    remote=REMOTE.replace('__SCRIPT__',base64.b64encode(script).decode())
    key=Path.home()/'.ssh'/'id_rsa_new'
    target=sys.argv[1]
    result=subprocess.run(['ssh','-i',str(key),'-o','IdentitiesOnly=yes','-o','BatchMode=yes','-o','ConnectTimeout=10',target,'python3 -'],input=remote,text=True,capture_output=True,timeout=90)
    # Remote output contains only gate names, counts, and the synthetic business number.
    evidence.write_text('candidate='+CANDIDATE+'\n'+result.stdout+'remote_exit='+str(result.returncode)+'\n',encoding='utf-8')
    print(result.stdout,end='')
    print('remote_exit='+str(result.returncode))
    if result.stderr: print('remote_stderr_present=true')
    return result.returncode

if __name__=='__main__': sys.exit(main())
