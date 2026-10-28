package com.ranke.health.ui.screen


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranke.health.model.BiologicalSex
import com.ranke.health.model.HealthProfile
import com.ranke.health.ui.theme.AppBodySize
import com.ranke.health.ui.theme.AppCardPadding
import com.ranke.health.ui.theme.AppCardShape
import com.ranke.health.ui.theme.AppControlShape
import com.ranke.health.ui.theme.AppPagePadding
import com.ranke.health.ui.theme.AppPageTitleSize
import com.ranke.health.ui.theme.AppSectionGap
import com.ranke.health.ui.theme.AppSectionTitleSize
import com.ranke.health.ui.theme.Background
import com.ranke.health.ui.theme.CardOutline
import com.ranke.health.ui.theme.Ink
import com.ranke.health.ui.theme.Lime
import com.ranke.health.ui.theme.Muted
import com.ranke.health.ui.theme.Outline
import com.ranke.health.ui.theme.Raised
import com.ranke.health.ui.theme.Surface

@Composable
internal fun ProfileScreen(
    profile: HealthProfile,
    onSave: (HealthProfile) -> Unit,
    onSaveData: () -> Unit,
    onRestoreData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var height by remember(profile) { mutableStateOf("%.1f".format(profile.heightCm)) }
    var weight by remember(profile) { mutableStateOf("%.1f".format(profile.weightKg)) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var sex by remember(profile) { mutableStateOf(profile.sex) }
    var trainingCarbsPerKg by remember(profile) { mutableStateOf("%.1f".format(profile.trainingCarbsPerKg)) }
    var restCarbsPerKg by remember(profile) { mutableStateOf("%.1f".format(profile.restCarbsPerKg)) }
    var proteinPerKg by remember(profile) { mutableStateOf("%.1f".format(profile.proteinPerKg)) }
    val calculated = profile.copy(
        heightCm = height.toDoubleOrNull()?.coerceIn(100.0, 250.0) ?: profile.heightCm,
        weightKg = weight.toDoubleOrNull()?.coerceIn(30.0, 300.0) ?: profile.weightKg,
        age = age.toIntOrNull()?.coerceIn(15, 100) ?: profile.age,
        sex = sex,
        trainingCarbsPerKg = trainingCarbsPerKg.toDoubleOrNull()?.coerceIn(.1, 10.0) ?: profile.trainingCarbsPerKg,
        restCarbsPerKg = restCarbsPerKg.toDoubleOrNull()?.coerceIn(.1, 10.0) ?: profile.restCarbsPerKg,
        proteinPerKg = proteinPerKg.toDoubleOrNull()?.coerceIn(.1, 5.0) ?: profile.proteinPerKg
    )
    val inputValid = height.toDoubleOrNull()?.let { it in 100.0..250.0 } == true &&
        weight.toDoubleOrNull()?.let { it in 30.0..300.0 } == true &&
        age.toIntOrNull()?.let { it in 15..100 } == true &&
        trainingCarbsPerKg.toDoubleOrNull()?.let { it in .1..10.0 } == true &&
        restCarbsPerKg.toDoubleOrNull()?.let { it in .1..10.0 } == true &&
        proteinPerKg.toDoubleOrNull()?.let { it in .1..5.0 } == true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = AppPagePadding,
        verticalArrangement = Arrangement.spacedBy(AppSectionGap)
    ) {
        item {
            Text("我", color = Ink, fontSize = AppPageTitleSize, fontWeight = FontWeight.Black)
            Text("调整身体数据，参考指标与热量会自动更新", color = Muted, fontSize = AppBodySize)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    EditorialSectionTitle("身体数据")
                    Row(
                        Modifier.fillMaxWidth().border(1.dp, Outline, CircleShape).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        BiologicalSex.entries.forEach { option ->
                            val selected = sex == option
                            Box(
                                Modifier.weight(1f).clip(CircleShape)
                                    .background(if (selected) Lime else Color.Transparent)
                                    .clickable { sex = option }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(option.label, color = if (selected) Background else Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileNumberField("身高 (cm)", height, { height = it }, Modifier.weight(1f), ImeAction.Next)
                        ProfileNumberField("体重 (kg)", weight, { weight = it }, Modifier.weight(1f), ImeAction.Next)
                    }
                    ProfileNumberField("年龄", age, { age = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), ImeAction.Done)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileMetric("BMI", "%.1f".format(calculated.bmi), calculated.bmiLabel, Modifier.weight(1f))
                ProfileMetric("基础代谢", "%,.0f".format(calculated.basalMetabolicRate), "kcal / 天", Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditorialSectionTitle("参考热量")
                    CalorieResultRow("无运动总消耗", calculated.sedentaryExpenditure, "基础代谢 ÷ 0.7")
                    CalorieResultRow("训练日平衡热量", calculated.trainingBalanceKcal, "含力训 %.0f kcal".format(calculated.strengthTrainingKcal))
                    CalorieResultRow("休息日平衡热量", calculated.restBalanceKcal, "无额外训练")
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Outline))
                    CalorieResultRow("训练日应吃", calculated.trainingIntakeKcal, "平衡热量 × %.0f%%".format(calculated.intakeFactor * 100), Lime)
                    CalorieResultRow("休息日应吃", calculated.restIntakeKcal, "平衡热量 × %.0f%%".format(calculated.intakeFactor * 100), Lime)
                    Text("基础代谢采用 Mifflin-St Jeor 方程，仅供参考，不影响首页计划。", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditorialSectionTitle("饮食参考")
                    Text("每公斤参考量", color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorialCompactField("力训碳水", trainingCarbsPerKg, { trainingCarbsPerKg = it }, Modifier.weight(1f))
                        EditorialCompactField("休息碳水", restCarbsPerKg, { restCarbsPerKg = it }, Modifier.weight(1f))
                        EditorialCompactField("蛋白质", proteinPerKg, { proteinPerKg = it }, Modifier.weight(1f))
                    }
                    EditorialMacroTable(calculated)
                    Text("以上结果随体重实时变化，仅供参考，不会修改首页已有计划。", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            SaveCenterCard(
                onSaveData = {
                    onSave(calculated)
                    onSaveData()
                },
                saveEnabled = inputValid,
                onRestoreData = onRestoreData
            )
        }
    }
}

@Composable
internal fun EditorialRule() = Box(Modifier.fillMaxWidth().height(1.dp).background(CardOutline))

@Composable
internal fun EditorialSectionTitle(title: String) {
    Text(title, color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
}

@Composable
internal fun EditorialNumberField(label: String, value: String, unit: String, onChange: (String) -> Unit, imeAction: ImeAction) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.width(42.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            suffix = { Text(unit, color = Muted, fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
            modifier = Modifier.weight(1f).height(56.dp)
        )
    }
}

@Composable
internal fun EditorialMetric(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Lime, fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = if (subtitle == "正常") Lime else Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun EditorialCalorieLine(label: String, value: Double, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (highlight) Ink else Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("%,.0f".format(value), color = if (highlight) Lime else Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(" kcal", color = if (highlight) Lime else Muted, fontSize = 9.sp)
    }
}

@Composable
internal fun CalorieBars(trainingBalance: Double, trainingIntake: Double, restBalance: Double, restIntake: Double, modifier: Modifier = Modifier) {
    val max = maxOf(trainingBalance, restBalance).coerceAtLeast(1.0)
    Row(modifier.height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        listOf("训练日" to (trainingBalance to trainingIntake), "休息日" to (restBalance to restIntake)).forEach { (label, values) ->
            val totalHeight = (100 * values.first / max).dp
            val intakeHeight = (100 * values.second / max).dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(28.dp).height(totalHeight).background(Raised, AppControlShape), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().height(intakeHeight).background(Lime, AppControlShape))
                }
                Spacer(Modifier.height(5.dp))
                Text("%,.0f".format(values.second), color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun EditorialCompactField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 10.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            suffix = { Text("g/kg", color = Muted, fontSize = 8.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
internal fun EditorialMacroTable(profile: HealthProfile) {
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
            Text("", modifier = Modifier.width(72.dp))
            Text("碳水", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("蛋白质", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        }
        listOf(
            Triple("力训日", profile.trainingCarbohydratesGrams, profile.dailyProteinGrams),
            Triple("休息日", profile.restCarbohydratesGrams, profile.dailyProteinGrams)
        ).forEachIndexed { index, row ->
            if (index > 0) EditorialRule()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.first, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                Text("%.0fg".format(row.second), color = if (index == 0) Lime else Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("%.0fg".format(row.third), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun EditorialSaveCenter(onSaveData: () -> Unit, saveEnabled: Boolean, onRestoreData: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp)) {
        EditorialSectionTitle("保存中心")
        Spacer(Modifier.height(5.dp))
        Text("完整备份身体设置与全部记录", color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSaveData,
                enabled = saveEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Background),
                shape = AppControlShape,
                modifier = Modifier.weight(1.2f).height(50.dp)
            ) { Icon(Icons.Rounded.Save, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("保存全部数据", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = onRestoreData,
                modifier = Modifier.weight(.9f).height(50.dp).border(1.dp, Lime, AppControlShape),
                colors = ButtonDefaults.textButtonColors(contentColor = Lime)
            ) { Icon(Icons.Rounded.Download, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("恢复数据") }
        }
    }
}

@Composable
internal fun MacroReferenceHeader() {
    Row(Modifier.fillMaxWidth()) {
        Text("类型", color = Muted, fontSize = 11.sp, modifier = Modifier.width(62.dp))
        Text("碳水", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("蛋白质", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun MacroReferenceRow(day: String, carbohydrates: Double, carbohydrateRule: String, protein: Double, proteinRule: String = "1.4 g/kg") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(day, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
        Column(Modifier.weight(1f)) {
            Text("%.0f g".format(carbohydrates), color = Lime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(carbohydrateRule, color = Muted, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("%.0f g".format(protein), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(proteinRule, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun ProfileNumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, imeAction: ImeAction) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, modifier = modifier,
        singleLine = true,
        shape = AppControlShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction)
    )
}

@Composable
internal fun ProfileMetric(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(17.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = Lime, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Ink, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun CalorieResultRow(label: String, kcal: Double, note: String, valueColor: Color = Ink) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = Muted, fontSize = 11.sp)
        }
        Text("%,.0f kcal".format(kcal), color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
