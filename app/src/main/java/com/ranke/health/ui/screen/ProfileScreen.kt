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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranke.health.R
import com.ranke.health.model.BiologicalSex
import com.ranke.health.model.HealthProfile
import com.ranke.health.ui.components.FitText
import com.ranke.health.ui.i18n.AppLanguage
import com.ranke.health.ui.i18n.isEnglishUi
import com.ranke.health.ui.i18n.label
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
import com.ranke.health.ui.theme.Surface

@Composable
internal fun ProfileScreen(
    profile: HealthProfile,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
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
            Text(stringResource(R.string.profile_title), color = Ink, fontSize = AppPageTitleSize, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.profile_sub), color = Muted, fontSize = AppBodySize)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    EditorialSectionTitle(stringResource(R.string.profile_body))
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
                                FitText(
                                    text = option.label(),
                                    maxFontSize = 12.sp,
                                    minFontSize = 9.sp,
                                    color = if (selected) Background else Muted,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileNumberField(stringResource(R.string.profile_height), height, { height = it }, Modifier.weight(1f), ImeAction.Next)
                        ProfileNumberField(stringResource(R.string.profile_weight), weight, { weight = it }, Modifier.weight(1f), ImeAction.Next)
                    }
                    ProfileNumberField(stringResource(R.string.profile_age), age, { age = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), ImeAction.Done)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileMetric("BMI", "%.1f".format(calculated.bmi), calculated.bmiStatus.label(), Modifier.weight(1f))
                ProfileMetric(stringResource(R.string.profile_bmr), "%,.0f".format(calculated.basalMetabolicRate), stringResource(R.string.profile_kcal_per_day), Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditorialSectionTitle(stringResource(R.string.profile_reference_kcal))
                    CalorieResultRow(stringResource(R.string.profile_sedentary), calculated.sedentaryExpenditure, stringResource(R.string.profile_bmr_div))
                    CalorieResultRow(
                        stringResource(R.string.profile_training_balance),
                        calculated.trainingBalanceKcal,
                        stringResource(R.string.profile_incl_lifting).format(calculated.strengthTrainingKcal)
                    )
                    CalorieResultRow(stringResource(R.string.profile_rest_balance), calculated.restBalanceKcal, stringResource(R.string.profile_no_extra))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Outline))
                    CalorieResultRow(
                        stringResource(R.string.profile_training_intake),
                        calculated.trainingIntakeKcal,
                        stringResource(R.string.profile_balance_times).format(calculated.intakeFactor * 100),
                        Lime
                    )
                    CalorieResultRow(
                        stringResource(R.string.profile_rest_intake),
                        calculated.restIntakeKcal,
                        stringResource(R.string.profile_balance_times).format(calculated.intakeFactor * 100),
                        Lime
                    )
                    Text(stringResource(R.string.profile_bmr_note), color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditorialSectionTitle(stringResource(R.string.profile_diet_ref))
                    Text(stringResource(R.string.profile_per_kg), color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorialCompactField(stringResource(R.string.profile_training_carb), trainingCarbsPerKg, { trainingCarbsPerKg = it }, Modifier.weight(1f))
                        EditorialCompactField(stringResource(R.string.profile_rest_carb), restCarbsPerKg, { restCarbsPerKg = it }, Modifier.weight(1f))
                        EditorialCompactField(stringResource(R.string.profile_protein), proteinPerKg, { proteinPerKg = it }, Modifier.weight(1f))
                    }
                    EditorialMacroTable(calculated)
                    Text(stringResource(R.string.profile_macro_note), color = Muted, fontSize = 10.sp)
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
        // 语言切换固定放在页面最底部，使用与“身体数据”男/女选择器同构的胶囊分段控件。
        item {
            LanguageCard(language = language, onLanguageChange = onLanguageChange)
        }
    }
}

@Composable
internal fun LanguageCard(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            EditorialSectionTitle(stringResource(R.string.profile_language))
            Row(
                Modifier.fillMaxWidth().border(1.dp, Outline, CircleShape).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                AppLanguage.entries.forEach { option ->
                    val selected = language == option
                    Box(
                        Modifier.weight(1f).clip(CircleShape)
                            .background(if (selected) Lime else Color.Transparent)
                            .clickable { if (!selected) onLanguageChange(option) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 三等分后每格约 100dp，"跟随系统"与 "System" 都能完整显示。
                        FitText(
                            text = option.label(),
                            maxFontSize = 12.sp,
                            minFontSize = 8.sp,
                            color = if (selected) Background else Muted,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Text(stringResource(R.string.profile_language_hint), color = Muted, fontSize = 10.sp)
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
internal fun EditorialCompactField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 10.sp, maxLines = 1)
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
    // 英文行标签（Training / Rest）比中文长，列宽按语言放宽，不挤压数值。
    val labelWidth = if (isEnglishUi()) 80.dp else 72.dp
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
            Spacer(Modifier.width(labelWidth))
            Text(stringResource(R.string.profile_carbs_col), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.profile_protein), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        }
        listOf(
            Triple(stringResource(R.string.profile_training_row), profile.trainingCarbohydratesGrams, profile.dailyProteinGrams),
            Triple(stringResource(R.string.profile_rest_row), profile.restCarbohydratesGrams, profile.dailyProteinGrams)
        ).forEachIndexed { index, row ->
            if (index > 0) EditorialRule()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FitText(
                    text = row.first,
                    maxFontSize = 13.sp,
                    minFontSize = 10.sp,
                    modifier = Modifier.width(labelWidth),
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
                Text("%.0fg".format(row.second), color = if (index == 0) Lime else Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                Text("%.0fg".format(row.third), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun ProfileNumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, imeAction: ImeAction) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, maxLines = 1) }, modifier = modifier,
        singleLine = true,
        shape = AppControlShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction)
    )
}

@Composable
internal fun ProfileMetric(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(17.dp)) {
            Text(title, color = Muted, fontSize = 12.sp, maxLines = 1)
            FitText(value, maxFontSize = 28.sp, minFontSize = 18.sp, color = Lime, fontWeight = FontWeight.Black)
            Text(subtitle, color = Ink, fontSize = 12.sp, maxLines = 1)
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
        Spacer(Modifier.width(8.dp))
        Text("%,.0f kcal".format(kcal), color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
