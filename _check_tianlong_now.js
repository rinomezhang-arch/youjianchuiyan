const { execSync } = require('child_process');
const sshKey = 'C:\\Users\\rinom\\.ssh\\id_rsa_new';
const sshHost = 'ubuntu@1.13.173.213';

function sshRun(label, sessionKey) {
  try {
    const params = JSON.stringify({ key: sessionKey, limit: 6, includeTools: false });
    const cmd = `openclaw gateway call sessions.history --params '${params}'`;
    const out = execSync(`ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "${sshKey}" ${sshHost} "${cmd}"`, {
      encoding: 'utf8', timeout: 20000
    });
    console.log(`=== ${label} ===`);
    console.log(out.slice(-4000));
  } catch (e) {
    console.log(`${label} FAIL:`, (e.stdout || '') + (e.stderr || '') + e.message.slice(-500));
  }
}

sshRun('MAIN', 'agent:main:main');
sshRun('DASHBOARD', 'agent:main:dashboard:dbfaa562-53e6-40a4-836b-a2a540d42137');
