export type MealPeriod = 'BREAKFAST' | 'LUNCH' | 'PRE_WORKOUT' | 'DINNER' | 'NIGHT_SNACK'

export interface WeightEntry {
  time: string
  kg: number
  source: string
}

export interface MealEntry {
  time: string
  name: string
  kcal: number
  source: string
  period: MealPeriod
  carbs: number
  protein: number
  fat: number
}

export interface CaloriePlan {
  trainingCarbsGrams: number
  trainingProteinGrams: number
  trainingFatGrams: number
  restCarbsGrams: number
  restProteinGrams: number
  restFatGrams: number
  trainingDays: number[]
}

export interface ManniuData {
  schemaVersion: 1
  exportedAt: string
  weights: WeightEntry[]
  meals: MealEntry[]
  foodPresets: unknown[]
  hiddenQuickFoods: string[]
  quickFoodOrder: string[]
  dashboardSectionOrder: string[]
  trainingSettings: Record<string, unknown>
  caloriePlan: CaloriePlan
  healthProfile: Record<string, unknown>
  workoutDays: unknown[]
  exercisePrs: unknown[]
}

export const PERIOD_LABELS: Record<MealPeriod, string> = {
  BREAKFAST: '早餐',
  LUNCH: '午餐',
  PRE_WORKOUT: '练前餐',
  DINNER: '晚餐',
  NIGHT_SNACK: '宵夜'
}
