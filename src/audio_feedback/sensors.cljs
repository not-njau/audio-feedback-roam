(ns audio-feedback.sensors
(:require [clojure.string :as str]))

(defn fetch-current-main-uid! []
(js/Promise. (fn [resolve __]
(if-let [m (some-> js/window .-roamAlphaAPI .-ui .-mainWindow)]
(-> (js/Promise.resolve (.getOpenPageOrBlockUid m)) (.then (fn [uid] (resolve (or uid (.dateToPageUid (.-util (.-roamAlphaAPI js/window)) (js/Date.)))))) (.catch #(resolve (.dateToPageUid (.-util (.-roamAlphaAPI js/window)) (js/Date.)))))
(resolve nil)))))

(defn get-sidebar-uids []
(try
(if-let [api (some-> js/window .-roamAlphaAPI .-ui .-rightSidebar)]
(vec (keep (fn [w] (or (aget w "block-uid") (aget w "page-uid") (aget w "mentions-uid")))
(or (.getWindows api) [])))
[])
(catch :default __ [])))

(defn pull-block-tree! [uid]
(try
(let [api (.-roamAlphaAPI js/window)
eid (.q api "[:find ?e . :in $ ?uid :where [?e :block/uid ?uid]]" uid)]
(when eid
(js->clj (.pull (.-data api)
      "[:block/uid :block/open {:block/refs [:node/title]} {:block/children ...}]"
      eid)
:keywordize-keys false)))
(catch :default __ nil)))

(defn flatten-tree
([node] (flatten-tree node 0 {}))
([node depth acc]
(if-not node acc
(let [uid    (get node ":block/uid")
titles (into #{} (keep #(get % ":node/title") (get node ":block/refs" [])))
st     (cond (contains? titles "DONE") :done
      (contains? titles "TODO") :todo
      :else nil)
acc__  (if (not-empty uid)
  (assoc acc uid {:status st
                  :open   (get node ":block/open" true)
                  :kids   (count (get node ":block/children" []))
                  :depth  depth})
  acc)]
(reduce (fn [m c] (flatten-tree c (inc depth) m))
acc__ (get node ":block/children" []))))))
