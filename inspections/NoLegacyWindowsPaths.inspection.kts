import org.intellij.lang.annotations.Language

@Language("HTML")
val htmlDescription = """
  <html>
  <body>
    Detects legacy Windows workspace paths after the desktop-tauri move to repo root.
    <p>Flags these string fragments:</p>
    <ul>
      <li><code>apps/windows</code></li>
      <li><code>windows/ui</code></li>
      <li><code>windows/desktop-tauri</code></li>
    </ul>
  </body>
  </html>
""".trimIndent()

val legacyPathPatterns = listOf(
  "apps/windows",
  "windows/ui",
  "windows/desktop-tauri",
)

val noLegacyWindowsPathsInspection = localInspection { psiFile, inspection ->
  val text = psiFile.text ?: return@localInspection
  val hit = legacyPathPatterns.firstOrNull { pattern -> text.contains(pattern) } ?: return@localInspection
  val message = "Legacy path '$hit' found. Use 'desktop-tauri' root paths instead."
  inspection.registerProblem(psiFile, message)
}

listOf(
  InspectionKts(
    id = "NoLegacyWindowsPaths",
    localTool = noLegacyWindowsPathsInspection,
    name = "No Legacy Windows Paths",
    htmlDescription = htmlDescription,
    level = HighlightDisplayLevel.WARNING,
  )
)
