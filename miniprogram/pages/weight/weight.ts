import { addWeight, loadData } from '../../utils/store'

Page({
  data: { kg: '', latest: '--', change: '--', records: [] as Array<Record<string, string>>, empty: true },
  onShow() { this.refresh() },
  onKg(e: WechatMiniprogram.Input) { this.setData({ kg: e.detail.value }) },
  save() {
    const kg = Number(this.data.kg)
    if (!Number.isFinite(kg) || kg < 20 || kg > 400) return void wx.showToast({ title: '请输入 20–400 kg', icon: 'none' })
    addWeight(kg)
    this.setData({ kg: '' })
    wx.showToast({ title: '已记录', icon: 'success' })
    this.refresh()
  },
  refresh() {
    const records = loadData().weights.slice().reverse()
    const latest = records[0]
    const previous = records[1]
    const change = latest && previous ? latest.kg - previous.kg : null
    this.setData({
      latest: latest ? latest.kg.toFixed(1) : '--',
      change: change === null ? '--' : `${change >= 0 ? '+' : ''}${change.toFixed(1)} kg`,
      empty: records.length === 0,
      records: records.map(item => ({ time: item.time, kg: item.kg.toFixed(1), date: formatTime(item.time) }))
    })
  }
})

function formatTime(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
