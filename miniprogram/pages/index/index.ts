import { PERIOD_LABELS } from '../../types/models'
import { isToday, loadData, targetForToday } from '../../utils/store'

Page({
  data: {
    dateText: '',
    modeText: '',
    consumed: '0',
    target: '0',
    progress: 0,
    carbs: '0',
    protein: '0',
    fat: '0',
    targetCarbs: '0',
    targetProtein: '0',
    targetFat: '0',
    latestWeight: '--',
    meals: [] as Array<Record<string, string>>,
    emptyMeals: true
  },

  onShow() { this.refresh() },

  refresh() {
    const data = loadData()
    const target = targetForToday(data)
    const meals = data.meals.filter(item => isToday(item.time))
    const consumed = meals.reduce((sum, item) => sum + item.kcal, 0)
    const carbs = meals.reduce((sum, item) => sum + item.carbs, 0)
    const protein = meals.reduce((sum, item) => sum + item.protein, 0)
    const fat = meals.reduce((sum, item) => sum + item.fat, 0)
    const latestWeight = data.weights.length ? data.weights[data.weights.length - 1].kg.toFixed(1) : '--'
    this.setData({
      dateText: formatDate(new Date()),
      modeText: target.training ? '训练日' : '休息日',
      consumed: consumed.toFixed(0),
      target: target.kcal.toFixed(0),
      progress: Math.min(100, Math.round(consumed / Math.max(1, target.kcal) * 100)),
      carbs: carbs.toFixed(0),
      protein: protein.toFixed(0),
      fat: fat.toFixed(0),
      targetCarbs: target.carbs.toFixed(0),
      targetProtein: target.protein.toFixed(0),
      targetFat: target.fat.toFixed(0),
      latestWeight,
      meals: meals.slice(-5).reverse().map(item => ({
        time: item.time,
        name: item.name,
        period: PERIOD_LABELS[item.period],
        kcal: item.kcal.toFixed(0)
      })),
      emptyMeals: meals.length === 0
    })
  },

  goMeal() { wx.switchTab({ url: '/pages/meal/meal' }) },
  goWeight() { wx.switchTab({ url: '/pages/weight/weight' }) }
})

function formatDate(date: Date): string {
  return `${date.getMonth() + 1}月${date.getDate()}日 · 周${'日一二三四五六'[date.getDay()]}`
}
