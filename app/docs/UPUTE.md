# e-LijekoviHR — Upute (kratko)

Ovaj dokument daje kratak pregled što aplikacija sadrži i kako je koristiti.

## Što aplikacija sadrži
- Praćenje lijekova: dodavanje, uređivanje i brisanje lijekova.
- Grupiranje po terminima: Jutro / Popodne / Večer.
- Dodatne funkcionalnosti: "Uzmi sve", intervalno doziranje, drag & drop.
- Export / Import podataka u JSON formatu (korištenjem kotlinx.serialization).
- UI: Jetpack Compose + Material 3.
- Trenutna pohrana: files + automatsko spremanje (preporuka: Room za finalnu verziju).

## Brze upute za korištenje
1. Dodavanje lijeka:
   - Pritisnite FAB (+) → unesite naziv, dozu i odaberite termine → Spremi.
2. Uređivanje lijeka:
   - Pritisnite ikonu za uređivanje na kartici → izmijenite podatke → Spremi.
3. Brisanje lijeka:
   - Pritisnite ikonu za brisanje; nakon brisanja pojavljuje se Undo (Snackbar).
4. Export / Import:
   - Postavke → Upravljanje podacima → Export/Import JSON.

## Kratki pregled važnih implementacijskih elemenata
- Model: Lijek.kt i enum DobaDana.
- Persistencija: trenutačno files + auto-save; preporuka: migrirati na Room/DataStore.
- Notifikacije i raspoređivanje: planirano kroz WorkManager / exact alarms.

## Hitne preporuke (kratko)
- Prioritet 1: stabilna lokalna pohrana (Room), swipe-to-delete s potvrdom + Undo, pretraga/filtriranje.
- Prioritet 2: bolji reset dnevnih statusa (timestamp-based), poboljšati notifikacijski scheduler.
- Prioritet 3+: UX poboljšanja (animacije, dark toggle), napredne značajke (kalendar, statistike), sigurnost i testiranje.

## Gdje pronaći detalje
- Detaljan changelog, roadmap i TODO nalaze se u originalnom docs/README.md (kanon).
- Ako želite potpunu verziju dokumentacije, pogledajte: I:\PythonLab\e_lijekovi_2\docs\README.md

## Obavijesti i ponašanje aplikacije (Jutro / Podne / Večer) — sažetak implementacije

Cilj
- Korisnik definira lijekove i termine (JUTRO, PODNE, VEČER). Aplikacija šalje dnevnu obavijest kada je vrijeme za uzimanje lijekova koji su označeni za taj termin.
- Ako korisnik potvrdi "Yes" u notifikaciji (Da, uzeto), aplikacija smanjuje količinu lijeka za definiranu dozu (npr. 1 ili više) i sprema promjenu.

Glavni koraci (kratko)
1. Model
   - Medication:
     - id: String/UUID
     - name: String
     - dosePerTake: Int (koliko tableta/komada uzima po uzimanju)
     - totalQuantity: Int (trenutna količina)
     - schedule: Set<DobaDana> (JUTRO, PODNE, VECER)
     - imageUri: String? (opcionalno)
     - enabled: Boolean
   - DobaDana enum: JUTRO, PODNE, VECER

2. Pohrana
   - Zadržati postojeću file/json serializaciju; svaki put kad se spremi repository pozvati scheduler.updateAll(context).
   - Preporuka: migrirati na Room kasnije.

3. Scheduler (AlarmManager ili WorkManager)
   - Tri konfigurabilna vremena (defaults: 08:00, 13:00, 20:00).
   - Postaviti daily exact alarm za svako DobaDana; alarm šalje Intent s extras: { "doba": "JUTRO" }.
   - Na promjeni lijekova ili vremena: clear i re-schedule alarm-e.

4. Receiver / Worker
   - MedReminderReceiver (BroadcastReceiver) koji na trigger:
     - Učita sve lijekove iz repository.
     - Filter: lijekovi s schedule sadrže primljeno DobaDana i enabled == true.
     - Ako lista nije prazna: izgradi group notifikaciju (kanal "medications_reminders") s:
       - Tijek: naslov ("Vrijeme za JUTRO terapiju"), sadržaj sa sažetkom (npr. "3 lijek(a). Tap za detalje.")
       - LargeStyle/Inbox: listu naziva i količina (npr. "Aspirin — 1 od 30")
       - Akcije: "Da (uzeto)" i "Odgodi 10 min"
         - "Da": PendingIntent koji poziva ActionReceiver → smanji totalQuantity za dosePerTake i spremi
         - "Odgodi": re-schedule single exact alarm za +n minuta
     - Notifikacije grupirati po danu/kanalu.

5. Akcije iz notifikacije
   - ACTION_TAKEN: primi medication ids i za svaki decrement totalQuantity = max(0, totalQuantity - dosePerTake); spremi promjene; opcionalno prikaži potvrdu (Toast/Snackbar) i update notifikacije.
   - ACTION_SNOOZE: postavi alarm za +n minuta samo za taj DobaDana.

6. UI — početni ekran
   - Prikaži tri kartice Jutro / Podne / Večer samo ako postoji bar jedan enabled lijek za taj termin (dinamički).
   - Klik na karticu → otvara listu lijekova za taj termin.
   - Svaka stavka (kartica lijeka) prikazuje:
     - Slika (ako postoji),
     - Naziv i dozu,
     - Količinu u obliku "10 od 30" (trenutno / početno),
     - Male oznake/ikone za termine (● JUTRO ● PODNE ● VECER) ili tri točke ispod slike koje pokazuju termine.

7. Dodaci / edge-cases
   - BootReceiver: pri BOOT_COMPLETED re-scheduleovati sve alarms.
   - Promjena vremenske zone / DST: koristiti java.time.LocalTime i izračun next trigger.
   - Ako korisnik odbije dozvole za notifikacije (Android 13+): obavijestiti i ponuditi put do postavki.
   - Doze: za kritične podsjetnike koristiti setExactAndAllowWhileIdle; ako preciznost nije potrebna, WorkManager PeriodicWorkRequest može biti korisniji.

## Implementacijski koraci — korak-po-korak (akcijski)

1. Model (data/Medication.kt, data/DobaDana.kt)
   - Medication: id (UUID), name, dosePerTake:Int, totalQuantity:Int, schedule:Set<DobaDana>, imageUri:String?, enabled:Boolean.

2. Repository (data/MedicationRepository.kt)
   - Metode: loadAll():List<Medication>, saveAll(List<Medication>), update(medication), delete(id).
   - Nakon svakog save/update/delete pozvati NotificationScheduler.updateAll(context).

3. Scheduler (notifications/NotificationScheduler.kt)
   - Koristiti AlarmManager.setExactAndAllowWhileIdle.
   - Konfigurabilna vremena: defaults 08:00, 13:00, 20:00.
   - Za svaki DobaDana postaviti PendingIntent s extra "doba" i jedinstvenim requestCode.
   - Metode: updateAll(context), scheduleFor(doba), cancelAll(context).

4. Receiveri (notifications/)
   - MedReminderReceiver (BroadcastReceiver): na trigger učitati meds za primljeno DobaDana i prikazati notifikaciju.
   - NotificationActionReceiver: ACTION_TAKEN (s medIds) → dekrementirati totalQuantity za dosePerTake i spremiti.
   - BootReceiver: pri BOOT_COMPLETED pozvati NotificationScheduler.updateAll(context).

5. Notifikacije
   - Channel: "medications_reminders" (high importance).
   - Prikaz grupirane notifikacije (InboxStyle ili MessagingStyle) s listom lijekova i količina.
   - Akcije: "Da (uzeto)" → PendingIntent za ACTION_TAKEN (proslijedi medIds), "Odgodi 10 min" → jednokratni alarm +10 min.

6. UI (ui/)
   - HomeScreen.kt: tri kartice (JUTRO/PODNE/VECER) prikazane samo ako postoje meds za termin.
   - MedicationListScreen.kt: lista kartica lijekova; svaka kartica prikazuje sliku, naziv, "trenutno od početnog" (npr. 10 od 30) i male oznake termina (●).

7. Testiranje i edge-case
   - Reboot uređaja, promjena vremena, Doze, permisije POST_NOTIFICATIONS (Android 13+), prazne liste, 0 quantity.

## Razvoj po modulima — predloženi redoslijed i workflow

Cilj: graditi aplikaciju iterativno po malim, nezavisnim modulima tako da imaš potpunu kontrolu nad svakom isporukom.

Opći workflow
1. Za svaki modul generiram kratak plan (API/skeleton, testovi, demo).
2. Napravim PR/commit samo za taj modul.
3. Ti pregledaš (funkcionalnost + kod). Nakon tvoje potvrde prelazimo na sljedeći modul.
4. Ako treba, radi ispravke i ponovnu validaciju prije merge-a.

Preporučeni redoslijed modula (prioriteti i kratki opis)

1) Modul: data-model i repository (obavezno)
   - Sadržaj: data/Medication.kt, data/DobaDana.kt, data/MedicationRepository.kt (file/JSON storage).
   - Zadaci: definirati model, osnovne CRUD metode, serializacija, jednostavni unit testovi.
   - Kriterij prihvata: mogu dodati/urediti/brisati lijekove i podatak se persistrira i učitava.

2) Modul: UI - osnovni screens (početni mock)
   - Sadržaj: ui/HomeScreen.kt (tri kartice), ui/MedicationListScreen.kt (prikaz kartica).
   - Zadaci: prikaz dinamičnih kartica bez notifikacija (povezati s repository).
   - Kriterij: na home vidiš samo kartice za termine koji imaju lijekove; klik otvara listu.

3) Modul: NotificationScheduler + MedReminderReceiver (alarm scheduling)
   - Sadržaj: notifications/NotificationScheduler.kt, notifications/MedReminderReceiver.kt
   - Zadaci: postaviti dnevne alarm-e (defaults 08:00/13:00/20:00), receiver koji šalje test notifikaciju.
   - Kriterij: alarm triggeruje receiver i prikazuje test notifikaciju u to vrijeme.

4) Modul: Notifikacije & akcije (Da / Odgodi) + ActionReceiver
   - Sadržaj: notifications/NotificationActionReceiver.kt, notifikacijski kanal.
   - Zadaci: prikaz stvarne grupirane notifikacije s listom lijekova; ACTION_TAKEN smanjuje quantity.
   - Kriterij: iz notifikacije "Da" ažurira medication.totalQuantity i persistrira promjenu.

5) Modul: BootReceiver i rescheduling
   - Sadržaj: notifications/BootReceiver.kt
   - Zadaci: pri BOOT_COMPLETED ponovno postaviti alarm-e.
   - Kriterij: nakon reboot-a alarmi su ponovno raspoređeni.

6) Modul: Settings (vremena, snooze duration, notifikacije)
   - Sadržaj: ui/SettingsScreen.kt, spremanje postavki.
   - Zadaci: korisnik može promijeniti vremena za jutro/podne/večer; promjena re-schedule-a alarm-e.
   - Kriterij: promjena vremena odmah utječe na raspored.

7) Modul: UX poboljšanja i slike lijekova
   - Sadržaj: podrška za imageUri u modelu, picker slike.
   - Zadaci: prikaz slike na kartici lijekova, fallback ikona.
   - Kriterij: upload/preview slike i spremanje reference.

8) Modul: Testovi, dozvole i proizvodnja
   - Sadržaj: unit tests, integracijski test za scheduler, POST_NOTIFICATIONS handling (Android 13+).
   - Zadaci: pokriti critical flows testovima, primiti permisije.
   - Kriterij: svi osnovni scenariji testirani; aplikacija radi i s ograničenjima Doze.

Veličina isporuka i vremena
- Svaki modul = mali PR (1–3 datoteke + test) — cilj: isporuka unutar 1–3 dana po modulu (ovisno o složenosti).
- Po završetku modula: kratki demo (screenshot/riječ) i tvoj feedback.

Kontrola i rollback
- Svaki modul radim na feature branchu; nakon tvoje potvrde mergeamo u glavnu granu.
- Ako nešto "zajebe", revertamo PR i ispravimo prije sljedećeg koraka.

Prednosti ovakvog pristupa
- Imaš potpunu kontrolu nad redoslijedom i integracijom.
- Razvoj je postupni — lako je debugirati i rollback.
- Svaki modul ima jasno definirane kriterije prihvata.

## Dodatak: što smo implementirali (kratko, aktualno)

U ovoj iteraciji implementirali smo sljedeće:

- UI: `MedicationListScreen` — prikaz spremljenih lijekova u listi s karticama; svaka kartica ima tipke za Uredi i Obriši.
- UI: `MedicationFormScreen` — forma za unos novog ili uređivanje postojećeg lijeka. Pri uređivanju polja su unaprijed popunjena.
- Glavna logika: `MainActivity.MainScreen` je proširena da:
  - Učita lijekove iz `MedicationRepository` i prikazuje ih u `MedicationListScreen`.
  - Omogući otvaranje forme za dodavanje ili uređivanje (Edit otvara formu s inicijalnim podacima).
  - Nakon spremanja poziva `repo.add()` za novi unos ili `repo.update()` za uređeni unos.
  - Prikazuje potvrdu korisniku pomoću `Snackbar` nakon spremanja ili brisanja.
- Trajna pohrana: koristi se postojeći `MedicationRepository` (files/medications.json).

Datoteke koje su promijenjene / dodane:
- app/src/main/java/com/example/e_lijekovi/ui/MedicationListScreen.kt (novo)
- app/src/main/java/com/example/e_lijekovi/ui/MedicationFormScreen.kt (izmjena: podrška za edit)
- app/src/main/java/com/example/e_lijekovi/MainActivity.kt (izmjena: edit flow, snackbar, refresh liste)

Sljedeći koraci koje preporučamo:
- Dodati Undo akciju u Snackbar (scope.launch { snackbarHostState.showSnackbar("Lijek obrisan", actionLabel = "Undo") }) i podršku za poništavanje brisanja u repo.
- Validacija unosa u `MedicationFormScreen` (npr. obavezan naziv, pozitivni brojevi).
- Prikaz detalja lijeka i/ili slika (imageUri) u kartici.
- Migracija pohrane na Room za pouzdaniju upotrebu i relacije.

---

Ažurirano: implementirano prikaz liste, uređivanje i snackbar potvrde.
