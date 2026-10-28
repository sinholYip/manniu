package com.ranke.health.data

import com.ranke.health.model.FoodReference
import com.ranke.health.model.caloriesFromMacros
import com.ranke.health.model.foodDisplayName
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class FoodCatalogRepository {
    fun quickFoods(): List<FoodReference> = defaultQuickFoods

    // english = true 时额外用英文显示名匹配，中文名始终可搜，保证两种界面下都能命中本地参考值。
    internal fun searchLocalFoods(query: String, english: Boolean = false): List<FoodReference> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return searchableFoods.filter {
            it.name.contains(normalized, ignoreCase = true) ||
                (english && foodDisplayName(it.name, true).contains(normalized, ignoreCase = true))
        }
    }

    suspend fun search(query: String, english: Boolean = false): List<FoodReference> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return@withContext emptyList()
        val local = searchLocalFoods(normalized, english)
        val online = runCatching { searchOpenFoodFacts(normalized, english) }.getOrDefault(emptyList())
        (local + online).distinctBy { it.name.lowercase() }.take(30)
    }

    private fun searchOpenFoodFacts(query: String, english: Boolean): List<FoodReference> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = URI(
            "https://world.openfoodfacts.org/cgi/search.pl" +
                "?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=8" +
                "&fields=product_name,product_name_zh,brands,nutriments"
        ).toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "ManNiuHealth/0.2 Android")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val products = root.optJSONArray("products") ?: return emptyList()
            buildList {
                for (index in 0 until products.length()) {
                    val product = products.optJSONObject(index) ?: continue
                    val nutrients = product.optJSONObject("nutriments") ?: continue
                    val localizedName = product.optString("product_name_zh")
                    val primaryName = product.optString("product_name")
                    val name = if (english) primaryName.ifBlank { localizedName } else localizedName.ifBlank { primaryName }
                    if (name.isBlank()) continue
                    val carbs = nutrients.optDouble("carbohydrates_100g", Double.NaN)
                    val protein = nutrients.optDouble("proteins_100g", Double.NaN)
                    val fat = nutrients.optDouble("fat_100g", Double.NaN)
                    if (listOf(carbs, protein, fat).all { it.isNaN() }) continue
                    val safeCarbs = carbs.orZero()
                    val safeProtein = protein.orZero()
                    val safeFat = fat.orZero()
                    add(FoodReference(
                        name = name,
                        kilocaloriesPer100g = caloriesFromMacros(safeCarbs, safeProtein, safeFat),
                        carbohydratesPer100g = safeCarbs,
                        proteinPer100g = safeProtein,
                        fatPer100g = safeFat,
                        source = "Open Food Facts"
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun Double.orZero(): Double = if (isNaN() || isInfinite()) 0.0 else coerceAtLeast(0.0)

    companion object {
        private val defaultQuickFoods = listOf(
            FoodReference("米饭（熟）", 116.0, 25.9, 2.6, 0.3, "内置参考值"),
            FoodReference("馒头", 223.0, 47.0, 7.0, 1.1, "内置参考值"),
            FoodReference("面条（熟）", 110.0, 24.2, 2.7, 0.2, "内置参考值"),
            FoodReference("鸡蛋", 144.0, 2.8, 13.3, 8.8, "内置参考值"),
            FoodReference("纯牛奶", 60.0, 4.8, 3.2, 3.3, "内置参考值"),
            FoodReference("鸡胸肉（熟）", 165.0, 0.0, 31.0, 3.6, "内置参考值"),
            FoodReference("牛肉（瘦，熟）", 194.0, 0.0, 26.0, 9.0, "内置参考值"),
            FoodReference("北豆腐", 98.0, 4.2, 12.2, 4.8, "内置参考值")
        )

        private val searchableFoods = defaultQuickFoods + listOf(
            FoodReference("红薯（熟）", 76.0, 17.7, 1.4, 0.2, "内置参考值"),
            FoodReference("香蕉", 93.0, 22.0, 1.4, 0.2, "内置参考值"),
            FoodReference("苹果", 53.0, 13.7, 0.4, 0.2, "内置参考值"),
            FoodReference("西兰花（熟）", 35.0, 7.2, 2.4, 0.4, "内置参考值"),
            FoodReference("燕麦粥（熟）", 71.0, 12.0, 2.5, 1.5, "USDA参考值"),
            FoodReference("糙米饭（熟）", 123.0, 25.6, 2.7, 1.0, "USDA参考值"),
            FoodReference("藜麦（熟）", 120.0, 21.3, 4.4, 1.9, "USDA参考值"),
            FoodReference("玉米（熟）", 96.0, 21.0, 3.4, 1.5, "USDA参考值"),
            FoodReference("土豆（熟）", 87.0, 20.1, 1.9, 0.1, "USDA参考值"),
            FoodReference("全麦面包", 247.0, 41.4, 13.0, 3.4, "USDA参考值"),
            FoodReference("鸡腿肉（熟，去皮）", 209.0, 0.0, 26.0, 10.9, "USDA参考值"),
            FoodReference("猪里脊（熟）", 196.0, 0.0, 29.0, 7.5, "USDA参考值"),
            FoodReference("三文鱼（熟）", 206.0, 0.0, 22.1, 12.4, "USDA参考值"),
            FoodReference("虾（熟）", 99.0, 0.2, 24.0, 0.3, "USDA参考值"),
            FoodReference("金枪鱼（水浸罐头）", 116.0, 0.0, 25.5, 0.8, "USDA参考值"),
            FoodReference("希腊酸奶（原味脱脂）", 59.0, 3.6, 10.3, 0.4, "USDA参考值"),
            FoodReference("切达奶酪", 403.0, 1.3, 22.9, 33.1, "USDA参考值"),
            FoodReference("毛豆（熟）", 121.0, 8.9, 11.9, 5.2, "USDA参考值"),
            FoodReference("鹰嘴豆（熟）", 164.0, 27.4, 8.9, 2.6, "USDA参考值"),
            FoodReference("黑豆（熟）", 132.0, 23.7, 8.9, 0.5, "USDA参考值"),
            FoodReference("牛油果", 160.0, 8.5, 2.0, 14.7, "USDA参考值"),
            FoodReference("杏仁", 579.0, 21.6, 21.2, 49.9, "USDA参考值"),
            FoodReference("花生酱", 588.0, 20.0, 25.1, 50.4, "USDA参考值"),
            FoodReference("蓝莓", 57.0, 14.5, 0.7, 0.3, "USDA参考值"),
            FoodReference("橙子", 47.0, 11.8, 0.9, 0.1, "USDA参考值"),
            FoodReference("菠菜（熟）", 23.0, 3.8, 3.0, 0.3, "USDA参考值"),

            // 中国食物成分表代表值；成品会随配方、含水量和烹调方式变化。
            FoodReference("白米粥", 46.0, 9.8, 1.1, 0.3, "中国食物成分表参考"),
            FoodReference("小米粥", 46.0, 8.4, 1.4, 0.7, "中国食物成分表参考"),
            FoodReference("杂粮粥", 64.0, 12.4, 2.2, 0.7, "中国食物成分表参考"),
            FoodReference("花卷", 214.0, 45.6, 6.4, 1.0, "中国食物成分表参考"),
            FoodReference("油条", 388.0, 51.0, 6.9, 17.6, "中国食物成分表参考"),
            FoodReference("猪肉包子", 227.0, 28.6, 7.3, 10.0, "中国食物成分表参考"),
            FoodReference("菜包子", 180.0, 32.0, 6.0, 3.2, "中国食物成分表参考"),
            FoodReference("猪肉水饺（熟）", 231.0, 26.0, 9.0, 10.1, "中国食物成分表参考"),
            FoodReference("韭菜鸡蛋水饺（熟）", 190.0, 27.0, 8.0, 5.6, "中国食物成分表参考"),
            FoodReference("烧卖", 220.0, 30.0, 8.5, 7.0, "中国食物成分表参考"),
            FoodReference("年糕", 156.0, 34.7, 3.3, 0.6, "中国食物成分表参考"),
            FoodReference("米粉（熟）", 116.0, 25.9, 2.0, 0.3, "中国食物成分表参考"),
            FoodReference("粉丝（熟）", 84.0, 19.1, 0.8, 0.1, "中国食物成分表参考"),
            FoodReference("凉皮", 117.0, 22.8, 4.4, 0.3, "中国食物成分表参考"),
            FoodReference("荞麦面（熟）", 99.0, 21.4, 5.1, 0.1, "中国食物成分表参考"),
            FoodReference("玉米面粥", 43.0, 9.2, 1.1, 0.3, "中国食物成分表参考"),

            FoodReference("猪里脊（生）", 155.0, 0.0, 20.2, 7.9, "中国食物成分表参考"),
            FoodReference("猪瘦肉（生）", 143.0, 1.5, 20.3, 6.2, "中国食物成分表参考"),
            FoodReference("猪五花肉（生）", 395.0, 2.4, 14.0, 37.0, "中国食物成分表参考"),
            FoodReference("鸡腿肉（生，去皮）", 120.0, 0.0, 19.2, 4.6, "中国食物成分表参考"),
            FoodReference("鸭肉（熟）", 240.0, 0.0, 23.5, 15.7, "中国食物成分表参考"),
            FoodReference("猪肝（熟）", 129.0, 5.0, 20.4, 3.6, "中国食物成分表参考"),
            FoodReference("草鱼（生）", 113.0, 0.0, 16.6, 5.2, "中国食物成分表参考"),
            FoodReference("鲫鱼（生）", 108.0, 3.8, 17.1, 2.7, "中国食物成分表参考"),
            FoodReference("黄花鱼（生）", 99.0, 0.1, 17.9, 3.0, "中国食物成分表参考"),
            FoodReference("带鱼（生）", 127.0, 3.1, 17.7, 4.9, "中国食物成分表参考"),
            FoodReference("鱿鱼（鲜）", 84.0, 3.0, 17.4, 1.6, "中国食物成分表参考"),
            FoodReference("蛤蜊（熟）", 79.0, 5.0, 13.8, 1.0, "中国食物成分表参考"),

            FoodReference("豆浆（无糖）", 31.0, 1.2, 3.0, 1.6, "中国食物成分表参考"),
            FoodReference("豆腐干", 142.0, 11.5, 16.2, 3.6, "中国食物成分表参考"),
            FoodReference("豆腐皮", 447.0, 18.8, 51.6, 23.0, "中国食物成分表参考"),
            FoodReference("腐竹（干）", 461.0, 22.3, 44.6, 21.7, "中国食物成分表参考"),
            FoodReference("绿豆（干）", 329.0, 62.0, 21.6, 0.8, "中国食物成分表参考"),
            FoodReference("红小豆（干）", 324.0, 63.4, 20.2, 0.6, "中国食物成分表参考"),

            FoodReference("莲藕（生）", 47.0, 11.5, 1.2, 0.2, "中国食物成分表参考"),
            FoodReference("山药（生）", 57.0, 12.4, 1.9, 0.2, "中国食物成分表参考"),
            FoodReference("南瓜（生）", 23.0, 5.3, 0.7, 0.1, "中国食物成分表参考"),
            FoodReference("白萝卜（生）", 16.0, 4.0, 0.7, 0.1, "中国食物成分表参考"),
            FoodReference("大白菜（生）", 20.0, 3.4, 1.6, 0.2, "中国食物成分表参考"),
            FoodReference("小白菜（生）", 17.0, 2.7, 1.5, 0.3, "中国食物成分表参考"),
            FoodReference("黄瓜（生）", 16.0, 2.9, 0.8, 0.2, "中国食物成分表参考"),
            FoodReference("番茄（生）", 15.0, 3.3, 0.9, 0.2, "中国食物成分表参考"),
            FoodReference("茄子（生）", 23.0, 4.9, 1.1, 0.2, "中国食物成分表参考"),
            FoodReference("青椒（生）", 22.0, 5.4, 1.0, 0.2, "中国食物成分表参考"),
            FoodReference("香菇（鲜）", 26.0, 5.2, 2.2, 0.3, "中国食物成分表参考"),
            FoodReference("金针菇（鲜）", 32.0, 6.0, 2.4, 0.4, "中国食物成分表参考"),
            FoodReference("海带（鲜）", 16.0, 3.0, 1.2, 0.1, "中国食物成分表参考"),

            FoodReference("梨", 51.0, 13.1, 0.3, 0.1, "中国食物成分表参考"),
            FoodReference("葡萄", 45.0, 10.3, 0.4, 0.3, "中国食物成分表参考"),
            FoodReference("桃", 42.0, 10.1, 0.6, 0.1, "中国食物成分表参考"),
            FoodReference("西瓜", 31.0, 6.8, 0.5, 0.3, "中国食物成分表参考"),
            FoodReference("猕猴桃", 61.0, 14.5, 0.8, 0.6, "中国食物成分表参考"),
            FoodReference("柚子", 42.0, 9.5, 0.8, 0.2, "中国食物成分表参考"),

            // 连锁餐饮代表值，仅用于内置搜索，不加入 defaultQuickFoods。
            // 不同门店、批次和配方可能有差异，以下均按每 100 克估算。
            FoodReference("肯德基（KFC）香辣鸡腿堡", 260.0, 27.0, 12.0, 11.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）新奥尔良烤鸡腿堡", 230.0, 28.0, 13.0, 7.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）劲脆鸡腿堡", 265.0, 27.0, 12.0, 12.0, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）老北京鸡肉卷", 235.0, 26.0, 11.0, 9.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）吮指原味鸡", 255.0, 8.0, 20.0, 15.0, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）香辣鸡翅", 310.0, 13.0, 19.0, 20.0, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）新奥尔良烤翅", 240.0, 9.0, 23.0, 12.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）黄金鸡块", 270.0, 17.0, 16.0, 15.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）薯条", 310.0, 41.0, 4.0, 14.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）葡式蛋挞", 360.0, 37.0, 6.0, 21.0, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）土豆泥", 105.0, 14.0, 2.0, 4.5, "肯德基公开营养资料参考"),
            FoodReference("肯德基（KFC）玉米棒", 105.0, 19.0, 3.5, 1.5, "肯德基公开营养资料参考"),

            FoodReference("麦当劳 巨无霸", 255.0, 21.0, 12.0, 13.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 麦辣鸡腿汉堡", 270.0, 24.0, 13.0, 13.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 板烧鸡腿堡", 225.0, 25.0, 14.0, 7.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 双层吉士汉堡", 265.0, 19.0, 15.0, 14.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 吉士汉堡", 260.0, 25.0, 13.0, 12.0, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 麦香鸡", 250.0, 26.0, 10.5, 11.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 麦香鱼", 240.0, 27.0, 10.5, 10.0, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 麦乐鸡", 260.0, 16.0, 17.0, 14.0, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 麦辣鸡翅", 315.0, 13.0, 20.0, 20.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 薯条", 310.0, 41.0, 3.5, 15.0, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 脆薯饼", 325.0, 32.0, 3.0, 20.5, "麦当劳公开营养资料参考"),
            FoodReference("麦当劳 圆筒冰淇淋", 170.0, 28.0, 4.0, 4.5, "麦当劳公开营养资料参考")
        )
    }
}
