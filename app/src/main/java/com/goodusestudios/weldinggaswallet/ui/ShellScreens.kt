package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goodusestudios.weldinggaswallet.R
import com.goodusestudios.weldinggaswallet.data.BackupUiState
import com.goodusestudios.weldinggaswallet.data.BillingStatus
import com.goodusestudios.weldinggaswallet.data.BillingUiState
import com.goodusestudios.weldinggaswallet.localization.GoodUseCommonLocalization
import com.goodusestudios.weldinggaswallet.localization.rememberGoodUseLabelResolver
import com.goodusestudios.weldinggaswallet.wallet.WalletStore
import androidx.compose.material3.OutlinedTextField
import java.util.Locale

@Composable
fun OnboardingScreen(
    config: OnboardingConfig,
    legal: LegalConfig,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onComplete: () -> Unit,
) {
    val pages = if (config.presentation == OnboardingPresentation.SinglePage) config.pages.take(1) else config.pages
    var page by rememberSaveable { mutableIntStateOf(0) }
    var legalAccepted by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val layoutMode = onboardingLayoutModeFor(maxHeight.value.toInt(), LocalDensity.current.fontScale)
        val anchoredTopGap = if (maxHeight >= 900.dp) 56.dp else 40.dp
        val contentModifier = Modifier
            .align(Alignment.Center)
            .fillMaxHeight()
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp)

        if (layoutMode == OnboardingLayoutMode.Scrollable) {
            Column(contentModifier.verticalScroll(rememberScrollState())) {
                OnboardingHeader(config.showBrandMark)
                Spacer(Modifier.height(24.dp))
                OnboardingPageContent(pages[page])
                Spacer(Modifier.height(32.dp))
                OnboardingActions(page, pages.lastIndex, config.requireLegalAcceptance, legalAccepted, legal.version,
                    onAccepted = { legalAccepted = it }, onPrivacy, onTerms, { page-- }) {
                    if (page < pages.lastIndex) page++ else onComplete()
                }
            }
        } else {
            Column(contentModifier) {
                OnboardingHeader(config.showBrandMark)
                Spacer(Modifier.height(anchoredTopGap))
                OnboardingPageContent(pages[page])
                Spacer(Modifier.weight(1f))
                OnboardingActions(page, pages.lastIndex, config.requireLegalAcceptance, legalAccepted, legal.version,
                    onAccepted = { legalAccepted = it }, onPrivacy, onTerms, { page-- }) {
                    if (page < pages.lastIndex) page++ else onComplete()
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(showBrandMark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showBrandMark) {
            Image(
                painterResource(R.drawable.ic_brand_mark),
                "${ShellConfig.appName} logo",
                Modifier.size(42.dp).testTag("shell-app-logo"),
            )
        }
        Text(ShellConfig.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(page.step, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(page.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(page.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingActions(
    page: Int,
    lastPage: Int,
    requiresAcceptance: Boolean,
    accepted: Boolean,
    legalVersion: Int,
    onAccepted: (Boolean) -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val finalPage = page == lastPage
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (finalPage && requiresAcceptance) {
            Row(
                Modifier.fillMaxWidth().clickable { onAccepted(!accepted) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = accepted, onCheckedChange = onAccepted)
                Text("I accept the Terms of Use and acknowledge the Privacy Policy (version $legalVersion).")
            }
        }
        Button(
            onClick = onContinue,
            enabled = !finalPage || !requiresAcceptance || accepted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (finalPage) "Get started" else "Continue") }
        if (page > 0) {
            OutlinedButton(onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onPrivacy) { Text("Privacy") }
            TextButton(onClick = onTerms) { Text("Terms") }
        }
    }
}

@Composable
fun LegalOnlyOnboardingScreen(
    legal: LegalConfig,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onAccept: () -> Unit,
) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Before you continue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Review the Privacy Policy and Terms of Use effective ${legal.effectiveDate}.")
            Row {
                TextButton(onClick = onPrivacy) { Text("Privacy policy") }
                TextButton(onClick = onTerms) { Text("Terms of use") }
            }
            Row(
                Modifier.fillMaxWidth().clickable { accepted = !accepted },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(accepted, { accepted = it })
                Text("I accept version ${legal.version}.")
            }
            Button(onClick = onAccept, enabled = accepted, modifier = Modifier.fillMaxWidth()) {
                Text("Accept and continue")
            }
        }
    }
}

@Composable
fun LegalUpdateScreen(legal: LegalConfig, onPrivacy: () -> Unit, onTerms: () -> Unit, onAccept: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Terms updated", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Review the documents effective ${legal.effectiveDate}. Acceptance is stored as legal version ${legal.version}, so a future version can ask again.")
            Row {
                TextButton(onClick = onPrivacy) { Text("Privacy policy") }
                TextButton(onClick = onTerms) { Text("Terms of use") }
            }
            Row(
                Modifier.fillMaxWidth().clickable { accepted = !accepted },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(accepted, { accepted = it })
                Text("I accept version ${legal.version}.")
            }
            Button(onClick = onAccept, enabled = accepted, modifier = Modifier.fillMaxWidth()) { Text("Accept and continue") }
        }
    }
}

@Composable
fun FeatureScreen(destination: String, state: SampleContentState, expanded: Boolean, onRetry: () -> Unit) {
    when (state) {
        SampleContentState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        SampleContentState.Empty -> StateMessage("Nothing here yet", "Your app’s primary empty state belongs here.")
        SampleContentState.Error -> StateMessage("Couldn’t load content", "Keep the explanation human and offer one clear recovery action.", "Try again", onRetry)
        SampleContentState.Populated -> {
            val content = (1..8).map { "${destination.replaceFirstChar(Char::uppercase)} item $it" }
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    FeatureList(content, Modifier.weight(0.42f))
                    Surface(Modifier.weight(0.58f).fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        StateMessage("Select an item", "A list-detail layout uses tablet space without merely stretching the phone UI.")
                    }
                }
            } else FeatureList(content, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun FeatureList(items: List<String>, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 8.dp)) {
        item {
            Column(
                Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("This is the replaceable feature area. Shell chrome stays untouched.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item) },
                supportingContent = { Text("Useful supporting information") },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun StateMessage(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun SettingsScreen(
    monetizationMode: MonetizationMode,
    privacyOptionsRequired: Boolean,
    showLab: Boolean,
    onUpgrade: () -> Unit,
    onIcons: () -> Unit,
    onLanguage: () -> Unit,
    walletStore: WalletStore,
    onCurrency: () -> Unit,
    onHelp: () -> Unit,
    onPrivacyOptions: () -> Unit,
    backupAvailable: Boolean,
    backupProviderName: String,
    onBackup: () -> Unit,
    onLab: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSupport: () -> Unit,
) {
    val label = rememberGoodUseLabelResolver()
    val wallet by walletStore.state.collectAsStateWithLifecycle()
    var deleteOpen by remember { mutableStateOf(false) }
    var deletePhrase by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        if (monetizationMode.requiredProductKind != null) {
            item { SettingsRow("Upgrade", "Purchase or restore access", Icons.Outlined.LockOpen, onUpgrade) }
        }
        item { SettingsRow(label("common.language"), "Choose the app language", Icons.Outlined.Language, onLanguage) }
        item { SettingsRow("Currency", "${walletStore.currencySign(walletStore.defaultCurrency)} · ${if (wallet.currencyOverride == null) "Automatic" else "Selected"}", Icons.Outlined.CurrencyExchange, onCurrency) }
        if (privacyOptionsRequired) {
            item { SettingsRow("Ad privacy choices", "Review or withdraw advertising consent", Icons.Outlined.PrivacyTip, onPrivacyOptions) }
        }
        item { SettingsRow("Backup", "Optional native file backup", Icons.Outlined.Backup, onBackup) }
        item { SettingsRow("Help", "Simple numbered guide", Icons.Outlined.HelpOutline, onHelp) }
        item { SettingsRow(label("common.support"), ShellConfig.supportEmail, Icons.Outlined.SupportAgent, onSupport) }
        item { SettingsRow(label("common.privacyPolicy"), "Opens in your browser", Icons.Outlined.PrivacyTip, onPrivacy) }
        item { SettingsRow(label("common.termsOfUse"), "Opens in your browser", Icons.Outlined.Description, onTerms) }
        item { SettingsRow("Delete all data", "Erase this wallet from this device", Icons.Outlined.DeleteOutline) { deleteOpen = true } }
        if (showLab) item { SettingsRow("Shell Lab", "Exercise every reusable state", Icons.Outlined.Science, onLab) }
        item {
            Text(
                "Welding Gas Wallet · Cylinder records stay on this device",
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false; deletePhrase = "" },
            title = { Text("Delete all data?") },
            text = { Column { Text("This removes cylinders, suppliers, costs, activity, reminders and preferences. Purchases and separately saved backup files are not deleted."); Spacer(Modifier.height(12.dp)); OutlinedTextField(deletePhrase, { deletePhrase = it }, label = { Text("Type DELETE to confirm") }) } },
            confirmButton = { TextButton({ walletStore.deleteAllData(); deleteOpen = false; deletePhrase = "" }, enabled = deletePhrase.trim().equals("DELETE", true)) { Text("Delete all data", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleteOpen = false; deletePhrase = "" }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
fun BackupScreen(
    providerName: String,
    state: BackupUiState,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Icon(Icons.Outlined.Cloud, null)
            Spacer(Modifier.height(12.dp))
            Text("Optional backup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Backups use ${providerName}. The shell never enables cloud storage or account collection unless a derived app supplies and documents a provider.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Button(onClick = onCreate, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                Text("Create backup")
            }
        }
        item {
            OutlinedButton(onClick = onRestore, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
                Text("Restore latest backup")
            }
        }
        if (state.working) item { CircularProgressIndicator() }
        state.message?.let { message -> item { Text(message) } }
    }
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val choices = remember { listOf(
        "en" to "English", "es-419" to "Español (Latinoamérica)", "pt-BR" to "Português (Brasil)", "fr" to "Français", "ar" to "العربية", "hi" to "हिन्दी", "bn" to "বাংলা", "ur" to "اردو", "id" to "Bahasa Indonesia", "vi" to "Tiếng Việt", "ja" to "日本語", "ru" to "Русский", "zh-Hans" to "简体中文", "zh-Hant" to "繁體中文", "ms" to "Bahasa Melayu", "ta" to "தமிழ்", "te" to "తెలుగు", "mr" to "मराठी", "pa-Guru" to "ਪੰਜਾਬੀ", "gu" to "ગુજરાતી", "kn" to "ಕನ್ನಡ", "ml" to "മലയാളം", "th" to "ไทย", "ko" to "한국어", "fil" to "Filipino", "fa" to "فارسی", "sw" to "Kiswahili", "ha" to "Hausa", "am" to "አማርኛ", "tr" to "Türkçe"
    ) }
    val current = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Language") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(choices) { (tag, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                androidx.core.os.LocaleListCompat.forLanguageTags(tag),
                            )
                            onDismiss()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == tag, onClick = null)
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
fun LabScreen(
    mode: MonetizationMode,
    state: SampleContentState,
    onMode: (MonetizationMode) -> Unit,
    onState: (SampleContentState) -> Unit,
    onRemoveAds: () -> Unit,
    onResetOnboarding: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Monetization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MonetizationMode.entries.forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == value, onClick = { onMode(value) })
                    Text(value.readableName())
                }
            }
        }
        item {
            Text("Feature state", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SampleContentState.entries.forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state == value, onClick = { onState(value) })
                    Text(value.name)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRemoveAds, modifier = Modifier.fillMaxWidth()) { Text("Toggle entitlement") }
                OutlinedButton(onClick = onResetOnboarding, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset onboarding")
                }
            }
        }
    }
}

@Composable
fun PaywallScreen(
    billing: BillingUiState,
    benefits: List<String>,
    onRetry: () -> Unit,
    onRestore: () -> Unit,
    onPurchase: (String) -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(billing.products) {
        if (selectedId !in billing.products.map { it.id }) {
            selectedId = billing.products.firstOrNull { it.available }?.id ?: billing.products.firstOrNull()?.id
        }
    }
    val selected = billing.products.firstOrNull { it.id == selectedId }
    LazyColumn(
        modifier = Modifier.testTag("shell-paywall"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Make the useful thing unlimited.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Choose a Play product. Pricing and eligibility come directly from Google Play when the product is active.")
        }
        items(benefits) { value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(value)
            }
        }
        items(billing.products, key = { it.id }) { product ->
            Card(
                Modifier.fillMaxWidth().clickable(enabled = product.available) { selectedId = product.id },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(product.id == selectedId, onClick = { selectedId = product.id }, enabled = product.available)
                    Column(Modifier.weight(1f)) {
                        Text(product.title, fontWeight = FontWeight.SemiBold)
                        Text(product.formattedPrice, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!product.available) Text("Unavailable in this build", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (billing.working) item { CircularProgressIndicator() }
        billing.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (billing.status == BillingStatus.Unavailable) {
            item { OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Reconnect to Google Play") } }
        }
        selected?.let { product ->
            item {
                Text(
                    if (product.kind == StoreProductKind.Subscription) {
                        "Subscription renews automatically at ${product.formattedPrice} for the displayed billing period until cancelled in Google Play."
                    } else {
                        "One-time purchase. This charge does not recur."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Button(
                onClick = { selected?.id?.let(onPurchase) },
                enabled = selected?.available == true && !billing.working,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shell-purchase"),
            ) { Text(if (billing.entitled) "Access active" else "Continue · ${selected?.formattedPrice ?: "choose a product"}") }
        }
        item {
            TextButton(
                onClick = onRestore,
                enabled = !billing.working,
                modifier = Modifier.fillMaxWidth().testTag("shell-restore"),
            ) { Text("Restore purchases") }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onPrivacy, modifier = Modifier.testTag("shell-privacy")) { Text("Privacy") }
                TextButton(onClick = onTerms, modifier = Modifier.testTag("shell-terms")) { Text("Terms") }
            }
        }
    }
}

@Composable
fun LegalScreen(title: String, body: String, effectiveDate: String, onOpenWeb: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Effective $effectiveDate", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Spacer(Modifier.height(16.dp)) }
        item { Text(body, style = MaterialTheme.typography.bodyLarge) }
        item { Spacer(Modifier.height(16.dp)) }
        item { OutlinedButton(onClick = onOpenWeb) { Text("Open published document") } }
    }
}

@Composable
fun LegalDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(title) },
        text = { Text(body) },
    )
}

private fun MonetizationMode.readableName() = when (this) {
    MonetizationMode.Free -> "Free"
    MonetizationMode.Ads -> "Ads"
    MonetizationMode.AdsWithRemovePurchase -> "Ads + remove purchase"
    MonetizationMode.AdsWithSubscription -> "Ads + subscription"
    MonetizationMode.OneTimeUnlock -> "One-time unlock"
    MonetizationMode.Subscription -> "Subscription"
    MonetizationMode.UsageCapWithOneTimeUnlock -> "Usage cap + one-time unlock"
    MonetizationMode.UsageCapWithSubscription -> "Usage cap + subscription"
}
