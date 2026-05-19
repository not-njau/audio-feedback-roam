# 🎧 Audio Feedback — Roam Research Extension

Adds subtle, synthesized audio cues to your Roam workflow. Every block action — checking a TODO, collapsing a branch, navigating to a new page — gets a sound that confirms the action without interrupting your flow.

> **🚀 Zero Dependencies:** Built entirely on the native browser Web Audio API. No external libraries, no bloated bundles. Sounds are generated mathematically on the fly.

---

## Demo



---

## Themes

Select your theme in **Settings → Extensions → Audio Feedback → Sound Theme**.

| Theme | Character |
|---|---|
| 🌿 Zen | Soft sine tones that dissolve as quickly as they appear. Designed to confirm without interrupting. |
| 👾 Retro | Square-wave pulses with Game Boy-era frequencies. Every action lands with a satisfying, deliberate click. |
| 💫 Halo | Layered FM bells with a floating upper harmonic. |
| 🎛 Custom | Your sounds, your rules. Upload any audio file to your Roam graph and paste the Firebase URL to replace any event sound. |

---

## Events

| Event | Trigger |
|---|---|
| TODO Done | Checking a TODO checkbox → DONE |
| Collapse | Collapsing a block that has children |
| Expand | Expanding a block that has children |
| Indent | Indenting a block (Tab) |
| Outdent | Outdenting a block (Shift+Tab) |
| Navigate | Zooming into a block or changing page |
| Sidebar Open | Opening a new item in the right sidebar |

---

## Settings Reference

| Setting | Description | Default |
|---|---|---|
| Enable Audio Feedback | Master on/off switch | On |
| Master Volume | 0.0 to 1.0 | 0.4 |
| Sound Theme | Zen · Retro · Halo · Custom | Zen |
| TODO Sounds | Toggle checkbox event sounds | On |
| Navigation Sounds | Toggle zoom and sidebar sounds | On |
| Hierarchy Sounds | Toggle collapse, expand, indent, outdent sounds | On |

---

## Command Palette

Three commands are available via `Cmd+P` (or `Ctrl+P`):

- **Audio Feedback: Toggle On/Off** — master switch without opening settings
- **Audio Feedback: Volume Up** — increases master volume by 0.1
- **Audio Feedback: Volume Down** — decreases master volume by 0.1
- **Audio Feedback: Next Theme** — cycles Zen → Retro → Halo → Zen (Custom is excluded from cycling; select it manually in settings)

---

## Custom Sounds

Custom theme lets you replace any event sound with your own audio file.

### How to upload a sound to your Roam graph

1. Open any page in Roam
2. Type `/Upload file` and select your `.wav` or `.mp3`
3. Once uploaded, right-click the file attachment → **Copy link**
4. The URL will look like: `https://firebasestorage.googleapis.com/...`
5. Paste that full URL into the corresponding field in **Settings → Audio Feedback → CUSTOM SOUND URLs**
6. Switch theme to **Custom**

### Notes on Custom playback

- **First play has a brief load delay** — the extension fetches and decodes the audio file on first use. Every subsequent play of the same URL is instantaneous (the buffer is cached for the session).
- **Blank URL = silence** — if a URL field is empty, that event produces no sound. This is intentional. You can mix custom sounds for some events and silence for others.
- **URL must include the `?alt=media&token=...` suffix** — without it, Firebase returns a redirect rather than the raw audio file and playback will fail silently.
- Test URL (courtesy of Adam Krivka): `https://firebasestorage.googleapis.com/v0/b/firescript-577a2.appspot.com/o/imgs%2Fapp%2FMyelin%2F-J1q4HZsEl.wav?alt=media&token=34ab3de0-1b43-4361-8c7e-141c1925011b`

---

## Physics Bridge

All three synthesized themes (Zen, Retro, Halo) are aware of your block's position in the hierarchy. Sounds shift subtly based on:

- **Depth** — how many levels deep the block is nested. Deeper blocks produce slightly higher pitched, shorter sounds.
- **Mass** — how many children the block has. Heavier blocks produce slightly lower pitched, longer sounds.

This means the same action sounds meaningfully different depending on where it occurs in your outline — without ever being distracting.

---

## Compatibility & Requirements

- Roam Research (web and desktop)
- Requires a modern browser with Web Audio API support (all current browsers qualify)
- **Note on Audio Initialization:** On modern browsers, the audio engine requires a user gesture to "wake up". Simply clicking anywhere in Roam after loading your graph will activate the synthesizer.

---

## Feature Requests, Bugs, and Feedback

If you have an idea for a new feature or find a bug, please file it under **Issues** with a short description and a screenshot.

If you have any additional comments or suggestions, feel free to DM me:
- **Twitter / X:** [@not_njau](https://twitter.com/not_njau)
- **Roam Slack:** William Njau

---

## Support My Work

If you enjoy this extension and want to support my work, consider subscribing to my newsletter: 
[williamnjau.substack.com](https://williamnjau.substack.com/)

If this extension has made your Roam workflow a little more satisfying, consider supporting its development:
[![Ko-fi](https://img.shields.io/badge/Support%20me%20on-Ko--fi-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/williamnjau)
