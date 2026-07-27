const { execSync } = require('child_process');

const sshKey = 'C:\\Users\\rinom\\.ssh\\id_rsa_new';
const sshHost = 'ubuntu@1.13.173.213';

try {
  const cmd = `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i "${sshKey}" ${sshHost} "openclaw sessions list --json"`;
  const out = execSync(cmd, { encoding: 'utf8', timeout: 20000 });
  const data = JSON.parse(out);
  
  // Find the main session
  const mainSession = data.sessions.find(s => s.key === 'agent:main:main');
  if (mainSession) {
    console.log('Main session lastInteractionAt:', new Date(mainSession.lastInteractionAt).toISOString());
    console.log('Main session updatedAt:', new Date(mainSession.updatedAt).toISOString());
    console.log('Main session status:', mainSession.status);
  }
  
  // Check for any recent activity in the last 30 minutes
  const now = Date.now();
  const thirtyMinAgo = now - 30 * 60 * 1000;
  const recentSessions = data.sessions.filter(s => s.lastInteractionAt > thirtyMinAgo);
  
  console.log('\nRecent sessions (last 30 min):', recentSessions.length);
  recentSessions.forEach(s => {
    console.log(`  - ${s.key}: ${new Date(s.lastInteractionAt).toISOString()}`);
  });
  
} catch (e) {
  console.error('Error:', e.message);
  process.exit(1);
}
