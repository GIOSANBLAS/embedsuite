package com.embedsuite.app.core

import android.content.Context
import android.content.SharedPreferences
import com.embedsuite.app.connection.FirmwareProfile
import com.embedsuite.app.connection.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("embed_prefs", Context.MODE_PRIVATE)

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, false))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _scanlinesEnabled = MutableStateFlow(prefs.getBoolean(KEY_SCANLINES, false))
    val scanlinesEnabled: StateFlow<Boolean> = _scanlinesEnabled.asStateFlow()

    private val _autoReconnect = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RECONNECT, true))
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _defaultTransport = MutableStateFlow(
        TransportType.valueOf(prefs.getString(KEY_DEFAULT_TRANSPORT, TransportType.USB.name) ?: TransportType.USB.name)
    )
    val defaultTransport: StateFlow<TransportType> = _defaultTransport.asStateFlow()

    private val _glassIntensity = MutableStateFlow(prefs.getFloat(KEY_GLASS, 1f))
    val glassIntensity: StateFlow<Float> = _glassIntensity.asStateFlow()

    private val _developerMode = MutableStateFlow(prefs.getBoolean(KEY_DEVELOPER_MODE, false))
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.fromPref(prefs.getString(KEY_THEME_MODE, ThemeMode.OBSCURO.prefValue)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
        _developerMode.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.prefValue).apply()
        _themeMode.value = mode
    }

    var splashShown: Boolean
        get() = prefs.getBoolean(KEY_SPLASH_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_SPLASH_SHOWN, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var permissionsComplete: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS, value).apply()

    var tehLinkPairingGuideSeen: Boolean
        get() = prefs.getBoolean(KEY_TEH_LINK_PAIRING_GUIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_TEH_LINK_PAIRING_GUIDE, value).apply()

    /** Solo efecto en builds DEBUG — simula T-Embed sin hardware. */
    var useMockTransport: Boolean
        get() = prefs.getBoolean(KEY_MOCK_TRANSPORT, false)
        set(value) = prefs.edit().putBoolean(KEY_MOCK_TRANSPORT, value).apply()

    /**
     * Fuerza perfil hardware real (LilyGO T-Embed CC1101 Plus + Bruce vía USB).
     * @return true si se desactivó mock transport (requiere re-emparejar TEH-Link).
     */
    fun ensureRealHardwareMode(): Boolean {
        val hadMock = useMockTransport
        useMockTransport = false
        setDefaultTransport(TransportType.USB)
        setFirmwareProfile(FirmwareProfile.BRUCE)
        return hadMock
    }

    var fieldKeepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_FIELD_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_FIELD_SCREEN, value).apply()

    private val _rfFrequencyMhz = MutableStateFlow(prefs.getString(KEY_RF_FREQ, "433.92") ?: "433.92")
    val rfFrequencyMhzFlow: StateFlow<String> = _rfFrequencyMhz.asStateFlow()

    private val _fieldFrequencyMhz = MutableStateFlow(prefs.getString(KEY_FIELD_FREQ, "433.92") ?: "433.92")
    val fieldFrequencyMhzFlow: StateFlow<String> = _fieldFrequencyMhz.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.fromTag(prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.tag)))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    var fieldFrequencyMhz: String
        get() = prefs.getString(KEY_FIELD_FREQ, "433.92") ?: "433.92"
        set(value) {
            prefs.edit().putString(KEY_FIELD_FREQ, value).apply()
            _fieldFrequencyMhz.value = value
        }

    var rfFrequencyMhz: String
        get() = prefs.getString(KEY_RF_FREQ, "433.92") ?: "433.92"
        set(value) {
            prefs.edit().putString(KEY_RF_FREQ, value).apply()
            _rfFrequencyMhz.value = value
        }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setScanlinesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCANLINES, enabled).apply()
        _scanlinesEnabled.value = enabled
    }

    fun setAutoReconnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply()
        _autoReconnect.value = enabled
    }

    fun setDefaultTransport(type: TransportType) {
        prefs.edit().putString(KEY_DEFAULT_TRANSPORT, type.name).apply()
        _defaultTransport.value = type
    }

    fun setGlassIntensity(value: Float) {
        prefs.edit().putFloat(KEY_GLASS, value.coerceIn(0.3f, 1f)).apply()
        _glassIntensity.value = value.coerceIn(0.3f, 1f)
    }

    fun getAppLanguage(): AppLanguage = _appLanguage.value

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.tag).apply()
        _appLanguage.value = language
    }

    private val _firmwareProfile = MutableStateFlow(
        FirmwareProfile.fromPref(prefs.getString(KEY_FIRMWARE_PROFILE, FirmwareProfile.BRUCE.name))
    )
    val firmwareProfile: StateFlow<FirmwareProfile> = _firmwareProfile.asStateFlow()

    private val _auditModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUDIT_MODE, true))
    val auditModeEnabled: StateFlow<Boolean> = _auditModeEnabled.asStateFlow()

    fun setFirmwareProfile(profile: FirmwareProfile) {
        prefs.edit().putString(KEY_FIRMWARE_PROFILE, profile.name).apply()
        _firmwareProfile.value = profile
    }

    fun setAuditModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIT_MODE, enabled).apply()
        _auditModeEnabled.value = enabled
    }

    companion object {
        const val PREFS_NAME = "embed_prefs"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_SCANLINES = "scanlines"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_DEFAULT_TRANSPORT = "default_transport"
        private const val KEY_GLASS = "glass_intensity"
        private const val KEY_SPLASH_SHOWN = "splash_shown"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_PERMISSIONS = "permissions_complete"
        private const val KEY_TEH_LINK_PAIRING_GUIDE = "teh_link_pairing_guide_seen"
        private const val KEY_MOCK_TRANSPORT = "mock_transport"
        private const val KEY_FIELD_SCREEN = "field_keep_screen"
        private const val KEY_FIELD_FREQ = "field_freq_mhz"
        private const val KEY_RF_FREQ = "rf_freq_mhz"
        const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_FIRMWARE_PROFILE = "firmware_profile"
        private const val KEY_AUDIT_MODE = "audit_mode_enabled_v430"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
