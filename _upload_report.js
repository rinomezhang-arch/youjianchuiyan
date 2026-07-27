const COS = require('cos-nodejs-sdk-v5');

const cos = new COS({
    SecretId: process.env.COS_SECRET_ID || 'AKIDyX3m8ZvT4nKz',
    SecretKey: process.env.COS_SECRET_KEY || '9X7mP2qR5sT8vW0yU3iO6pA1dF4gH7jK',
    Domain: 'cos.accelerate.myqcloud.com',
    Protocol: 'https:',
});

const Bucket = 'youjian-data-1409286104';
const Region = 'ap-nanjing';
const Key = '天地双龙工作空间/项目管理/餐饮管理系统/数据库审计报告.md';
const FilePath = 'f:\\solo\\数据库审计报告.md';

cos.putObject({
    Bucket,
    Region,
    Key,
    Body: require('fs').createReadStream(FilePath),
}, function(err, data) {
    if (err) {
        console.error('上传失败:', err);
    } else {
        console.log('✅ 上传成功!');
        console.log('ETag:', data.ETag);
        console.log('Location:', data.Location);
        console.log('路径:', Key);
        
        // 验证文件存在
        cos.headObject({ Bucket, Region, Key }, function(e, d) {
            if (e) {
                console.log('验证失败:', e);
            } else {
                console.log('✅ 文件验证通过，大小:', d['Content-Length'], '字节');
            }
        });
    }
});
