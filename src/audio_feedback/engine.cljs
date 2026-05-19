(ns audio-feedback.engine
  (:require [audio-feedback.config :as config]
            [audio-feedback.themes :as themes]
            [clojure.string :as str]))

;; ─── Audio Context (Singleton) ───────────────────────────

(defn get-ctx []
  (or (.-coderAudioContext js/window)
      (let [Ctk (or js/window.AudioContext js/window.webkitAudioContext)]
        (when Ctk
          (let [ctx (new Ctk)]
            (set! (.-coderAudioContext js/window) ctx)
            ctx)))))

(defn ensure-audio-active! [ctx]
  (when (and ctx (= (.-state ctx) "suspended"))
    (.resume ctx)))

;; ─── Theme Name Normalizer ───────────────────────────────

(def theme-label->key
  {"Zen"      :zen
   "Retro"    :retro
   "Halo"     :halo
   "Custom"   :custom
   "🌿 Zen"   :zen
   "👾 Retro" :retro
   "💫 Halo"  :halo
   "zen"      :zen
   "retro"    :retro
   "halo"     :halo
   "custom"   :custom})

;; ─── File Playback (Buffer-Cached) ───────────────────────

(defonce buffer-cache (atom {}))

(defn play-file-v2! [ctx {:keys [src vol] :or {vol 1.0}}]
  (when (and ctx src (not (str/blank? src)))
    (if-let [buf (get @buffer-cache src)]
      (let [source (.createBufferSource ctx)
            gain   (.createGain ctx)]
        (set! (.-buffer source) buf)
        (.connect source gain)
        (.connect gain (.-destination ctx))
        (set! (.-value (.-gain gain)) vol)
        (.start source))
      (-> (js/fetch src)
          (.then (fn [r] (.arrayBuffer r)))
          (.then (fn [ab] (.decodeAudioData ctx ab)))
          (.then (fn [buf]
                   (swap! buffer-cache assoc src buf)
                   (let [source (.createBufferSource ctx)
                         gain   (.createGain ctx)]
                     (set! (.-buffer source) buf)
                     (.connect source gain)
                     (.connect gain (.-destination ctx))
                     (set! (.-value (.-gain gain)) vol)
                     (.start source))))
          (.catch (fn [e] (js/console.warn "Audio file load failed:" e)))))))

;; ─── Synthesis Primitives ────────────────────────────────

(defn play-standard! [ctx params vol-scalar]
  (let [{:keys [freq shape duration vol]
         :or {freq 440 shape "sine" duration 0.2 vol 0.1}} params
        t    (.-currentTime ctx)
        osc  (.createOscillator ctx)
        gain (.createGain ctx)]
    (.connect osc gain)
    (.connect gain (.-destination ctx))
    (set! (.-value (.-frequency osc)) freq)
    (set! (.-type osc) shape)
    (set! (.-value (.-gain gain)) (* vol vol-scalar))
    (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t duration))
    (.start osc t)
    (.stop osc (+ t duration))))

(defn play-fm-bell! [ctx params vol-scalar]
  (let [{:keys [carrier-freq mod-freq mod-index duration vol]
         :or {carrier-freq 440 mod-freq 220 mod-index 1 duration 0.8 vol 0.1}} params
        t        (.-currentTime ctx)
        carrier  (.createOscillator ctx)
        mod      (.createOscillator ctx)
        mod-gain (.createGain ctx)
        out-gain (.createGain ctx)]
    (.connect mod mod-gain)
    (.connect mod-gain (.-frequency carrier))
    (.connect carrier out-gain)
    (.connect out-gain (.-destination ctx))
    (set! (.-value (.-frequency carrier)) carrier-freq)
    (set! (.-value (.-frequency mod)) mod-freq)
    (set! (.-value (.-gain mod-gain)) (* mod-index mod-freq))
    (set! (.-value (.-gain out-gain)) (* vol vol-scalar))
    (.exponentialRampToValueAtTime (.-gain out-gain) 0.0001 (+ t duration))
    (.start carrier t)
    (.start mod t)
    (.stop carrier (+ t duration))
    (.stop mod (+ t duration))))

(defn play-dual-fm-bell! [ctx params vol-scalar]
  (play-fm-bell! ctx params vol-scalar)
  (js/setTimeout
    #(play-fm-bell! ctx
       (assoc params :carrier-freq (* (:carrier-freq params 1000) 1.5))
       (* vol-scalar 0.7))
    80))

(defn play-freq-sweep! [ctx params vol-scalar]
  (let [{:keys [start-freq end-freq shape duration vol]
         :or {start-freq 440 end-freq 220 shape "sine" duration 0.3 vol 0.1}} params
        t    (.-currentTime ctx)
        osc  (.createOscillator ctx)
        gain (.createGain ctx)]
    (.connect osc gain)
    (.connect gain (.-destination ctx))
    (set! (.-type osc) shape)
    (.setValueAtTime (.-frequency osc) start-freq t)
    (.exponentialRampToValueAtTime (.-frequency osc) end-freq (+ t duration))
    (set! (.-value (.-gain gain)) (* vol vol-scalar))
    (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t duration))
    (.start osc t)
    (.stop osc (+ t duration))))

(defn play-bell! [ctx params vol-scalar]
  (let [{:keys [freq duration vol]
         :or {freq 440 duration 0.4 vol 0.1}} params
        t    (.-currentTime ctx)
        osc  (.createOscillator ctx)
        gain (.createGain ctx)]
    (.connect osc gain)
    (.connect gain (.-destination ctx))
    (set! (.-type osc) "sine")
    (set! (.-value (.-frequency osc)) freq)
    (set! (.-value (.-gain gain)) (* vol vol-scalar))
    (.exponentialRampToValueAtTime (.-gain gain) 0.0001 (+ t duration))
    (.start osc t)
    (.stop osc (+ t duration))))

(defn play-filtered-square! [ctx params vol-scalar]
  (let [{:keys [freq cutoff-freq duration vol]
         :or {freq 440 cutoff-freq 800 duration 0.4 vol 0.1}} params
        t      (.-currentTime ctx)
        osc    (.createOscillator ctx)
        filter (.createBiquadFilter ctx)
        gain   (.createGain ctx)]
    (.connect osc filter)
    (.connect filter gain)
    (.connect gain (.-destination ctx))
    (set! (.-type osc) "square")
    (set! (.-value (.-frequency osc)) freq)
    (set! (.-type filter) "lowpass")
    (set! (.-value (.-frequency filter)) cutoff-freq)
    (set! (.-value (.-gain gain)) (* vol vol-scalar))
    (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t duration))
    (.start osc t)
    (.stop osc (+ t duration))))

(defn play-layered! [ctx params vol-scalar]
  (let [{:keys [freq1 freq2 shape duration vol1 vol2]
         :or {freq1 440 freq2 554 shape "sine" duration 0.3 vol1 0.1 vol2 0.1}} params
        t     (.-currentTime ctx)
        osc1  (.createOscillator ctx)
        osc2  (.createOscillator ctx)
        gain1 (.createGain ctx)
        gain2 (.createGain ctx)]
    (.connect osc1 gain1) (.connect gain1 (.-destination ctx))
    (.connect osc2 gain2) (.connect gain2 (.-destination ctx))
    (set! (.-type osc1) shape) (set! (.-type osc2) shape)
    (set! (.-value (.-frequency osc1)) freq1)
    (set! (.-value (.-frequency osc2)) freq2)
    (set! (.-value (.-gain gain1)) (* vol1 vol-scalar))
    (set! (.-value (.-gain gain2)) (* vol2 vol-scalar))
    (.linearRampToValueAtTime (.-gain gain1) 0.0 (+ t duration))
    (.linearRampToValueAtTime (.-gain gain2) 0.0 (+ t duration))
    (.start osc1 t) (.start osc2 t)
    (.stop osc1 (+ t duration)) (.stop osc2 (+ t duration))))

(defn play-two-tone-seq! [ctx params vol-scalar]
  (let [{:keys [freq1 freq2 shape duration vol]
         :or {freq1 330 freq2 494 shape "square" duration 0.05 vol 0.1}} params]
    (play-standard! ctx {:freq freq1 :shape shape :duration duration :vol vol} vol-scalar)
    (js/setTimeout
      #(play-standard! ctx {:freq freq2 :shape shape :duration duration :vol vol} vol-scalar)
      (* duration 1000))))

;; ─── Physics Bridge (Technique-Aware) ───────────────────

(defn apply-depth-law [params level]
  (if (nil? level)
    params
    (case (:technique params)
      :standard        (update params :freq + (* level 40))
      :fm-bell         (update params :carrier-freq + (* level 30))
      :dual-fm-bell    (update params :carrier-freq + (* level 30))
      :freq-sweep      (-> params
                           (update :start-freq + (* level 30))
                           (update :end-freq + (* level 30)))
      :bell            (update params :freq + (* level 40))
      :filtered-square (update params :freq + (* level 30))
      :layered         (-> params
                           (update :freq1 + (* level 25))
                           (update :freq2 + (* level 25)))
      :two-tone-seq    (-> params
                           (update :freq1 + (* level 25))
                           (update :freq2 + (* level 25)))
      params)))

(defn apply-mass-law [params mass]
  (if (nil? mass)
    params
    (case (:technique params)
      :standard        (update params :duration #(min 0.6 (+ % (* mass 0.02))))
      :fm-bell         (update params :mod-index #(min 0.5 (+ % (* mass 0.05))))
      :dual-fm-bell    (update params :mod-index #(min 0.3 (+ % (* mass 0.03))))
      :freq-sweep      (update params :duration #(min 0.5 (+ % (* mass 0.02))))
      :bell            (update params :duration #(min 0.7 (+ % (* mass 0.03))))
      :filtered-square (update params :cutoff-freq #(min 2000 (+ % (* mass 60))))
      :layered         (update params :duration #(min 0.5 (+ % (* mass 0.02))))
      :two-tone-seq    (update params :duration #(min 0.15 (+ % (* mass 0.01))))
      params)))

;; ─── Technique Dispatch Router ───────────────────────────

(defn play-event! [ctx params vol-scalar]
  (try
    (case (:technique params)
      :standard        (play-standard! ctx params vol-scalar)
      :fm-bell         (play-fm-bell! ctx params vol-scalar)
      :dual-fm-bell    (play-dual-fm-bell! ctx params vol-scalar)
      :freq-sweep      (play-freq-sweep! ctx params vol-scalar)
      :bell            (play-bell! ctx params vol-scalar)
      :filtered-square (play-filtered-square! ctx params vol-scalar)
      :layered         (play-layered! ctx params vol-scalar)
      :two-tone-seq    (play-two-tone-seq! ctx params vol-scalar)
      (play-standard! ctx params vol-scalar))
    (catch :default _ nil)))

;; ─── Main Entry Point ────────────────────────────────────

(defn dispatch-event! [event]
  (let [ctx        (get-ctx)
        event-id   (:id event)
        level      (:level event)
        mass       (:mass event)
        vol-scalar (js/parseFloat (config/get-config "volume" "0.4"))
        theme-key  (get theme-label->key (config/get-config "theme" "Zen") :zen)]
    (when (and ctx (config/get-config "enabled" true))
      (ensure-audio-active! ctx)
      (if (= theme-key :custom)
        ;; Custom theme path — URL lookup only, no synth fallback
        (let [url-key (str "custom-sound-" (name event-id))
              url     (config/get-config url-key "")]
          (when-not (str/blank? url)
            (play-file-v2! ctx {:src url :vol (* 0.8 vol-scalar)})))
        ;; Synth theme path — Zen / Retro / Halo
        (let [theme-map (get themes/themes theme-key themes/zen-theme)
              base      (get theme-map event-id)]
          (when base
            (let [params (-> base
                             (apply-depth-law level)
                             (apply-mass-law mass))]
              (play-event! ctx params vol-scalar))))))))
