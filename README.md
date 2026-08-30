# JARVIS POC

Two vertical slices of an on-device Android agent, nothing more:

1. **Amazon** — search a product, add it to the cart, walk to checkout, **stop on the
   "Place your order" screen**.
2. **Instagram** — caption an uploaded photo with on-device Gemma-3n, then drive the
   composer and post it.

Everything happens through an `AccessibilityService`. There is no LLM planner: the two
flows are deterministic Kotlin scripts. The model is used only for captioning.

---

## Before you start

Two things worth knowing up front.

**Both flows automate apps whose terms prohibit it.** Instagram in particular is
aggressive about detecting automation and bans accounts for it. **Use a throwaway
Instagram account.** Amazon may respond with captchas or OTP re-auth on a flow that
repeatedly walks to checkout.

**There are two independent auto-confirm switches, both off by default**, one per flow —
buying something and posting something are different decisions.

| Switch | Off (default) | On |
|---|---|---|
| Amazon | Selects Pay on Delivery, parks on "Place your order". Nothing is ordered. | Places a real Cash-on-Delivery order — but only if COD is confirmed (see below). |
| Instagram | Fills the caption in, stops before Share. | Taps Share. The post goes live. |

Leave the Amazon one off until the flow passes cleanly several times in a row.

### The COD guard

The Amazon flow selects **Cash / Pay on Delivery** during checkout, and will not
place an order it cannot prove is COD. Two independent conditions must both hold:
it actually tapped the COD option, *and* "Pay on Delivery" is visible on the final
review screen. If either fails it stops and hands you the screen, even with
auto-confirm on.

That guard is the whole point of the feature. A silent failure to select COD does
not stop checkout — it falls through to whatever card is saved on the account, so
"COD selection didn't work" and "we just charged your card" would otherwise be the
same run. COD unavailable for an item or pincode shows as a greyed-out option,
which never matches a selector, so that case stops cleanly too.

Because a flow halts while the *target* app owns the screen, results are announced by
toast and notification, not just written to the in-app trace.

---

## Voice command

Press **Speak a command** and say something like:

> "choose the most recent photo and post it on Instagram with a relevant caption"

That chains newest-gallery-photo → on-device caption → Instagram composer. Add a tone
word to steer the caption: *funny, witty, professional, casual, poetic, short*.

Parsing is keyword matching (`voice/VoiceCommand.kt`), not a second model pass — the
open question in this POC is whether we can drive real apps reliably, and a parser you
can predict by reading it keeps that question isolated.

Three things worth knowing:

- **This is the only feature that needs `READ_MEDIA_IMAGES`.** The photo picker needs no
  permission; "pick the latest one for me" is impossible without library-wide read
  access. It's requested when you first press the mic, and denying it leaves the manual
  picker working.
- **Amazon voice works too**, but the phrase must name a product: *"order a USB C cable
  on Amazon"* runs; *"place an order on Amazon"* is rejected, because guessing a product
  when money is involved is not a service. The parsed product is mirrored into the Amazon
  card so a mis-transcription is visible on screen, and the run obeys the same
  auto-confirm switch and COD guard as the button. Quantities are ignored — it always
  adds one unit.
- **On-device speech is opt-in, off by default.** `EXTRA_PREFER_OFFLINE` is not a soft
  preference: with no offline language pack installed for your locale the recogniser
  refuses outright ("Voice search isn't available") rather than falling back to the
  network. Turn the switch on only after installing a pack via Settings → System →
  Languages & input → On-device speech recognition.

## Build

Requires JDK 17 and the Android SDK (compileSdk 35).

```bash
cd jarvis-poc

# First time only — this repo has no gradle-wrapper.jar checked in.
# Either open the project once in Android Studio (it generates the wrapper),
# or, with a local Gradle 8.9+ on PATH:
gradle wrapper --gradle-version 8.9

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If `adb devices` shows `unauthorized`, revoke and re-accept the USB debugging prompt:

```bash
adb kill-server && adb start-server && adb devices
# then accept "Allow USB debugging" on the phone
```

Use `~/Library/Android/sdk/platform-tools/adb`, not the PCSuite one.

---

## Push the caption model

Download **Gemma-3n E2B** in `.litertlm` form from HuggingFace
`google/gemma-3n-E2B-it-litert-lm`. It is a gated repo — accept the licence on your HF
account first. E4B also works if you have the RAM.

```bash
adb shell mkdir -p /sdcard/Android/data/com.jarvispoc/files/llm
adb push <downloaded>.litertlm \
  /sdcard/Android/data/com.jarvispoc/files/llm/gemma-3n-E2B-it-int4.litertlm
```

The filename must match `ModelLocator.MODEL_FILE`. If what you download is named
differently, either rename it on push (as above) or change the constant in
`app/src/main/java/com/jarvispoc/ai/ModelLocator.kt`.

The app's external files dir is used rather than `/data/local/tmp/llm/` — Google's
samples use the latter, but SELinux on OEM ROMs (OriginOS/Funtouch on the iQOO) often
blocks an app from reading it even though `adb push` writes it fine.
`/data/local/tmp/llm/` is still checked as a fallback.

The app's Status card tells you which path resolved, or reports the model missing.

---

## Enable the accessibility service

Settings → Accessibility → **JARVIS POC agent** → on.

Or from ADB — but note this **overwrites** the list of enabled services, so it will
switch off anything else you rely on:

```bash
adb shell settings put secure enabled_accessibility_services \
  com.jarvispoc/com.jarvispoc.service.JarvisAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

The Status card shows `running` once the service actually binds.

---

## Start here: the screen dumper

**Every selector in both flows is a first guess.** They were written without access to a
real device, against no dumps. The dumper is how they get corrected, and it is the first
thing to run.

In the app, press **Dump (amazon)** or **Dump (instagram)**, then switch to the screen you
want captured — you have 5 seconds. **A failed flow also dumps automatically**, tagged
`failure-<flow>`, which is usually the dump you actually want. Then:

```bash
adb pull /sdcard/Android/data/com.jarvispoc/files/dumps ./dumps
adb logcat -s JarvisPoc     # live trace, same content as the in-app pane
```

Capture these five screens and the flows can be made accurate:

| Screen | Fixes |
|---|---|
| Amazon search results | `RESULT_ITEM` |
| Amazon product page | `ADD_TO_CART` |
| Amazon cart | `PROCEED_TO_BUY` |
| Amazon checkout | `CHECKOUT_ADVANCE`, `PLACE_ORDER` |
| Instagram composer | `CAPTION_FIELD`, `SHARE_BUTTON`, `FEED_OPTION` |

Selectors live in the `companion object` of `AmazonOrderFlow.kt` and
`InstagramPostFlow.kt`. Each is a `query(...)` with ordered fallbacks — first match wins,
so you can add a real resource id at the top and leave the guesses beneath it.

**If a dump comes back empty**, the app is suppressing its hierarchy (`FLAG_SECURE` or
similar). That is a hard blocker for automating it, and better to discover on day one
than in week three.

---

## Layout

```
app/src/main/java/com/jarvispoc/
├── MainActivity.kt          Compose control panel + live trace
├── core/
│   ├── UiNode.kt            flattened accessibility node
│   ├── Selector.kt          Selector + Query (ordered fallbacks)
│   ├── AgentLog.kt          shared StateFlow trace, mirrored to logcat
│   ├── Contracts.kt         FlowResult: Success | AwaitingUser | Failed
│   └── Photos.kt            software-bitmap decode + FileProvider staging
├── service/
│   ├── JarvisAccessibilityService.kt
│   ├── ScreenObserver.kt    tree → List<UiNode>, JSON dumps
│   ├── ActionExecutor.kt    awaitNode / tap / setText / scroll / launch
│   └── Notifier.kt          toast + notification for out-of-app results
├── ai/
│   ├── CaptionEngine.kt     interface
│   ├── GemmaCaptionEngine.kt MediaPipe + Gemma-3n
│   └── ModelLocator.kt
├── flows/
│   ├── Flow.kt
│   ├── AmazonOrderFlow.kt
│   └── InstagramPostFlow.kt
└── voice/
    └── VoiceCommand.kt      spoken phrase → intent, keyword-matched
```

### Why it is shaped this way

**Layered fallbacks in `ActionExecutor`.** Real apps break the accessibility contract
constantly. `tap()` tries `ACTION_CLICK`, then the nearest clickable ancestor (Amazon
marks the tile container clickable, not the text inside), then a synthetic gesture at the
node centre. `setText()` tries `ACTION_SET_TEXT` then a clipboard paste, because
Instagram's caption field routinely refuses the former.

**Polling, not events.** `awaitNode()` re-walks the tree every 250ms until a query
matches. Accessibility event streams from these two apps are far too noisy to drive a
state machine.

**Deep links where possible.** Amazon search and cart are reached by URL rather than by
automating the search box and hunting the cart icon. Fewer steps, fewer failure points.

**The Instagram photo arrives by `ACTION_SEND`,** which Meta documents as the supported
route into the feed composer. That skips the single most brittle thing we could attempt —
identifying one specific thumbnail in a media grid. The photo is re-encoded into our own
files dir and served via FileProvider, because the picker's content URI is granted to us
and cannot be re-granted onward to Instagram.

**Flows run on the service's coroutine scope,** not the Activity's, so they survive our
app dropping to the background when Amazon takes the screen.

---

## Verify

| Phase | Check |
|---|---|
| Build | `assembleDebug` green, APK installs, Status shows `running` |
| Dumper | Dumps from all five screens contain real ids/text and are non-empty |
| Amazon | Pick a **cheap, in-stock** item. Success = parks on "Place your order" without tapping. Run 5×; under 4/5 means selector work, not a rewrite. **Empty the cart between runs** — checkout takes the whole cart, so five runs means five items on one order |
| Caption | Generate with Instagram never opened. Record cold-start and per-caption latency separately — the first call loads multi-GB weights |
| Instagram | End-to-end on a throwaway account |

---

## Troubleshooting

**`package not installed or not visible`** — the target isn't in the `<queries>` block in
`AndroidManifest.xml`. Android 11+ hides packages you haven't declared.

**Model won't load** — check the Status card for the resolved path. If it says MISSING,
the filename doesn't match `ModelLocator.MODEL_FILE`.

**Caption fails with a pixel-access error** — something handed the engine a HARDWARE
bitmap. `Photos.load` forces `ALLOCATOR_SOFTWARE`; anything new feeding the engine must
do the same.

**Flow times out on a step** — the trace names the query that timed out
(`timed out after 8000ms waiting for 'Add to Cart'`). Dump that screen and fix the
selector.

**Build fails on MediaPipe symbols** — `tasks-genai` is in maintenance mode upstream and
its API has moved before. The names this code uses are
`LlmInference.LlmInferenceOptions`, `LlmInferenceSession.LlmInferenceSessionOptions`,
`GraphOptions.setEnableVisionModality`, `session.addImage`, `BitmapImageBuilder`. If any
have moved, `GemmaCaptionEngine.kt` is the only file that touches them.

---

## Not built

LLM planner · multi-app generality · memory/persistence · voice input · OCR/vision
perception fallback · Amazon login automation (stay signed in) ·
WhatsApp/Gmail/Uber/Chrome.

`Flow` is an interface, so a planner can be added later without touching the executor or
the service.
