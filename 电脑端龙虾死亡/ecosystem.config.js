module.exports = {
  apps: [
    {
      name: "lobster",
      script: "node",
      args: ["C:\\Users\\rinom\\AppData\\Roaming\\npm\\node_modules\\openclaw\\openclaw.mjs", "gateway"],
      cwd: "C:\\Users\\rinom\\.openclaw",
      instances: 1,
      exec_mode: "fork",
      watch: false,
      max_memory_restart: "512M",
      env: {
        NODE_ENV: "production"
      },
      error_file: "C:\\Users\\rinom\\openclaw\\logs\\lobster-error.log",
      out_file: "C:\\Users\\rinom\\openclaw\\logs\\lobster-out.log",
      log_date_format: "YYYY-MM-DD HH:mm:ss",
      merge_logs: true,
      autorestart: true,
      restart_delay: 5000,
      max_restarts: 10,
      min_uptime: "30s",
      kill_timeout: 5000
    }
  ]
};