const { execSync } = require('child_process');
const sshKey = 'C:\\Users\\rinom\\.ssh\\id_rsa_new';
const sshHost = 'ubuntu@1.13.173.213';

try {
  // Use gateway call to get session history via sessions.history
  const params = JSON.stringify({key: "agent:main:main", limit: 8, includeTools: false});
  const cmd = `/usr/local/bin/openclaw gateway call sessions.history --params '${params}'`;
  const out = execSync(`ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "${sshKey}" ${sshHost} "${cmd}"`, { encoding: 'utf8', timeout: 20000 });
  console.log(out.slice(-5000));
} catch(e) {
  console.log('Error:', (e.stdout || '') + (e.stderr || '') + e.message.slice(-500));
}
