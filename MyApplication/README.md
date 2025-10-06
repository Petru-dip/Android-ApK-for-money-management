# FinanceApp (Java, Android)
- Bază de date Room v2 (Expense + Income) cu migrare 1→2 (adaugă `categoryType` în Expense).
- Înregistrare **venituri** și **cheltuieli**, filtre (PERSONAL/FIRMA + perioadă), și **rapoarte grafice** (MPAndroidChart).
## Cum rulezi
1. Deschide folderul în Android Studio (AGP 8.6+, JDK 17).
2. Android Studio va configura automat Gradle Wrapper și va descărca dependențele.
3. Rulează pe emulator/dispozitiv.
## Notă
- Pachet: `com.example.expensetracker`. Dacă vrei alt pachet, folosește Refactor > Rename.
- Dacă ai date existente din versiunea v1 (fără `categoryType`), migrarea 1→2 va păstra datele și va seta `categoryType='PERSONAL'` implicit.
