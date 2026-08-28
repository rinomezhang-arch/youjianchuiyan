/**
 * 文件上传 Upload
 * 两种实现：
 *   1) 如果后端有统一的 Spring 文件上传接口：POST /files/upload（form-data，字段名 file），
 *      返回 { code:200, data: { url: 'https://xxx/a.png' } }
 *   2) 如果你们走腾讯云 COS 直传（又见炊烟的部署常用方式），前端用 cos-js-sdk-v5 或
 *      由后端签发临时密钥/签名再上传。
 * 这里封装"通用上传函数"，对接第一种（后端统一上传）；
 * 如需 COS 直传，可把 upload() 的内部改成走 COS SDK + 后端 STS 签名流程。
 */
import http from '@/utils/request'

/**
 * 选择图片 + 上传（整合两个步骤，页面一行调用）
 * @param {Object} opt
 * @param {number} opt.count     可选张数（默认1，最多9）
 * @param {string} opt.sourceType album/camera 都选
 * @param {Function} opt.onProgress  预留进度回调（暂用接口回调支持）
 */
export function chooseAndUpload(opt = {}) {
  const { count = 1, sourceType = ['album', 'camera'] } = opt
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sourceType,
      success: async (r) => {
        try {
          const list = []
          for (const f of r.tempFilePaths) {
            const url = await uploadOne(f)
            list.push(url)
          }
          resolve(list)
        } catch (e) { reject(e) }
      },
      fail: reject
    })
  })
}

function uploadOne(filePath) {
  return http.upload('/files/upload', filePath, {}, 'file', {})
}

export function uploadFile(filePath, formData = {}, field = 'file') {
  return http.upload('/files/upload', filePath, formData, field)
}

export default { chooseAndUpload, uploadFile }
