import { exportAndroidBackup, importAndroidBackup, loadData } from '../../utils/store'

Page({
  data: { weights: 0, meals: 0, lastExport: '尚未导出' },
  onShow() { this.refresh() },
  refresh() {
    const data = loadData()
    this.setData({ weights: data.weights.length, meals: data.meals.length })
  },
  importBackup() {
    wx.chooseMessageFile({ count: 1, type: 'file', extension: ['json'], success: choice => {
      const file = choice.tempFiles[0]
      wx.getFileSystemManager().readFile({ filePath: file.path, encoding: 'utf8', success: result => {
        try {
          const imported = importAndroidBackup(String(result.data))
          this.refresh()
          wx.showModal({ title: '恢复完成', content: `新增 ${imported.weightsAdded} 条体重、${imported.mealsAdded} 条饮食记录`, showCancel: false })
        } catch (error) {
          wx.showModal({ title: '无法恢复', content: error instanceof Error ? error.message : '备份格式无效', showCancel: false })
        }
      }, fail: () => wx.showToast({ title: '读取文件失败', icon: 'none' }) })
    } })
  },
  exportBackup() {
    const path = `${wx.env.USER_DATA_PATH}/man牛健康数据.json`
    wx.getFileSystemManager().writeFile({ filePath: path, data: exportAndroidBackup(), encoding: 'utf8', success: () => {
      wx.shareFileMessage({ filePath: path, fileName: 'man牛健康数据.json', success: () => {
        this.setData({ lastExport: new Date().toLocaleString() })
      }, fail: () => wx.showToast({ title: '已生成文件，可稍后再分享', icon: 'none' }) })
    }, fail: () => wx.showToast({ title: '生成备份失败', icon: 'none' }) })
  }
})
