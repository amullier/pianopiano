package fr.antmu.pianopiano

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests pour tracer la logique de détection des lancements d'apps
 * et identifier pourquoi la pause ne se déclenche pas correctement
 */
class AppLaunchDetectorLogicTest {

    // Simulation des variables d'état
    private var previousForegroundPackage: String? = null
    private var lastDetectedPackage: String? = null
    private var lastDetectionTime: Long = 0
    private var currentForegroundPackage: String? = null
    private val lastActiveTimestamps = mutableMapOf<String, Long>()

    // Variables pour capturer les résultats
    private var pauseTriggered = false
    private var lastPauseType: PauseType? = null

    private val DETECTION_COOLDOWN_MS = 2000L
    private val EXIT_THRESHOLD_MS = 5000L  // 5 secondes de tolérance

    private val YOUTUBE = "com.google.android.youtube"
    private val INSTAGRAM = "com.instagram.android"

    enum class PauseType {
        INITIAL,
        PERIODIC
    }

    private fun resetTest() {
        previousForegroundPackage = null
        lastDetectedPackage = null
        lastDetectionTime = 0
        currentForegroundPackage = null
        lastActiveTimestamps.clear()
        pauseTriggered = false
        lastPauseType = null
    }

    @Test
    fun `Scenario 1 - Changement d'onglet YouTube apres 3 secondes`() {
        resetTest()
        println("\n=== SCENARIO 1: Changement d'onglet YouTube après 3s (dans la tolérance) ===")

        var currentTime = 0L

        // 1. Premier lancement YouTube
        println("\n[T=0s] Lancement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Première ouverture devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)

        // Reset pour le prochain événement
        pauseTriggered = false
        lastPauseType = null

        // 2. Événement YouTube après 3s (< 5s de tolérance)
        currentTime += 3000
        println("\n[T=3s] Nouvel événement YouTube (changement d'onglet)")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertFalse("Événement dans les 5s ne devrait PAS déclencher la pause", pauseTriggered)
    }

    @Test
    fun `Scenario 2 - YouTube vers Instagram puis retour apres 10 secondes`() {
        resetTest()
        println("\n=== SCENARIO 2: YouTube → Instagram (10s) → YouTube ===")

        var currentTime = 0L

        // 1. Lancement YouTube
        println("\n[T=0s] Lancement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Première ouverture YouTube devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Passage à Instagram (non configuré)
        currentTime += 1000
        println("\n[T=1s] Passage à Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = false)
        assertFalse("Instagram non configuré ne devrait PAS déclencher la pause", pauseTriggered)

        // 3. Retour à YouTube après 10s
        currentTime += 10000
        println("\n[T=11s] Retour à YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Retour sur YouTube après >5s devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)
    }

    @Test
    fun `Scenario 3 - YouTube vers Home puis retour apres 10 secondes`() {
        resetTest()
        println("\n=== SCENARIO 3: YouTube → Home (10s) → YouTube ===")

        var currentTime = 0L

        // 1. Lancement YouTube
        println("\n[T=0s] Lancement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Première ouverture YouTube devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Passage au Home (launcher)
        currentTime += 1000
        println("\n[T=1s] Passage au Home")
        onHomeEvent(currentTime)

        // 3. Retour à YouTube après 10s
        currentTime += 10000
        println("\n[T=11s] Retour à YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Retour sur YouTube après >5s devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)
    }

    @Test
    fun `Scenario 4 - Premiere ouverture application configuree`() {
        resetTest()
        println("\n=== SCENARIO 4: Première ouverture app configurée ===")

        var currentTime = 0L

        // Premier lancement Instagram (configuré)
        println("\n[T=0s] Premier lancement Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)

        assertTrue("Première ouverture Instagram devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)
    }

    @Test
    fun `Scenario 5 - Evenements YouTube frequents dans la tolerance`() {
        resetTest()
        println("\n=== SCENARIO 5: Événements YouTube fréquents (< 5s) ===")
        println("NOTE: Les changements d'onglet YouTube ne génèrent généralement PAS d'événements Android")
        println("Ce test simule des événements multiples rapprochés (< 5s)")

        var currentTime = 0L

        // 1. Lancement YouTube
        println("\n[T=0s] Lancement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Première ouverture devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Événement après 3 secondes
        currentTime += 3000
        println("\n[T=3s] Événement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertFalse("Événement à 3s ne devrait PAS déclencher la pause", pauseTriggered)

        // 3. Événement après 4 secondes de plus
        currentTime += 4000
        println("\n[T=7s] Événement YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertFalse("Événement à 7s ne devrait PAS déclencher la pause (dernier événement à T=3s, delta=4s)", pauseTriggered)

        // 4. Événement après 10 secondes de plus (> 5s depuis dernier)
        currentTime += 10000
        println("\n[T=17s] Événement YouTube après 10s sans événement")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Événement après >5s devrait déclencher la pause", pauseTriggered)
    }

    @Test
    fun `Scenario 6 - Instagram vers Home puis retour apres 6 secondes`() {
        resetTest()
        println("\n=== SCENARIO 6: Instagram → Home → Instagram (6s) ===")

        var currentTime = 0L

        // 1. Lancement Instagram
        println("\n[T=0s] Lancement Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertTrue("Première ouverture Instagram devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Passage au Home
        currentTime += 1000
        println("\n[T=1s] Passage au Home")
        onHomeEvent(currentTime)

        // 3. Retour à Instagram après 6s (> 5s de tolérance)
        currentTime += 6000
        println("\n[T=7s] Retour à Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertTrue("Retour sur Instagram après >5s devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)
    }

    @Test
    fun `Scenario 6b - Instagram vers Home puis retour rapide dans les 5s`() {
        resetTest()
        println("\n=== SCENARIO 6b: Instagram → Home → Instagram (3s) ===")

        var currentTime = 0L

        // 1. Lancement Instagram
        println("\n[T=0s] Lancement Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertTrue("Première ouverture Instagram devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Passage au Home
        currentTime += 1000
        println("\n[T=1s] Passage au Home")
        onHomeEvent(currentTime)

        // 3. Retour à Instagram après 3s (< 5s de tolérance)
        currentTime += 3000
        println("\n[T=4s] Retour rapide à Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertFalse("Retour sur Instagram dans les 5s ne devrait PAS déclencher la pause", pauseTriggered)
    }

    @Test
    fun `Scenario 7 - Instagram vers YouTube puis retour`() {
        resetTest()
        println("\n=== SCENARIO 7: Instagram → YouTube (30s) → Instagram ===")

        var currentTime = 0L

        // 1. Lancement Instagram
        println("\n[T=0s] Lancement Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertTrue("Première ouverture Instagram devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 2. Passage à YouTube
        currentTime += 2000
        println("\n[T=2s] Passage à YouTube")
        onAppEvent(YOUTUBE, currentTime, isConfigured = true)
        assertTrue("Première ouverture YouTube devrait déclencher la pause", pauseTriggered)

        pauseTriggered = false

        // 3. Retour à Instagram après 30s sur YouTube
        currentTime += 30000
        println("\n[T=32s] Retour à Instagram")
        onAppEvent(INSTAGRAM, currentTime, isConfigured = true)
        assertTrue("Retour sur Instagram après >5s devrait déclencher la pause", pauseTriggered)
        assertEquals(PauseType.INITIAL, lastPauseType)
    }

    @Test
    fun `Scenario 8 - Timer periodique note`() {
        println("\n=== SCENARIO 8: Timer périodique (NOTE) ===")
        println("NOTE: Le timer périodique n'est pas testé ici car il est géré par")
        println("PeriodicTimerManager avec des callbacks asynchrones.")
        println("")
        println("Le timer périodique devrait:")
        println("- Se déclencher tous les X minutes (configuré par app)")
        println("- Afficher une pause périodique (isPeriodic=true)")
        println("- Seulement si l'app est au premier plan (currentForegroundPackage match)")
        println("- Se relancer automatiquement après chaque pause")
    }

    private fun onHomeEvent(currentTime: Long) {
        println("  → handleAppLeft($previousForegroundPackage)")
        if (previousForegroundPackage != null) {
            updateLastActiveTime(previousForegroundPackage!!, currentTime)
        }
        currentForegroundPackage = null
        println("  → currentForegroundPackage = null")
        println("  → previousForegroundPackage reste = $previousForegroundPackage")
    }

    private fun onAppEvent(packageName: String, currentTime: Long, isConfigured: Boolean) {
        println("  Event: packageName=$packageName")

        // Mettre à jour currentForegroundPackage
        currentForegroundPackage = packageName
        println("  → currentForegroundPackage = $packageName")

        // Détecter si l'utilisateur a changé d'app configurée
        if (previousForegroundPackage != null && previousForegroundPackage != packageName) {
            println("  → handleAppLeft($previousForegroundPackage)")
            updateLastActiveTime(previousForegroundPackage!!, currentTime)
        }

        // Mettre à jour le package actuel
        previousForegroundPackage = packageName
        println("  → previousForegroundPackage = $packageName")

        // Application non configurée => JAMAIS DE PAUSE
        if (!isConfigured) {
            println("  → App NOT configured")
            println("  ✅ PAS DE PAUSE (app non configurée)")
            return
        }

        println("  → App IS configured")

        // Cooldown pour éviter les déclenchements multiples
        if (packageName == lastDetectedPackage && currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS) {
            println("  → COOLDOWN actif! (last=$lastDetectedPackage à T=${lastDetectionTime})")
            println("  ✅ PAS DE PAUSE (cooldown)")
            return
        }

        lastDetectedPackage = packageName
        lastDetectionTime = currentTime
        println("  → lastDetectedPackage = $packageName, lastDetectionTime = $currentTime")

        // Vérifier depuis combien de temps on a quitté cette app
        val shouldReset = shouldResetTimer(packageName, currentTime)
        println("  → shouldReset = $shouldReset")

        if (shouldReset) {
            // Première ouverture OU parti depuis > 5 secondes => PAUSE INITIALE
            println("  🔴 PAUSE INITIALE DECLENCHEE")
            pauseTriggered = true
            lastPauseType = PauseType.INITIAL
        } else {
            // Retour dans les 5 secondes (tolérance) => PAS DE PAUSE
            println("  ✅ PAS DE PAUSE (retour rapide < 5s)")
            updateLastActiveTime(packageName, currentTime)
            // Reprendre timer périodique si configuré (non simulé ici)
        }
    }

    private fun updateLastActiveTime(packageName: String, time: Long = 0) {
        lastActiveTimestamps[packageName] = time
        println("    → lastActiveTimestamp[$packageName] = $time")
    }

    private fun shouldResetTimer(packageName: String, currentTime: Long): Boolean {
        val lastActive = lastActiveTimestamps[packageName] ?: 0L
        val elapsed = currentTime - lastActive
        val result = lastActive == 0L || elapsed > EXIT_THRESHOLD_MS
        println("    → shouldResetTimer: lastActive=$lastActive, elapsed=${elapsed}ms, result=$result")
        return result
    }
}
