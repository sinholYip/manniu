import { MealPeriod, PERIOD_LABELS } from '../../types/models'
import { addMeal, caloriesFromMacros, deleteMeal, inferMealPeriod, loadData } from '../../utils/store'

const PERIODS: MealPeriod[] = ['BREAKFAST', 'LUNCH', 'PRE_WORKOUT', 'DINNER', 'NIGHT_SNACK']

Page({
  data: {
    periods: PERIODS.map(value => ({ value, label: PERIOD_LABELS[value] })),
    periodIndex: Math.max(0, PERIODS.indexOf(inferMealPeriod())),
    name: '', carbs: '', protein: '', fat: '', calculated: '0',
    meals: [] as Array<Record<string, string>>,
    empty: true
  },

  onShow() { this.refresh() },
  onName(e: WechatMiniprogram.Input) { this.setData({ name: e.detail.value }) },
  onCarbs(e: WechatMiniprogram.Input) { this.setData({ carbs: e.detail.value }, () => this.recalculate()) },
  onProtein(e: WechatMiniprogram.Input) { this.setData({ protein: e.detail.value }, () => this.recalculate()) },
  onFat(e: WechatMiniprogram.Input) { this.setData({ fat: e.detail.value }, () => this.recalculate()) },
  onPeriod(e: WechatMiniprogram.PickerChange) { this.setData({ periodIndex: Number(e.detail.value) }) },

  recalculate() {
    this.setData({ calculated: caloriesFromMacros(numberOf(this.data.carbs), numberOf(this.data.protein), numberOf(this.data.fat)).toFixed(0) })
  },

  save() {
    const name = this.data.name.trim()
    if (!name) return void wx.showToast({ title: '请输入食物名称', icon: 'none' })
    addMeal({
      name,
      period: PERIODS[this.data.periodIndex],
      carbs: numberOf(this.data.carbs),
      protein: numberOf(this.data.protein),
      fat: numberOf(this.data.fat)
    })
    this.setData({ name: '', carbs: '', protein: '', fat: '', calculated: '0' })
    wx.showToast({ title: '已记录', icon: 'success' })
    this.refresh()
  },

  remove(e: WechatMiniprogram.BaseEvent) {
    const time = String(e.currentTarget.dataset.time)
    wx.showModal({ title: '删除这条饮食？', content: '删除后无法恢复。', success: result => {
      if (result.confirm) { deleteMeal(time); this.refresh() }
    } })
  },

  refresh() {
    const meals = loadData().meals.slice().reverse()
    this.setData({
      empty: meals.length === 0,
      meals: meals.map(item => ({
        time: item.time,
        name: item.name,
        period: PERIOD_LABELS[item.period],
        date: formatTime(item.time),
        kcal: item.kcal.toFixed(0),
        macros: `碳 ${item.carbs.toFixed(0)} · 蛋 ${item.protein.toFixed(0)} · 脂 ${item.fat.toFixed(0)}`
      }))
    })
  }
})

function numberOf(value: string): number { const parsed = Number(value); return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0 }
function formatTime(iso: string): string { const d = new Date(iso); return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
