(ns audio-feedback.loop
  (:require [audio-feedback.sensors :as s]
            [audio-feedback.engine :as e]
            [audio-feedback.config :as config]
            [clojure.set :as cset]))

(defonce active? (atom false))
(defonce sys-state (atom {:main nil :sidebar #{} :tree {}}))
(def event-rank {:todo-done 0 :navigate 2 :sidebar-open 3
                 :collapse 4 :expand 5 :indent 6 :outdent 7})

(defn process-tick! [prev curr-main]
  (let [evs      (atom [])
        c-side   (set (s/get-sidebar-uids))
        all-roots (distinct (if curr-main (cons curr-main c-side) c-side))
        _ (when (and (config/get-config "nav-enabled" true)
                     (:main prev) curr-main (not= (:main prev) curr-main))
            (swap! evs conj {:id :navigate}))
        _ (when (and (config/get-config "nav-enabled" true)
                     (not-empty (:sidebar prev))
                     (not-empty (cset/difference c-side (:sidebar prev))))
            (swap! evs conj {:id :sidebar-open}))
        c-tree   (reduce (fn [acc uid]
                           (if-let [rt (s/pull-block-tree! uid)]
                             (merge acc (s/flatten-tree rt)) acc))
                         {} all-roots)
        o-tree   (:tree prev)]
    (when (and (not-empty o-tree) (not-empty c-tree) (< (count c-tree) (* 3 (count o-tree))))
      (doseq [[uid nb] c-tree]
        (when-let [ob (get o-tree uid)]
          (when (and (config/get-config "todo-enabled" true)
                     (not= (:status ob) :done)
                     (= (:status nb) :done))
            (js/console.log "✅ TODO checked:" uid "| Lvl:" (:depth nb))
            (swap! evs conj {:id :todo-done :level (:depth nb)}))
          (when (config/get-config "hierarchy-enabled" true)
            (cond
              (and (:open ob) (not (:open nb))
                   (or (pos? (:kids ob)) (pos? (:kids nb))))
              (do (js/console.log "🔽 Coll:" uid "| Mass:" (:kids ob))
                  (swap! evs conj {:id :collapse :mass (:kids ob)}))
              (and (not (:open ob)) (:open nb)
                   (or (pos? (:kids ob)) (pos? (:kids nb))))
              (do (js/console.log "🔼 Exp:" uid "| Mass:" (:kids nb))
                  (swap! evs conj {:id :expand :mass (:kids nb)}))
              (> (:depth nb) (:depth ob))
              (swap! evs conj {:id :indent :mass (:kids nb)})
              (< (:depth nb) (:depth ob))
              (swap! evs conj {:id :outdent :mass (:kids nb)}))))))
    (doseq [[idx ev] (map-indexed vector
                                  (->> @evs distinct
                                       (sort-by #(get event-rank (:id %) 99))))]
      (js/setTimeout #(e/dispatch-event! ev) (* idx 80)))
    {:main curr-main :sidebar c-side :tree c-tree}))

(defn master-loop []
  (when @active?
    (-> (s/fetch-current-main-uid!)
        (.then (fn [cm]
                 (when @active?
                   (reset! sys-state (process-tick! @sys-state cm))
                   (js/setTimeout master-loop 300))))
        (.catch #(js/setTimeout master-loop 1000)))))

(defn start-master-loop! []
  (when-not @active?
    (reset! active? true)
    (reset! sys-state {:main nil :sidebar #{} :tree {}})
    (master-loop)
    (js/console.log "🟢 Audio Feedback: Step 1 Baseline Active.")))

(defn stop-master-loop! []
  (reset! active? false)
  (js/console.log "🛑 Audio Feedback: Watcher Stopped."))
