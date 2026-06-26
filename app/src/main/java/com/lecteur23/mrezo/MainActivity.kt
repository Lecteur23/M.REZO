package com.lecteur23.mrezo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lecteur23.mrezo.ui.theme.MREZOTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "mrezo_security"
private const val KEY_SECRET_CODE = "secret_code"
private const val KEY_INTRO_DONE = "intro_done"
private const val KEY_TERMINAL_NETWORKS = "terminal_networks"
private const val KEY_HISTORY = "history"
private const val KEY_USSD_STEPS = "ussd_steps"
private const val KEY_USSD_STEP_INDEX = "ussd_step_index"
private const val KEY_USSD_RUNNING = "ussd_running"
private const val KEY_USSD_RESULT_MESSAGE = "ussd_result_message"
private const val KEY_USSD_RESULT_STATUS = "ussd_result_status"
private const val REQUEST_PHONE_PERMISSIONS = 80
private const val LOCK_DELAY_MS = 30 * 60 * 1000L
private val HISTORY_RETENTION_MS = TimeUnit.DAYS.toMillis(14)

val BrandBlue = Color(0xFF00187C)
private val Night = Color(0xFF07111F)
val Panel = Color(0xFF101A2D)
val PanelSoft = Color(0xFF16223A)
val TextSoft = Color(0xFF9CA8BE)
val Success = Color(0xFF22C55E)

class MainActivity : ComponentActivity() {
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }
    private var permissionRefresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MREZOTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Night
                ) {
                    val refreshTick = permissionRefresh
                    MRezoApp(
                        initialSecretCode = prefs.getString(KEY_SECRET_CODE, null),
                        introDone = prefs.getBoolean(KEY_INTRO_DONE, false),
                        initialTerminalNetworks = prefs.getString(KEY_TERMINAL_NETWORKS, null).toOperatorSet(),
                        initialNotifications = loadHistory(),
                        phonePermissionsGranted = refreshTick >= 0 && hasPhonePermissions(),
                        overlayPermissionGranted = MRezoOverlayController.canDraw(this@MainActivity),
                        ussdAccessibilityEnabled = isMRezoUssdServiceEnabled(),
                        onSaveSecretCode = { code ->
                            prefs.edit().putString(KEY_SECRET_CODE, code).apply()
                        },
                        onSaveIntroDone = {
                            prefs.edit().putBoolean(KEY_INTRO_DONE, true).apply()
                        },
                        onSaveTerminalNetworks = { operators ->
                            prefs.edit()
                                .putString(KEY_TERMINAL_NETWORKS, operators.joinToString(",") { it.name })
                                .apply()
                        },
                        onClearUserData = {
                            prefs.edit()
                                .remove(KEY_SECRET_CODE)
                                .remove(KEY_INTRO_DONE)
                                .remove(KEY_TERMINAL_NETWORKS)
                                .remove(KEY_HISTORY)
                                .apply()
                        },
                        onRequestPhonePermissions = {
                            requestPhonePermissions()
                            permissionRefresh++
                        },
                        onOpenOverlaySettings = {
                            openOverlaySettings()
                            permissionRefresh++
                        },
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            permissionRefresh++
                        },
                        onRefreshPermissions = { permissionRefresh++ },
                        onSaveNotifications = ::saveHistory,
                        onConsumeUssdResult = ::consumeUssdResult,
                        onRunOfferUssd = ::runOfferUssd,
                        onRunBalanceUssd = ::runBalanceUssd
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh++
    }

    private fun hasPhonePermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPhonePermissions() {
        val permissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PHONE_PERMISSIONS)
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun runBalanceUssd(
        operator: OperatorBrand,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ussdCode = when (operator) {
            OperatorBrand.MTN -> "#100#"
            OperatorBrand.ORANGE -> "#122#"
            OperatorBrand.MOOV -> null
        }

        if (ussdCode == null) {
            onError("Code solde ${operator.label} a renseigner.")
            return
        }

        runUssdCode(
            operator = operator,
            ussdCode = ussdCode,
            operationLabel = "consultation ${operator.label}",
            onResult = onResult,
            onError = onError
        )
    }

    private fun runOfferUssd(
        offer: Offer,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ussdRoot = offer.ussdRoot
        if (ussdRoot == null || offer.ussdSteps.isEmpty()) {
            onError("Ce forfait n'a pas encore de parcours USSD par etapes.")
            return
        }

        if (!isMRezoUssdServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            onError("Activez le service M-REZO USSD dans Accessibilite, puis relancez ce forfait.")
            return
        }

        if (!MRezoOverlayController.canDraw(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            onError("Autorisez M-REZO a s'afficher par-dessus les autres applications, puis relancez ce forfait.")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPhonePermissions()
            onError("Autorisez l'acces telephone puis relancez l'operation.")
            return
        }

        // AUTO-INJECTION: Remplacer {pin_cache} et {location_cache} par les valeurs securisees
        val pinStore = MRezoPinStore(this)
        val locationStore = MRezoLocationStore(this)
        val operatorPin = pinStore.getPinForOperator(offer.operator) ?: ""
        val operatorLocation = locationStore.getLocationForOperator(offer.operator) ?: ""

        val processedSteps = offer.ussdSteps.map { step ->
            step.replace("{pin_cache}", operatorPin)
                .replace("{location_cache}", operatorLocation)
        }

        prefs.edit()
            .putString(KEY_USSD_STEPS, processedSteps.joinToString("|"))
            .putInt(KEY_USSD_STEP_INDEX, 0)
            .putBoolean(KEY_USSD_RUNNING, true)
            .remove(KEY_USSD_RESULT_MESSAGE)
            .remove(KEY_USSD_RESULT_STATUS)
            .apply()

        try {
            MRezoOverlayController.show(this)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(ussdRoot)}"))
                    startActivity(callIntent)
                } catch (_: Exception) {
                    MRezoOverlayController.hide(this)
                    onError("Impossible de lancer le code $ussdRoot.")
                }
            }, 650L)
        } catch (_: Exception) {
            MRezoOverlayController.hide(this)
            onError("Impossible de lancer le code $ussdRoot.")
        }
    }

    private fun consumeUssdResult(): UssdSessionResult? {
        val message = prefs.getString(KEY_USSD_RESULT_MESSAGE, null) ?: return null
        val status = prefs.getString(KEY_USSD_RESULT_STATUS, "info").orEmpty()
        prefs.edit()
            .remove(KEY_USSD_RESULT_MESSAGE)
            .remove(KEY_USSD_RESULT_STATUS)
            .apply()
        return UssdSessionResult(message = message, isError = status == "error")
    }

    private fun isMRezoUssdServiceEnabled(): Boolean {
        val expected = "$packageName/${MRezoUssdAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
    private fun runUssdCode(
        operator: OperatorBrand,
        ussdCode: String,
        operationLabel: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPhonePermissions()
            onError("Autorisez l'acces telephone puis relancez l'operation.")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onError("Execution USSD automatique disponible a partir d'Android 8.")
            return
        }

        val telephonyManager = telephonyManagerForOperator(operator)
        telephonyManager.sendUssdRequest(
            ussdCode,
            object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager,
                    request: String,
                    response: CharSequence
                ) {
                    onResult(response.toString())
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager,
                    request: String,
                    failureCode: Int
                ) {
                    onError("Echec $operationLabel. Code erreur : $failureCode")
                }
            },
            Handler(Looper.getMainLooper())
        )
    }
    private fun loadHistory(): List<AppNotification> {
        val cutoff = System.currentTimeMillis() - HISTORY_RETENTION_MS
        val raw = prefs.getString(KEY_HISTORY, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val createdAtMillis = item.optLong("createdAtMillis", 0L)
                if (createdAtMillis < cutoff) return@mapNotNull null
                AppNotification(
                    title = item.optString("title"),
                    message = item.optString("message"),
                    timeLabel = formatHistoryDate(createdAtMillis),
                    createdAtMillis = createdAtMillis,
                    operator = item.optString("operator").takeIf { it.isNotBlank() }?.let { rawOperator ->
                        OperatorBrand.entries.find { it.name == rawOperator }
                    }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(history: List<AppNotification>) {
        val array = JSONArray()
        cleanHistory(history).forEach { notification ->
            array.put(
                JSONObject()
                    .put("title", notification.title)
                    .put("message", notification.message)
                    .put("createdAtMillis", notification.createdAtMillis)
                    .put("operator", notification.operator?.name.orEmpty())
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun telephonyManagerForOperator(operator: OperatorBrand): TelephonyManager {
        val defaultManager = getSystemService(TelephonyManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return defaultManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return defaultManager
        }

        return try {
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            val subscription = subscriptionManager.activeSubscriptionInfoList.orEmpty().firstOrNull { info ->
                val simLabel = "${info.carrierName} ${info.displayName}".lowercase()
                operator.simKeywords.any { keyword -> simLabel.contains(keyword) }
            }
            subscription?.let { defaultManager.createForSubscriptionId(it.subscriptionId) } ?: defaultManager
        } catch (_: SecurityException) {
            defaultManager
        }
    }
}

private enum class OfferType(val label: String) {
    INTERNET("Internet"),
    CALLS("Appels"),
    SMS("SMS"),
    COMBO("Combo")
}

private enum class AccountAction(
    val label: String,
    val resultMessage: String,
    val icon: ImageVector
) {
    COMMISSION("Commission", "Commission disponible : 12 500 FCFA. Message USSD recu et archive.", Icons.Filled.Payments),
    CONVERT("Convertir", "Conversion traitee. Le message final USSD est disponible dans l'historique.", Icons.Filled.SwapHoriz),
    BALANCE("Solde puce", "Solde puce consulte. Le message USSD final est disponible dans les notifications.", Icons.Filled.AccountBalanceWallet)
}

private data class UssdStep(
    val label: String,
    val value: String
)

private data class Offer(
    val operator: OperatorBrand,
    val type: OfferType,
    val name: String,
    val price: String,
    val validity: String,
    val executionSeconds: Int,
    val instruction: List<UssdStep>,
    val ussdRoot: String? = null,
    val ussdSteps: List<String> = emptyList()
)

private data class UssdSessionResult(
    val message: String,
    val isError: Boolean
)

private data class ProcessRequest(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val seconds: Int,
    val notificationTitle: String,
    val notificationMessage: String,
    val operator: OperatorBrand? = null,
    val autoComplete: Boolean = true
)

private data class AppNotification(
    val title: String,
    val message: String,
    val timeLabel: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val operator: OperatorBrand? = null
)

private fun String?.toOperatorSet(): Set<OperatorBrand> {
    if (isNullOrBlank()) return emptySet()
    return split(",")
        .mapNotNull { raw -> OperatorBrand.entries.find { it.name == raw } }
        .toSet()
}

private fun cleanHistory(history: List<AppNotification>): List<AppNotification> {
    val cutoff = System.currentTimeMillis() - HISTORY_RETENTION_MS
    return history
        .filter { it.operator != null && it.createdAtMillis >= cutoff }
        .sortedByDescending { it.createdAtMillis }
}

private fun formatHistoryDate(createdAtMillis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(createdAtMillis))
}

private val previewOffers = listOf(
    Offer(OperatorBrand.MTN, OfferType.INTERNET, "250 Mo", "100 FCFA", "1 jour", 12, hiddenInstruction("*123#", "2", "1")),
    Offer(OperatorBrand.MTN, OfferType.INTERNET, "1 Go", "300 FCFA", "7 jours", 14, hiddenInstruction("*123#", "2", "3")),
    Offer(OperatorBrand.MTN, OfferType.COMBO, "Test MTN *105#", "5000 FCFA", "Test", 30, menuInstruction("*105#", "2", "5", "1", "1"), ussdRoot = "*105#", ussdSteps = listOf("2", "5", "1", "1")),
    Offer(OperatorBrand.ORANGE, OfferType.INTERNET, "500 Mo", "150 FCFA", "1 jour", 13, hiddenInstruction("*144#", "2", "1")),
    Offer(OperatorBrand.ORANGE, OfferType.INTERNET, "1 Go", "400 FCFA", "7 jours", 15, hiddenInstruction("*144#", "2", "3")),
    Offer(OperatorBrand.ORANGE, OfferType.CALLS, "Appels Tous Reseaux", "500 FCFA", "3 jours", 12, hiddenInstruction("*144#", "3", "2")),
    Offer(OperatorBrand.MOOV, OfferType.INTERNET, "250 Mo", "100 FCFA", "1 jour", 12, hiddenInstruction("*155#", "2", "1")),
    Offer(OperatorBrand.MOOV, OfferType.INTERNET, "1 Go", "350 FCFA", "7 jours", 14, hiddenInstruction("*155#", "2", "3")),
    Offer(OperatorBrand.MOOV, OfferType.SMS, "SMS Illimites", "100 FCFA", "1 jour", 10, hiddenInstruction("*155#", "4", "1"))
)

private fun hiddenInstruction(root: String, menu: String, offer: String) = listOf(
    UssdStep("Composer", root),
    UssdStep("Menu", menu),
    UssdStep("Forfait", offer),
    UssdStep("Beneficiaire", "{numero_client}"),
    UssdStep("PIN", "{pin_cache}")
)

private fun menuInstruction(root: String, vararg steps: String) = listOf(
    UssdStep("Composer", root)
) + steps.mapIndexed { index, step -> UssdStep("Etape ${index + 1}", step) }

@Composable
private fun MRezoApp(
    initialSecretCode: String?,
    introDone: Boolean,
    initialTerminalNetworks: Set<OperatorBrand>,
    initialNotifications: List<AppNotification>,
    phonePermissionsGranted: Boolean,
    overlayPermissionGranted: Boolean,
    ussdAccessibilityEnabled: Boolean,
    onSaveSecretCode: (String) -> Unit,
    onSaveIntroDone: () -> Unit,
    onSaveTerminalNetworks: (Set<OperatorBrand>) -> Unit,
    onClearUserData: () -> Unit,
    onRequestPhonePermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onSaveNotifications: (List<AppNotification>) -> Unit,
    onConsumeUssdResult: () -> UssdSessionResult?,
    onRunOfferUssd: (Offer, (String) -> Unit, (String) -> Unit) -> Unit,
    onRunBalanceUssd: (OperatorBrand, (String) -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var secretCode by rememberSaveable { mutableStateOf(initialSecretCode) }
    var isLocked by rememberSaveable { mutableStateOf(initialSecretCode != null) }
    var showIntro by rememberSaveable { mutableStateOf(!introDone) }
    val requiredPermissionsReady = phonePermissionsGranted && overlayPermissionGranted && ussdAccessibilityEnabled
    var selectedTerminalNetworks by remember { mutableStateOf(initialTerminalNetworks) }
    var selectedOperator by remember { mutableStateOf<OperatorBrand?>(null) }
    var selectedOffer by remember { mutableStateOf<Offer?>(null) }
    var processingRequest by remember { mutableStateOf<ProcessRequest?>(null) }
    var completedRequest by remember { mutableStateOf<ProcessRequest?>(null) }
    var beneficiaryNumber by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var accountAction by remember { mutableStateOf<AccountAction?>(null) }
    var transferSheetOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var historyOperator by remember { mutableStateOf<OperatorBrand?>(null) }
    var appNotifications by remember {
        mutableStateOf(
            cleanHistory(initialNotifications).ifEmpty {
                listOf(
                    AppNotification(
                        title = "M-REZO",
                        message = "Les resultats USSD de M-REZO apparaitront ici.",
                        timeLabel = "Accueil"
                    )
                )
            }
        )
    }
    val recordNotification: (String, String, OperatorBrand?) -> Unit = { title, message, operator ->
        val now = System.currentTimeMillis()
        val nextHistory = cleanHistory(
            listOf(
                AppNotification(
                    title = title,
                    message = message,
                    timeLabel = formatHistoryDate(now),
                    createdAtMillis = now,
                    operator = operator
                )
            ) + appNotifications.filter { it.operator != null }
        )
        appNotifications = nextHistory
        onSaveNotifications(nextHistory)
    }

    val arePinsConfigured: () -> Boolean = {
        val pinStore = MRezoPinStore(context)
        selectedTerminalNetworks.all { operator ->
            !pinStore.getPinForOperator(operator).isNullOrBlank()
        }
    }
    LaunchedEffect(processingRequest) {
        val request = processingRequest
        if (request != null && !request.autoComplete) {
            while (processingRequest == request) {
                val result = onConsumeUssdResult()
                if (result != null) {
                    val message = result.message.ifBlank { "Aucune reponse finale recue." }
                    processingRequest = null
                    recordNotification(request.notificationTitle, message, request.operator)
                    completedRequest = request.copy(
                        title = if (result.isError) "Operation refusee" else request.title,
                        seconds = 1,
                        notificationMessage = message,
                        autoComplete = true
                    )
                    break
                }
                delay(1000)
            }
        }
    }

    val goHome: () -> Unit = {
        selectedOperator = null
        selectedOffer = null
        processingRequest = null
        completedRequest = null
        notificationsOpen = false
        historyOperator = null
        transferSheetOpen = false
        accountAction = null
        beneficiaryNumber = ""
        menuOpen = false
    }

    BackHandler(enabled = requiredPermissionsReady && !showIntro) {
        when {
            menuOpen -> menuOpen = false
            selectedOffer != null -> selectedOffer = null
            transferSheetOpen -> transferSheetOpen = false
            accountAction != null -> accountAction = null
            notificationsOpen -> notificationsOpen = false
            historyOperator != null -> historyOperator = null
            completedRequest != null -> {
                completedRequest = null
                beneficiaryNumber = ""
            }
            processingRequest != null -> Unit
            selectedOperator != null -> selectedOperator = null
            secretCode != null && !isLocked -> goHome()
            else -> Unit
        }
    }

    LaunchedEffect(isLocked, secretCode) {
        if (!isLocked && secretCode != null) {
            delay(LOCK_DELAY_MS)
            isLocked = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBackground())
    ) {
        when {
            !requiredPermissionsReady -> PermissionsSetupScreen(
                phonePermissionsGranted = phonePermissionsGranted,
                overlayPermissionGranted = overlayPermissionGranted,
                ussdAccessibilityEnabled = ussdAccessibilityEnabled,
                onRequestPhonePermissions = onRequestPhonePermissions,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onRefresh = onRefreshPermissions
            )

            showIntro -> IntroScreen(
                onContinue = {
                    showIntro = false
                    onSaveIntroDone()
                }
            )

            secretCode == null -> CreateSecretCodeScreen(
                onCreate = { code ->
                    secretCode = code
                    onSaveSecretCode(code)
                    isLocked = false
                }
            )

            isLocked -> LockScreen(
                secretCode = secretCode.orEmpty(),
                onUnlock = { isLocked = false }
            )

            selectedTerminalNetworks.isEmpty() -> TerminalNetworkSelectionScreen(
                onContinue = {
                    selectedTerminalNetworks = it
                    onSaveTerminalNetworks(it)
                }
            )

            !arePinsConfigured() -> PinRegistrationScreen(
                selectedNetworks = selectedTerminalNetworks,
                onSave = { mtnPin, orangePin, moovPin ->
                    val pinStore = MRezoPinStore(context)
                    pinStore.savePins(mtnPin, orangePin, moovPin)
                    onRefreshPermissions()
                }
            )

            else -> AnimatedContent(
                targetState = when {
                    processingRequest != null -> "processing"
                    completedRequest != null -> "success"
                    notificationsOpen -> "notifications"
                    historyOperator != null -> "history"
                    selectedOperator != null -> "catalog"
                    else -> "home"
                },
                label = "screen"
            ) { screen ->
                when (screen) {
                    "processing" -> ProcessingScreen(
                        request = processingRequest ?: return@AnimatedContent,
                        onFinished = {
                            val done = processingRequest ?: return@ProcessingScreen
                            recordNotification(done.notificationTitle, done.notificationMessage, done.operator)
                            completedRequest = done
                            processingRequest = null
                        }
                    )

                    "success" -> SuccessScreen(
                        request = completedRequest ?: return@AnimatedContent,
                        onDone = {
                            completedRequest = null
                            beneficiaryNumber = ""
                        }
                    )

                    "notifications" -> NotificationsScreen(
                        notifications = appNotifications,
                        onBack = { notificationsOpen = false }
                    )

                    "history" -> HistoryScreen(
                        operator = historyOperator ?: return@AnimatedContent,
                        notifications = appNotifications,
                        onBack = { historyOperator = null }
                    )

                    "catalog" -> CatalogScreen(
                        operator = selectedOperator ?: return@AnimatedContent,
                        offers = previewOffers.filter { it.operator == selectedOperator },
                        onBack = { selectedOperator = null },
                        onOfferClick = { selectedOffer = it }
                    )

                    else -> HomeScreen(
                        enabledOperators = selectedTerminalNetworks,
                        onOperatorClick = { selectedOperator = it },
                        onMenuClick = { menuOpen = true },
                        onTransferClick = { transferSheetOpen = true },
                        onActionClick = { accountAction = it },
                        onNotifyClick = { notificationsOpen = true }
                    )
                }
            }
        }

        selectedOffer?.let { offer ->
            PurchaseSheet(
                offer = offer,
                number = beneficiaryNumber,
                onNumberChange = { beneficiaryNumber = it.filter(Char::isDigit).take(10) },
                onDismiss = { selectedOffer = null },
                onSend = {
                    selectedOffer = null
                    if (offer.ussdRoot != null && offer.ussdSteps.isNotEmpty()) {
                        processingRequest = ProcessRequest(
                            title = "Test USSD ${offer.operator.label}",
                            subtitle = "",
                            accent = offer.operator.accent,
                            seconds = offer.executionSeconds,
                            notificationTitle = "Forfait ${offer.operator.label}",
                            notificationMessage = "Parcours ${offer.ussdRoot} puis ${offer.ussdSteps.joinToString(", ")} lance.",
                            operator = offer.operator,
                            autoComplete = false
                        )
                        onRunOfferUssd(
                            offer,
                            { result ->
                                processingRequest = null
                                val message = result.ifBlank { "Aucune reponse recue." }
                                recordNotification("Forfait ${offer.operator.label}", message, offer.operator)
                                completedRequest = ProcessRequest(
                                    title = "Forfait ${offer.operator.label}",
                                    subtitle = "",
                                    accent = offer.operator.accent,
                                    seconds = 1,
                                    notificationTitle = "Forfait ${offer.operator.label}",
                                    notificationMessage = message,
                                    operator = offer.operator
                                )
                            },
                            { error ->
                                MRezoOverlayController.hide(context)
                                processingRequest = null
                                val message = "${offer.ussdRoot} : $error"
                                recordNotification("Forfait ${offer.operator.label}", message, offer.operator)
                                completedRequest = ProcessRequest(
                                    title = "Forfait ${offer.operator.label}",
                                    subtitle = "",
                                    accent = offer.operator.accent,
                                    seconds = 1,
                                    notificationTitle = "Forfait ${offer.operator.label}",
                                    notificationMessage = message,
                                    operator = offer.operator
                                )
                            }
                        )
                    } else {
                        processingRequest = ProcessRequest(
                            title = "Traitement en cours",
                            subtitle = "",
                            accent = offer.operator.accent,
                            seconds = offer.executionSeconds,
                            notificationTitle = "Forfait ${offer.operator.label}",
                            notificationMessage = "${offer.name} envoye vers $beneficiaryNumber. Paiement 100% securise.",
                            operator = offer.operator
                        )
                    }
                }
            )
        }

        if (menuOpen) {
            MainMenuSheet(
                onDismiss = { menuOpen = false },
                onHistoryClick = { operator ->
                    menuOpen = false
                    historyOperator = operator
                },
                onLogout = {
                    goHome()
                    secretCode = null
                    showIntro = true
                    selectedTerminalNetworks = emptySet()
                    onClearUserData()
                }
            )
        }

        if (transferSheetOpen) {
            TransferUnitSheet(
                enabledOperators = selectedTerminalNetworks,
                onDismiss = { transferSheetOpen = false },
                onSend = { operator, number, amount ->
                    transferSheetOpen = false
                    accountAction = null
                    processingRequest = ProcessRequest(
                        title = "Transfert ${operator.label}",
                        subtitle = "",
                        accent = operator.accent,
                        seconds = 10,
                        notificationTitle = "Transfert ${operator.label}",
                        notificationMessage = "Transfert de $amount FCFA vers $number enregistre sur ${operator.label}. Resultat disponible dans M-REZO.",
                        operator = operator
                    )
                }
            )
        }

        accountAction?.let { action ->
            AccountActionSheet(
                action = action,
                enabledOperators = selectedTerminalNetworks,
                onDismiss = { accountAction = null },
                onSend = { operator ->
                    accountAction = null
                    if (action == AccountAction.BALANCE) {
                        processingRequest = ProcessRequest(
                            title = "Solde ${operator.label}",
                            subtitle = "",
                            accent = operator.accent,
                            seconds = 30,
                            notificationTitle = "Solde ${operator.label}",
                            notificationMessage = "Consultation du solde ${operator.label} en cours.",
                            operator = operator,
                            autoComplete = false
                        )
                        onRunBalanceUssd(
                            operator,
                            { result ->
                                processingRequest = null
                                val message = result.ifBlank { "Aucune reponse recue." }
                                recordNotification("Solde ${operator.label}", message, operator)
                                completedRequest = ProcessRequest(
                                    title = "Solde ${operator.label}",
                                    subtitle = "",
                                    accent = operator.accent,
                                    seconds = 1,
                                    notificationTitle = "Solde ${operator.label}",
                                    notificationMessage = message,
                                    operator = operator
                                )
                            },
                            { error ->
                                processingRequest = null
                                recordNotification("Solde ${operator.label}", error, operator)
                                completedRequest = ProcessRequest(
                                    title = "Solde ${operator.label}",
                                    subtitle = "",
                                    accent = operator.accent,
                                    seconds = 1,
                                    notificationTitle = "Solde ${operator.label}",
                                    notificationMessage = error,
                                    operator = operator
                                )
                            }
                        )
                    } else {
                        processingRequest = ProcessRequest(
                            title = action.label,
                            subtitle = "",
                            accent = operator.accent,
                            seconds = 9,
                            notificationTitle = "${action.label} ${operator.label}",
                            notificationMessage = "${action.resultMessage} Reseau : ${operator.label}.",
                            operator = operator
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun IntroScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBlue)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(BrandBlue, Color(0xFF020A2E), Color(0xFF00061E))
                    )
                )
        )
        Image(
            painter = painterResource(R.drawable.onboarding_kiosk),
            contentDescription = "M-REZO",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 420.dp)
                .align(Alignment.TopCenter)
                .padding(top = 26.dp, start = 12.dp, end = 12.dp),
            contentScale = ContentScale.Fit
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF00061E))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text("Bienvenue sur M-REZO", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Une seule application, toutes les possibilites, qui facilite et accelere votre quotidien.",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Commencer", color = BrandBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TerminalNetworkSelectionScreen(onContinue: (Set<OperatorBrand>) -> Unit) {
    var selected by remember { mutableStateOf(OperatorBrand.entries.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBackground())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        RoundedAppLogo(size = 92)
        Spacer(modifier = Modifier.height(22.dp))
        Text("Reseaux M-REZO", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choisissez les reseaux actifs sur ce telephone. M-REZO preparera ensuite le ciblage SIM automatiquement.",
            color = TextSoft,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
        )
        OperatorBrand.entries.forEach { operator ->
            val isSelected = operator in selected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        selected = if (isSelected) selected - operator else selected + operator
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) operator.accent else Panel
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetworkLogo(operator = operator, size = 46)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(operator.label, color = if (isSelected) operator.onAccent else Color.White, fontWeight = FontWeight.Bold)
                        Text("SIM auto : a configurer", color = if (isSelected) operator.onAccent.copy(alpha = 0.75f) else TextSoft, fontSize = 12.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = operator.onAccent)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onContinue(selected) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Ouvrir M-REZO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CreateSecretCodeScreen(onCreate: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val canSave = code.length >= 4 && code == confirm

    SecurityScreenShell(
        title = "Creer votre code secret",
        subtitle = "Chaque utilisateur protege son espace M-REZO."
    ) {
        SecretField("Code secret", code, onValueChange = { code = it.filter(Char::isDigit).take(6) })
        Spacer(modifier = Modifier.height(10.dp))
        SecretField("Confirmer le code", confirm, onValueChange = { confirm = it.filter(Char::isDigit).take(6) })
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onCreate(code) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Activer M-REZO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LockScreen(secretCode: String, onUnlock: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    SecurityScreenShell(
        title = "M-REZO verrouille",
        subtitle = "Entrez votre code secret pour continuer."
    ) {
        SecretField("Code secret", entered, onValueChange = {
            entered = it.filter(Char::isDigit).take(6)
            error = false
        })
        if (error) {
            Text("Code incorrect", color = Color(0xFFFF6B6B), modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = {
                if (entered == secretCode) onUnlock() else error = true
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Deverrouiller", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SecurityScreenShell(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoundedAppLogo(size = 104)
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(subtitle, color = TextSoft, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SecretField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = darkTextFieldColors()
    )
}

@Composable
private fun HomeScreen(
    enabledOperators: Set<OperatorBrand>,
    onOperatorClick: (OperatorBrand) -> Unit,
    onMenuClick: () -> Unit,
    onTransferClick: () -> Unit,
    onActionClick: (AccountAction) -> Unit,
    onNotifyClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)
    ) {
        item { TopBar(onMenuClick = onMenuClick, onNotifyClick = onNotifyClick) }
        item {
            TransferCreditCard(onClick = onTransferClick)
        }
        item {
            QuickActions(onActionClick = onActionClick)
        }
        item {
            Text(
                "Acheter un forfait",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OperatorBrand.entries.filter { it in enabledOperators }.forEach { operator ->
                    OperatorCard(
                        operator = operator,
                        modifier = Modifier.weight(1f),
                        onClick = { onOperatorClick(operator) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onMenuClick: () -> Unit, onNotifyClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(34.dp))
        }
        RoundedAppLogo(size = 92)
        IconButton(onClick = onNotifyClick) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun RoundedAppLogo(size: Int) {
    Image(
        painter = painterResource(R.drawable.app_icon),
        contentDescription = "M-REZO",
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape((size / 4).dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TransferCreditCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF0F2A55)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF56A3FF), modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Transfert direct", color = TextSoft, fontSize = 14.sp)
                Text("Transfert d'unite", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Numero beneficiaire et montant", color = TextSoft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickActions(onActionClick: (AccountAction) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountAction.entries.forEach { action ->
            ActionButton(action.label, action.icon, Modifier.weight(1f), onClick = { onActionClick(action) })
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(82.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.82f))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.86f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OperatorCard(operator: OperatorBrand, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(126.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = operator.accent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NetworkLogo(operator = operator, size = 58)
            Spacer(modifier = Modifier.height(8.dp))
            Text(operator.label, color = operator.onAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CatalogScreen(
    operator: OperatorBrand,
    offers: List<Offer>,
    onBack: () -> Unit,
    onOfferClick: (Offer) -> Unit
) {
    var selectedType by remember { mutableStateOf(OfferType.INTERNET) }
    val filteredOffers = offers.filter { it.type == selectedType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(operatorBackground(operator))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Forfaits ${operator.label}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Internet, appels et SMS", color = TextSoft, fontSize = 14.sp)
            }
            NetworkLogo(operator = operator, size = 58)
        }

        Spacer(modifier = Modifier.height(16.dp))
        SecurityBanner(operator = operator)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OfferType.entries.forEach { type ->
                TypeChip(
                    label = type.label,
                    selected = selectedType == type,
                    color = operator.accent,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = type }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (filteredOffers.isEmpty()) {
                item { EmptyTypeState(operator = operator) }
            } else {
                items(filteredOffers) { offer ->
                    OfferRow(offer = offer, onClick = { onOfferClick(offer) })
                }
            }
        }
    }
}

@Composable
private fun NetworkLogo(operator: OperatorBrand, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(operator.accent.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(operator.logoRes),
            contentDescription = operator.label,
            modifier = Modifier.size((size * 0.70f).dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SecurityBanner(operator: OperatorBrand) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.92f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(operator.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = operator.onAccent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Paiement 100% securise", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Transaction rapide et securisee", color = TextSoft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.height(38.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) color else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyTypeState(operator: OperatorBrand) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Text(
            "Catalogue ${operator.label} a completer avec les donnees revendeur.",
            color = TextSoft,
            modifier = Modifier.padding(18.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OfferRow(offer: Offer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.94f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NetworkLogo(operator = offer.operator, size = 50)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(offer.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Validite : ${offer.validity}", color = TextSoft, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(offer.price, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = offer.operator.accent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text("Envoyer", color = offer.operator.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseSheet(
    offer: Offer,
    number: String,
    onNumberChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkLogo(operator = offer.operator, size = 46)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("${offer.operator.label} - ${offer.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${offer.price} | ${offer.validity}", color = TextSoft, fontSize = 13.sp)
                }
            }

            TextField(
                value = number,
                onValueChange = onNumberChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Numero beneficiaire") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = darkTextFieldColors(offer.operator.accent)
            )

            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = offer.operator.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paiement 100% securise", color = TextSoft, fontSize = 13.sp)
                }
            }

            Button(
                onClick = onSend,
                enabled = number.length >= 8,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = offer.operator.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, tint = offer.operator.onAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Envoyer", color = offer.operator.onAccent, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annuler", color = TextSoft)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainMenuSheet(
    onDismiss: () -> Unit,
    onHistoryClick: (OperatorBrand) -> Unit,
    onLogout: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Espace M-REZO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            MenuLine(Icons.Filled.History, "Historique MTN", "Transactions MTN M-REZO sur 14 jours") { onHistoryClick(OperatorBrand.MTN) }
            MenuLine(Icons.Filled.History, "Historique Orange", "Transactions Orange M-REZO sur 14 jours") { onHistoryClick(OperatorBrand.ORANGE) }
            MenuLine(Icons.Filled.History, "Historique Moov", "Transactions Moov M-REZO sur 14 jours") { onHistoryClick(OperatorBrand.MOOV) }
            MenuLine(Icons.Filled.SupportAgent, "Service assistance", "Contacter le support M-REZO")
            MenuLine(Icons.Filled.Info, "Rechargement grossiste", "Module reserve a la prochaine application")
            MenuLine(
                icon = Icons.Filled.Lock,
                title = "Se deconnecter",
                subtitle = "Liberer cet espace M-REZO",
                onClick = onLogout
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferUnitSheet(
    enabledOperators: Set<OperatorBrand>,
    onDismiss: () -> Unit,
    onSend: (OperatorBrand, String, String) -> Unit
) {
    val availableOperators = enabledOperators.ifEmpty { OperatorBrand.entries.toSet() }
    var selectedOperator by remember(availableOperators) { mutableStateOf(availableOperators.first()) }
    var number by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val canSend = number.length >= 8 && amount.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Transfert d'unite", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Choisissez le reseau, le numero beneficiaire et la somme a transferer.", color = TextSoft)
            OperatorSelector(
                operators = availableOperators,
                selectedOperator = selectedOperator,
                onSelect = { selectedOperator = it }
            )
            TextField(
                value = number,
                onValueChange = { number = it.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Numero beneficiaire") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = darkTextFieldColors(BrandBlue)
            )
            TextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit).take(7) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Somme") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = darkTextFieldColors(BrandBlue)
            )
            Button(
                onClick = { onSend(selectedOperator, number, amount) },
                enabled = canSend,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Envoyer", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annuler", color = TextSoft)
            }
        }
    }
}

@Composable
private fun OperatorSelector(
    operators: Set<OperatorBrand>,
    selectedOperator: OperatorBrand,
    onSelect: (OperatorBrand) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        operators.forEach { operator ->
            val selected = operator == selectedOperator
            Button(
                onClick = { onSelect(operator) },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) operator.accent else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    operator.label,
                    color = if (selected) operator.onAccent else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBackground())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Notifications", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Messages finaux des operations M-REZO", color = TextSoft, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cleanHistory(notifications)) { notification ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notification.title, color = Color.White, fontWeight = FontWeight.Bold)
                            notification.operator?.let { operator ->
                                Text("Reseau : ${operator.label}", color = operator.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                            }
                            Text(notification.message, color = TextSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(notification.timeLabel, color = TextSoft.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    operator: OperatorBrand,
    notifications: List<AppNotification>,
    onBack: () -> Unit
) {
    val history = cleanHistory(notifications).filter { it.operator == operator }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(operatorBackground(operator))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Historique ${operator.label}", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Operations conservees pendant 14 jours", color = TextSoft, fontSize = 13.sp)
            }
            NetworkLogo(operator = operator, size = 48)
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            if (history.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Text(
                            "Aucune operation ${operator.label} sur les 14 derniers jours.",
                            color = TextSoft,
                            modifier = Modifier.padding(18.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(history) { notification ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.94f))) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(notification.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(notification.message, color = TextSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                            Text(notification.timeLabel, color = operator.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuLine(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSoft, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceCheckSheet(onDismiss: () -> Unit, onSelect: (OperatorBrand) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Consulter le solde", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Choisir la puce a interroger. Le message final apparaitra dans M-REZO.", color = TextSoft)
            OperatorBrand.entries.forEach { operator ->
                Button(
                    onClick = { onSelect(operator) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = operator.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Solde ${operator.label}", color = operator.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountActionSheet(
    action: AccountAction,
    enabledOperators: Set<OperatorBrand>,
    onDismiss: () -> Unit,
    onSend: (OperatorBrand) -> Unit
) {
    val availableOperators = enabledOperators.ifEmpty { OperatorBrand.entries.toSet() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(action.label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Choisissez le reseau a utiliser. Le resultat final sera disponible dans M-REZO.", color = TextSoft)
            availableOperators.forEach { operator ->
                Button(
                    onClick = { onSend(operator) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = operator.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(operator.label, color = operator.onAccent, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Annuler", color = TextSoft)
            }
        }
    }
}

@Composable
private fun ProcessingScreen(request: ProcessRequest, onFinished: () -> Unit) {
    var remaining by remember(request) { mutableIntStateOf(request.seconds) }
    val progressTarget = remaining / request.seconds.toFloat()
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "processingProgress"
    )

    LaunchedEffect(request) {
        remaining = request.seconds
        if (request.autoComplete) {
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
            }
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BrandBlue, Color(0xFF000B38))
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                color = request.accent,
                trackColor = Color.White.copy(alpha = 0.10f),
                strokeWidth = 10.dp,
                modifier = Modifier.size(184.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$remaining", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("secondes", color = TextSoft, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Text(request.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuccessScreen(request: ProcessRequest, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Success.copy(alpha = 0.14f)).border(1.dp, Success.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(76.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Execution validee", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(request.notificationMessage, color = TextSoft, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Success),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Terminer", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PinRegistrationScreen(
    selectedNetworks: Set<OperatorBrand>,
    onSave: (String, String, String) -> Unit
) {
    var mtnPin by remember { mutableStateOf("") }
    var orangePin by remember { mutableStateOf("") }
    var moovPin by remember { mutableStateOf("") }

    val hasMtn = selectedNetworks.contains(OperatorBrand.MTN)
    val hasOrange = selectedNetworks.contains(OperatorBrand.ORANGE)
    val hasMoov = selectedNetworks.contains(OperatorBrand.MOOV)

    val canContinue = (!hasMtn || mtnPin.length == 4) &&
                     (!hasOrange || orangePin.length == 4) &&
                     (!hasMoov || moovPin.length == 4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBackground())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = Color(0xFFFFCC00),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Codes PIN Operateurs",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Enregistrez vos codes PIN pour automatiser les operations USSD",
            color = TextSoft,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (hasMtn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OperatorBrand.MTN.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MTN", color = OperatorBrand.MTN.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = mtnPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) mtnPin = it },
                    placeholder = { Text("Code PIN MTN") },
                    colors = darkTextFieldColors(OperatorBrand.MTN.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (hasOrange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OperatorBrand.ORANGE.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ORA", color = OperatorBrand.ORANGE.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = orangePin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) orangePin = it },
                    placeholder = { Text("Code PIN Orange") },
                    colors = darkTextFieldColors(OperatorBrand.ORANGE.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (hasMoov) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OperatorBrand.MOOV.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MV", color = OperatorBrand.MOOV.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = moovPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) moovPin = it },
                    placeholder = { Text("Code PIN Moov") },
                    colors = darkTextFieldColors(OperatorBrand.MOOV.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSave(mtnPin, orangePin, moovPin) },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFCC00),
                disabledContainerColor = Color(0xFF56A3FF).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Enregistrer et Continuer",
                color = Color(0xFF08111F),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Vos codes PIN sont chiffres et securises",
            color = TextSoft.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

fun homeBackground() = Brush.verticalGradient(
    listOf(Color(0xFF08142A), Night, Color(0xFF030812))
)

private fun operatorBackground(operator: OperatorBrand) = Brush.verticalGradient(
    listOf(operator.accent.copy(alpha = 0.36f), Night, Color(0xFF030812))
)

@Composable
private fun darkTextFieldColors(accent: Color = Color(0xFF56A3FF)) = TextFieldDefaults.colors(
    focusedContainerColor = PanelSoft,
    unfocusedContainerColor = PanelSoft,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = TextSoft,
    unfocusedPlaceholderColor = TextSoft,
    focusedIndicatorColor = accent,
    unfocusedIndicatorColor = Color.Transparent
)
