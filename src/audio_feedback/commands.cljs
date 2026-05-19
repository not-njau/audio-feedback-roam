(ns audio-feedback.commands
  (:require [audio-feedback.config :refer [get-config set-config]]))

;; --- 1. Action Primitives ---

(def sub-toggle-keys ["todo-enabled" "nav-enabled" "hierarchy-enabled"])

(defn toggle-enabled! []
  (let [current (get-config "enabled" true)]
    (if current
      (do
        (doseq [k sub-toggle-keys]
          (set-config (str "saved-" k) (get-config k true)))
        (doseq [k sub-toggle-keys]
          (set-config k false))
        (set-config "enabled" false)
        (js/console.log "🔊 Audio Feedback: OFF"))
      (do
        (doseq [k sub-toggle-keys]
          (set-config k (get-config (str "saved-" k) true)))
        (set-config "enabled" true)
        (js/console.log "🔊 Audio Feedback: ON")))))

(defn volume-up! []
  (let [current (js/parseFloat (get-config "volume" "0.8"))
        next    (min 1.0 (js/parseFloat (.toFixed (+ current 0.1) 1)))]
    (set-config "volume" (str next))
    (js/console.log "🔊 Volume →" next)))

(defn volume-down! []
  (let [current (js/parseFloat (get-config "volume" "0.8"))
        next    (max 0.0 (js/parseFloat (.toFixed (- current 0.1) 1)))]
    (set-config "volume" (str next))
    (js/console.log "🔊 Volume →" next)))

(defn next-theme! []
  (let [order   ["Zen" "Retro" "Halo"]
        current (get-config "theme" "Zen")
        idx     (.indexOf (clj->js order) current)
        next    (get order (mod (inc idx) (count order)) "Zen")]
    (set-config "theme" next)
    (js/console.log "🎨 Theme →" next)))

;; --- 2. Command Palette Registration ---

(defn register-commands! [api]
  (let [palette (-> api .-ui .-commandPalette)]

    (.addCommand palette
      (clj->js {:label   "Audio Feedback: Toggle On/Off"
                :callback toggle-enabled!}))

    (.addCommand palette
      (clj->js {:label   "Audio Feedback: Volume Up"
                :callback volume-up!}))

    (.addCommand palette
      (clj->js {:label   "Audio Feedback: Volume Down"
                :callback volume-down!}))

    (.addCommand palette
      (clj->js {:label   "Audio Feedback: Next Theme"
                :callback next-theme!}))

    (js/console.log "✅ Audio Feedback: Commands registered.")))
