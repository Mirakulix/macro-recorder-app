# Entwicklungs-Aufgabenliste: Macro Recorder APK

## 📋 Projekt-Setup & Planung

- [ ] Projekt-Repository auf GitHub/GitLab erstellen
- [ ] Android Studio Projekt initialisieren (Kotlin/Java)
- [ ] Gradle-Konfiguration einrichten
- [ ] Minimum SDK Version festlegen (empfohlen: API 24/Android 7.0)
- [ ] Target SDK Version festlegen (aktuell: API 34/Android 14)
- [ ] Git .gitignore für Android konfigurieren
- [ ] README.md mit Projektbeschreibung erstellen
- [ ] LICENSE Datei hinzufügen
- [ ] Projektstruktur nach Clean Architecture planen
- [ ] Abhängigkeiten-Management (Version Catalog) einrichten
- [ ] CI/CD Pipeline (GitHub Actions/GitLab CI) vorbereiten
- [ ] Issue-Templates für Bugs und Features erstellen

---

## 🔐 Berechtigungen & Permissions

### Manifest-Konfiguration
- [ ] SYSTEM_ALERT_WINDOW Permission im Manifest deklarieren
- [ ] BIND_ACCESSIBILITY_SERVICE Permission deklarieren
- [ ] FOREGROUND_SERVICE Permission hinzufügen
- [ ] POST_NOTIFICATIONS Permission (Android 13+) hinzufügen
- [ ] READ_EXTERNAL_STORAGE Permission deklarieren
- [ ] WRITE_EXTERNAL_STORAGE Permission deklarieren
- [ ] MANAGE_EXTERNAL_STORAGE für Android 11+ vorbereiten
- [ ] Foreground Service Types definieren (dataSync, mediaPlayback)

### Permission-Handler Implementierung
- [ ] PermissionManager Klasse erstellen
- [ ] Overlay-Permission Check implementieren
- [ ] Accessibility-Service Check implementieren
- [ ] Storage-Permission Check implementieren
- [ ] Notification-Permission Check (Android 13+) implementieren
- [ ] Permission-Status Enum definieren (GRANTED, DENIED, NEVER_ASK_AGAIN)
- [ ] Methode für alle Permissions gleichzeitig prüfen
- [ ] Deep-Link zu Accessibility Settings erstellen
- [ ] Deep-Link zu Overlay Settings erstellen
- [ ] Deep-Link zu Storage Settings erstellen
- [ ] Permission-Request Flow mit Callbacks implementieren

### Permission-UI
- [ ] Permission-Dialog Layout erstellen (XML)
- [ ] Permission-Liste mit Icons und Status-Indikatoren
- [ ] "Zu Einstellungen"-Button implementieren
- [ ] "Später"-Button mit Reminder-Funktion
- [ ] Schritt-für-Schritt-Anleitung mit Screenshots
- [ ] Permission-Status Live-Update nach Rückkehr
- [ ] Animationen für Permission-Grant Feedback

---

## 🎨 UI/UX Design & Layouts

### Theme & Styling
- [ ] Material Design 3 Theme einrichten
- [ ] Farbpalette definieren (Primary, Secondary, Tertiary)
- [ ] Dark Mode Theme erstellen
- [ ] Light Mode Theme erstellen
- [ ] Typography Styles definieren (Headline, Body, Caption)
- [ ] Custom Icons erstellen/importieren
- [ ] Ripple-Effects und Touch-Feedback konfigurieren
- [ ] Elevation und Shadow Styles definieren

### Hauptmenü (MainActivity)
- [ ] MainActivity Layout erstellen (activity_main.xml)
- [ ] Toolbar mit App-Logo und Settings-Icon
- [ ] "Neue Aufzeichnung starten"-Button (prominent, groß)
- [ ] RecyclerView für Makro-Liste einrichten
- [ ] Empty-State View (wenn keine Makros vorhanden)
- [ ] FloatingActionButton für schnelle Aufzeichnung
- [ ] Bottom Navigation (optional: Makros, Einstellungen, Info)
- [ ] Pull-to-Refresh für Makro-Liste
- [ ] Search-Bar für Makro-Suche
- [ ] Filter/Sort-Optionen (Dropdown-Menü)

### Makro-Listen-Item
- [ ] Makro-Item Layout erstellen (item_macro.xml)
- [ ] Thumbnail/Vorschaubild ImageView
- [ ] Makro-Name TextView
- [ ] Dauer und Anzahl Aktionen TextView
- [ ] Play-Button (IconButton)
- [ ] Edit-Button (IconButton)
- [ ] Delete-Button (IconButton)
- [ ] Swipe-to-Delete Geste implementieren
- [ ] Long-Press für Kontextmenü
- [ ] Checkbox für Mehrfachauswahl (Batch-Operationen)

### Makro-Detail/Einstellungen-Screen
- [ ] MacroDetailActivity Layout erstellen
- [ ] Makro-Name EditText (editierbar)
- [ ] Vorschau-Video/Animation der Aufzeichnung
- [ ] Wiederholungen-Einstellung (NumberPicker/Slider)
- [ ] Geschwindigkeit-Einstellung (SeekBar mit Labels)
- [ ] Pause zwischen Durchläufen (TimePicker)
- [ ] Zeitplan-Einstellung (DateTimePicker)
- [ ] Intervall-Einstellung (Custom Dialog)
- [ ] Wochentage-Auswahl (Chip-Group)
- [ ] Notfall-Stop Einstellung (Toggle)
- [ ] Vibration-Feedback Einstellung (Switch)
- [ ] Visueller Indikator Einstellung (Switch)
- [ ] Speichern-Button mit Bestätigung
- [ ] Abbrechen-Button mit Änderungsprüfung

### Aufzeichnungs-UI
- [ ] Aufzeichnung-Countdown Dialog
- [ ] Fullscreen-Overlay während Aufzeichnung (transparent)
- [ ] Schwebendes Widget Layout (overlay_widget.xml)
- [ ] Widget Stop-Button Design
- [ ] Widget Pause-Button (optional)
- [ ] Widget Drag-Handle für Verschiebung
- [ ] Widget Minimieren/Maximieren Animation
- [ ] Aufzeichnung-Indikator (rotes Blinken)
- [ ] Touch-Visualisierung (Kreise an Touch-Position)

### Dialoge & Popups
- [ ] Makro-Speichern Dialog (Name eingeben)
- [ ] Makro-Löschen Bestätigungsdialog
- [ ] Makro-Export Dialog (Speicherort wählen)
- [ ] Makro-Import Dialog (Datei auswählen)
- [ ] Fehler-Dialog (generisch, wiederverwendbar)
- [ ] Erfolgs-Toast Messages
- [ ] Progress-Dialog für lange Operationen
- [ ] Onboarding-Tutorial Screens (ViewPager2)

### Einstellungen-Screen
- [ ] SettingsActivity mit PreferenceScreen
- [ ] Allgemeine Einstellungen (Theme, Sprache)
- [ ] Aufzeichnungs-Einstellungen (Standard-Geschwindigkeit)
- [ ] Benachrichtigungs-Einstellungen
- [ ] Speicher-Einstellungen (Auto-Backup)
- [ ] Erweiterte Einstellungen (Developer-Optionen)
- [ ] Über-Sektion (Version, Lizenzen, Credits)
- [ ] Datenschutz-Einstellungen
- [ ] Export/Import aller Einstellungen

---

## 🏗️ Core-Architektur & Datenmodelle

### Datenmodelle (Data Classes)
- [ ] Macro Data Class erstellen
  - [ ] id: String (UUID)
  - [ ] name: String
  - [ ] createdAt: Long (Timestamp)
  - [ ] duration: Long (Millisekunden)
  - [ ] eventCount: Int
  - [ ] thumbnailPath: String?
  - [ ] settings: MacroSettings
- [ ] MacroSettings Data Class
  - [ ] repeatCount: Int (-1 für endlos)
  - [ ] speed: Float (0.25x - 10x)
  - [ ] pauseBetweenRuns: Long (Millisekunden)
  - [ ] scheduledTime: Long? (Timestamp)
  - [ ] intervalMinutes: Int?
  - [ ] selectedDays: List<DayOfWeek>
  - [ ] emergencyStopEnabled: Boolean
  - [ ] vibrationEnabled: Boolean
  - [ ] visualIndicatorEnabled: Boolean
- [ ] TouchEvent Data Class
  - [ ] timestamp: Long (relativ zu Aufzeichnungsstart)
  - [ ] x: Float
  - [ ] y: Float
  - [ ] action: TouchAction (DOWN, UP, MOVE)
  - [ ] pressure: Float
  - [ ] pointerId: Int (für Multi-Touch)
- [ ] TouchAction Enum (DOWN, UP, MOVE, CANCEL)
- [ ] MacroExecutionState Sealed Class
  - [ ] Idle
  - [ ] Running (currentRun: Int, totalRuns: Int)
  - [ ] Paused
  - [ ] Completed
  - [ ] Error (message: String)

### Database (Room)
- [ ] Room Database Setup
- [ ] AppDatabase Klasse erstellen
- [ ] MacroEntity Entity definieren
- [ ] MacroDao Interface erstellen
  - [ ] @Insert insertMacro()
  - [ ] @Update updateMacro()
  - [ ] @Delete deleteMacro()
  - [ ] @Query getAllMacros()
  - [ ] @Query getMacroById()
  - [ ] @Query searchMacros()
  - [ ] @Query getMacrosByDate()
- [ ] Database Migrations vorbereiten
- [ ] Type Converters für komplexe Typen (List, Settings)
- [ ] Database Inspector Testing

### Repository Pattern
- [ ] MacroRepository Interface definieren
- [ ] MacroRepositoryImpl Implementierung
  - [ ] getAllMacros(): Flow<List<Macro>>
  - [ ] getMacroById(id: String): Macro?
  - [ ] saveMacro(macro: Macro)
  - [ ] updateMacro(macro: Macro)
  - [ ] deleteMacro(id: String)
  - [ ] searchMacros(query: String): List<Macro>
  - [ ] exportMacro(id: String): File
  - [ ] importMacro(file: File): Macro
- [ ] File Storage für Touch-Events (JSON)
- [ ] Thumbnail-Speicherung und -Verwaltung
- [ ] Cache-Strategie implementieren

---

## 🎬 Aufzeichnungs-Engine

### Accessibility Service
- [ ] MacroRecorderAccessibilityService erstellen
- [ ] Service im Manifest registrieren
- [ ] accessibility_service_config.xml erstellen
- [ ] onAccessibilityEvent() implementieren
- [ ] Touch-Event Capturing
- [ ] Event-Filtering (nur relevante Events)
- [ ] Multi-Touch Unterstützung
- [ ] Gesture-Detection (Swipe, Pinch, Long-Press)
- [ ] Event-Timing präzise erfassen (System.nanoTime())
- [ ] Event-Queue für Pufferung

### Recording Manager
- [ ] RecordingManager Singleton erstellen
- [ ] startRecording() Methode
- [ ] stopRecording() Methode
- [ ] pauseRecording() Methode
- [ ] resumeRecording() Methode
- [ ] isRecording: Boolean State
- [ ] recordedEvents: MutableList<TouchEvent>
- [ ] recordingStartTime: Long
- [ ] Event-Listener Interface für UI-Updates
- [ ] Speicher-Optimierung (Event-Kompression)
- [ ] Fehlerbehandlung bei Service-Disconnect

### Overlay Widget
- [ ] OverlayWidgetService (Foreground Service) erstellen
- [ ] WindowManager Parameter konfigurieren
- [ ] Widget View inflaten und hinzufügen
- [ ] Touch-Listener für Verschiebung implementieren
- [ ] Stop-Button Click-Listener
- [ ] Widget-Position speichern (SharedPreferences)
- [ ] Widget-Größe anpassbar machen
- [ ] Widget-Transparenz einstellbar
- [ ] Widget außerhalb Bildschirm verhindern
- [ ] Widget-Animationen (Erscheinen/Verschwinden)

### Recording Foreground Service
- [ ] RecordingForegroundService erstellen
- [ ] Persistent Notification während Aufzeichnung
- [ ] Notification mit Stop-Action
- [ ] Notification mit Pause-Action
- [ ] Service-Lifecycle Management
- [ ] Wake Lock für zuverlässige Aufzeichnung
- [ ] Battery-Optimization Handling
- [ ] Service-Restart nach System-Kill

---

## ▶️ Makro-Ausführungs-Engine

### Playback Manager
- [ ] PlaybackManager Singleton erstellen
- [ ] executeMacro(macroId: String) Methode
- [ ] stopExecution() Methode
- [ ] pauseExecution() Methode
- [ ] Touch-Event Simulation über AccessibilityService
- [ ] GestureDescription Builder für komplexe Gesten
- [ ] Timing-Präzision (Handler mit postDelayed)
- [ ] Geschwindigkeits-Multiplikator anwenden
- [ ] Wiederholungs-Logik implementieren
- [ ] Pausen zwischen Durchläufen
- [ ] Execution-State LiveData/Flow

### Gesture Dispatcher
- [ ] GestureDispatcher Klasse erstellen
- [ ] dispatchTap(x, y) Methode
- [ ] dispatchSwipe(startX, startY, endX, endY, duration)
- [ ] dispatchLongPress(x, y, duration)
- [ ] dispatchMultiTouch(points: List<Point>)
- [ ] Gesture-Validation (Koordinaten im Bildschirm)
- [ ] Fehlerbehandlung bei fehlgeschlagenen Gesten
- [ ] Retry-Mechanismus für kritische Gesten

### Scheduler
- [ ] MacroScheduler Klasse erstellen
- [ ] AlarmManager Integration
- [ ] WorkManager Integration für zuverlässige Ausführung
- [ ] scheduleMacro(macroId, time) Methode
- [ ] cancelScheduledMacro(macroId)
- [ ] Intervall-basierte Wiederholung
- [ ] Wochentage-Filter
- [ ] Notification bei geplanter Ausführung
- [ ] Wakeup-Alarm für präzises Timing
- [ ] Battery-Optimization Whitelist-Request

### Execution Foreground Service
- [ ] ExecutionForegroundService erstellen
- [ ] Notification während Ausführung
- [ ] Fortschrittsanzeige in Notification
- [ ] Stop-Action in Notification
- [ ] Execution-Logs für Debugging
- [ ] Crash-Recovery (Execution fortsetzen)
- [ ] Performance-Monitoring

---

## 🔧 ViewModels & Business Logic

### MainViewModel
- [ ] MainViewModel erstellen (extends ViewModel)
- [ ] macroList: StateFlow<List<Macro>>
- [ ] loadMacros() Methode
- [ ] deleteMacro(id: String)
- [ ] searchMacros(query: String)
- [ ] sortMacros(sortBy: SortOption)
- [ ] filterMacros(filter: FilterOption)
- [ ] Error-Handling mit sealed class Result
- [ ] Loading-State Management

### RecordingViewModel
- [ ] RecordingViewModel erstellen
- [ ] isRecording: StateFlow<Boolean>
- [ ] recordingDuration: StateFlow<Long>
- [ ] eventCount: StateFlow<Int>
- [ ] startRecording()
- [ ] stopRecording()
- [ ] pauseRecording()
- [ ] saveMacro(name: String)
- [ ] Timer für Aufzeichnungsdauer
- [ ] Event-Counter Update

### MacroDetailViewModel
- [ ] MacroDetailViewModel erstellen
- [ ] macro: StateFlow<Macro?>
- [ ] loadMacro(id: String)
- [ ] updateSettings(settings: MacroSettings)
- [ ] saveMacro()
- [ ] deleteMacro()
