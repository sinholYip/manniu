import { CaloriePlan, ManniuData, MealEntry, MealPeriod, WeightEntry } from '../types/models'

const STORAGE_KEY = 'manniu_data_v1'

const DEFAULT_PLAN: CaloriePlan = {
  trainingCarbsGrams: 275,
  trainingProteinGrams: 165,
  trainingFatGrams: 220 / 4.5,
  restCarbsGrams: 225,
  restProteinGrams: 135,
  restFatGrams: 40,
  trainingDays: [1, 2, 4, 6]
}

export function emptyData(): ManniuData {
  return {
    schemaVersion: 1,
    exportedAt: new Date().toISOString(),
    weights: [],
    meals: [],
    foodPresets: [],
    hiddenQuickFoods: [],
    quickFoodOrder: [],
    dashboardSectionOrder: [],
    trainingSettings: {},
    caloriePlan: DEFAULT_PLAN,
    healthProfile: {},
    workoutDays: [],
    exercisePrs: []
  }
}

export function loadData(): ManniuData {
  const stored = wx.getStorageSync<ManniuData>(STORAGE_KEY)
  return stored && stored.schemaVersion === 1 ? normalizeData(stored) : emptyData()
}

export function saveData(data: ManniuData): void {
  wx.setStorageSync(STORAGE_KEY, { ...data, exportedAt: new Date().toISOString() })
}

export function addWeight(kg: number): void {
  const data = loadData()
  data.weights.push({ time: new Date().toISOString(), kg, source: 'man牛小程序' })
  data.weights.sort((a, b) => a.time.localeCompare(b.time))
  saveData(data)
}

export function addMeal(input: Omit<MealEntry, 'time' | 'source' | 'kcal'>): void {
  const data = loadData()
  data.meals.push({
    ...input,
    time: new Date().toISOString(),
    source: 'man牛小程序',
    kcal: caloriesFromMacros(input.carbs, input.protein, input.fat)
  })
  data.meals.sort((a, b) => a.time.localeCompare(b.time))
  saveData(data)
}

export function deleteMeal(time: string): void {
  const data = loadData()
  data.meals = data.meals.filter(item => item.time !== time)
  saveData(data)
}

export function caloriesFromMacros(carbs: number, protein: number, fat: number): number {
  return Math.max(0, carbs) * 4 + Math.max(0, protein) * 4 + Math.max(0, fat) * 9
}

export function inferMealPeriod(date = new Date(), trainingDay = isTrainingDay(date)): MealPeriod {
  const hour = date.getHours()
  if (hour >= 4 && hour <= 10) return 'BREAKFAST'
  if (hour >= 11 && hour <= 14) return 'LUNCH'
  if (hour >= 15 && hour <= 17) return trainingDay ? 'PRE_WORKOUT' : 'LUNCH'
  if (hour >= 18 && hour <= 20) return 'DINNER'
  return 'NIGHT_SNACK'
}

export function isTrainingDay(date: Date, plan = loadData().caloriePlan): boolean {
  const day = date.getDay() === 0 ? 7 : date.getDay()
  return plan.trainingDays.includes(day)
}

export function targetForToday(data: ManniuData): { kcal: number; carbs: number; protein: number; fat: number; training: boolean } {
  const training = isTrainingDay(new Date(), data.caloriePlan)
  const plan = data.caloriePlan
  const carbs = training ? plan.trainingCarbsGrams : plan.restCarbsGrams
  const protein = training ? plan.trainingProteinGrams : plan.restProteinGrams
  const fat = training ? plan.trainingFatGrams : plan.restFatGrams
  return { kcal: caloriesFromMacros(carbs, protein, fat), carbs, protein, fat, training }
}

export function isToday(iso: string): boolean {
  const value = new Date(iso)
  const today = new Date()
  return value.getFullYear() === today.getFullYear() && value.getMonth() === today.getMonth() && value.getDate() === today.getDate()
}

export function importAndroidBackup(raw: string): { data: ManniuData; weightsAdded: number; mealsAdded: number } {
  const parsed = JSON.parse(raw) as Partial<ManniuData>
  if (parsed.schemaVersion !== 1) throw new Error('仅支持 schemaVersion 1 的 man牛备份')
  const incoming = normalizeData(parsed as ManniuData)
  const current = loadData()
  const weightKeys = new Set(current.weights.map(item => `${item.time}|${item.kg}`))
  const mealKeys = new Set(current.meals.map(item => `${item.time}|${item.name}|${item.kcal}`))
  const weights = incoming.weights.filter(item => !weightKeys.has(`${item.time}|${item.kg}`))
  const meals = incoming.meals.filter(item => !mealKeys.has(`${item.time}|${item.name}|${item.kcal}`))
  const merged: ManniuData = {
    ...current,
    ...incoming,
    weights: [...current.weights, ...weights].sort((a, b) => a.time.localeCompare(b.time)),
    meals: [...current.meals, ...meals].sort((a, b) => a.time.localeCompare(b.time)),
    foodPresets: mergeByName(current.foodPresets, incoming.foodPresets)
  }
  saveData(merged)
  return { data: merged, weightsAdded: weights.length, mealsAdded: meals.length }
}

export function exportAndroidBackup(): string {
  return JSON.stringify({ ...loadData(), exportedAt: new Date().toISOString() }, null, 2)
}

function normalizeData(data: ManniuData): ManniuData {
  const base = emptyData()
  const weights = Array.isArray(data.weights) ? data.weights.filter(validWeight) : []
  const meals = Array.isArray(data.meals) ? data.meals.filter(validMeal) : []
  return {
    ...base,
    ...data,
    caloriePlan: { ...DEFAULT_PLAN, ...(data.caloriePlan || {}) },
    weights,
    meals
  }
}

function validWeight(item: WeightEntry): boolean {
  return Boolean(item && !Number.isNaN(Date.parse(item.time)) && item.kg >= 20 && item.kg <= 400)
}

function validMeal(item: MealEntry): boolean {
  return Boolean(item && !Number.isNaN(Date.parse(item.time)) && item.name && item.kcal >= 0 && item.carbs >= 0 && item.protein >= 0 && item.fat >= 0)
}

function mergeByName(current: unknown[], incoming: unknown[]): unknown[] {
  const values = new Map<string, unknown>()
  for (const item of [...current, ...incoming]) {
    const name = typeof item === 'object' && item && 'name' in item ? String((item as { name: unknown }).name).toLowerCase() : JSON.stringify(item)
    values.set(name, item)
  }
  return [...values.values()]
}
